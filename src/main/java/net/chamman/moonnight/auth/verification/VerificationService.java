package net.chamman.moonnight.auth.verification;

import static net.chamman.moonnight.global.exception.HttpStatusCode.MISMATCH_RECIPIENT;
import static net.chamman.moonnight.global.exception.HttpStatusCode.MISMATCH_VERIFICATION_CODE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.NOT_VERIFY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SMS_SEND_FAIL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERIFICATION_EXPIRED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERIFICATION_NOT_FOUND;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.crypto.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.Verification.VerificationBuilder;
import net.chamman.moonnight.auth.verification.VerificationRepository.VerificationProjection;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.infra.MailSendException;
import net.chamman.moonnight.global.exception.infra.SmsSendException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.verification.IllegalVerificationException;
import net.chamman.moonnight.global.exception.verification.MismatchCodeException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.naver.mail.MailRecipientPayload;
import net.chamman.moonnight.infra.naver.mail.NaverMailClient;
import net.chamman.moonnight.infra.naver.mail.NaverMailPayload;
import net.chamman.moonnight.infra.naver.sms.NaverSmsClient;
import net.chamman.moonnight.infra.naver.sms.NaverSmsPayload;
import net.chamman.moonnight.infra.naver.sms.SmsRecipientPayload;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationService {
	
	private final VerificationRepository verificationRepository;
	private final NaverMailClient naverMailClient;
	private final NaverSmsClient naverSmsClient;
	private final TokenProvider tokenProvider;
	private final Obfuscator obfuscator;
	
	@Value("${naver-sms.senderPhone}")
	private String senderPhone;
	
	@Value("${naver-email.senderEmail}")
	private String senderEmail;
	
	/** 문자 인증번호 발송
	 * @param recipientPhone
	 * @param requestIp
	 * 
	 * @throws SmsSendException {@link NaverSmsClient#sendVerificationCode}
	 */
	public int sendSmsVerificationCode(String recipientPhone, String requestIp) {
		log.debug("인증번호 문자 발송. RecipientPhone: [{}], RequestIp: [{}]",
				LogMaskingUtil.maskPhone(recipientPhone, MaskLevel.MEDIUM),
				requestIp
				);
		
		String verificationCode = generateVerificationCode();
		String body = "인증번호 [" + verificationCode + "]를 입력해주세요.";
		
		// 수신자 설정
		SmsRecipientPayload smsRecipientPayload = new SmsRecipientPayload(recipientPhone.replaceAll("[^0-9]", ""),body);
		List<SmsRecipientPayload> messages = List.of(smsRecipientPayload);
		
		// 요청 데이터 생성
		NaverSmsPayload naverSmsPayload = NaverSmsPayload.builder()
				.type("SMS")
				.contentType("COMM")
				.countryCode("82")
				.from(senderPhone)
				.content("달밤청소 인증번호")
//            .content("[ 달밤청소 가입 인증번호 ]\n[" + verificationCode + "]를 입력해주세요")
				.messages(messages)
				.build();
		
		VerificationBuilder verificationBuilder = Verification.builder()
				.requestIp(requestIp)
				.recipient(recipientPhone)
				.verificationCode(verificationCode);
		
		try {
			int sendStatus = naverSmsClient.sendSms(naverSmsPayload);
			verificationBuilder.sendStatus(sendStatus);
			if((sendStatus/100)!=2) {
				log.error("인증번호 발송 요청 응답코드 실패: SendStatus: [{}], RecipientPhone: [{}] , RequestIp: [{}]", sendStatus, recipientPhone, requestIp);
				verificationRepository.save(verificationBuilder.build());
				throw new SmsSendException(SMS_SEND_FAIL, "인증번호 발송 실패: sendStatus: "+sendStatus);
			}
		} catch (Exception e) {
			log.error("인증번호 발송 요청 실패: RecipientPhone: [{}] , RequestIp: [{}]", recipientPhone, requestIp, e);
			verificationBuilder.sendStatus(500);
			verificationRepository.save(verificationBuilder.build());
			throw e;
		}
		
		Verification verification = verificationBuilder.build();
		verificationRepository.save(verification);
		
		return obfuscator.encode(verification.getVerificationId());
	}
	
	/** 이메일 인증번호 발송
	 * @param recipientEmail
	 * @param requestIp
	 * @throws MailSendException {@link NaverMailClient#sendMail}
	 * @return
	 */
	public int sendEmailVerificationCode(String recipientEmail, String requestIp) {
		log.debug("인증번호 이메일 발송. RecipientEmail: [{}], RequestIp: [{}]",
				LogMaskingUtil.maskEmail(recipientEmail, MaskLevel.MEDIUM),
				requestIp
				);
		
		String verificationCode = generateVerificationCode();
		String body = "인증번호 [" + verificationCode + "]를 입력해주세요.";
		
		// 수신자 설정
		MailRecipientPayload mailRecipientPayload = new MailRecipientPayload(recipientEmail,recipientEmail,"R");
		List<MailRecipientPayload> mails = List.of(mailRecipientPayload);
		
		// 요청 데이터 생성
		NaverMailPayload naverMailPayload = NaverMailPayload.builder()
				.senderAddress(senderEmail)
				.title("달밤청소 인증번호")
				.body(body)
				.recipients(mails)
				.individual(true)
				.advertising(false)
				.build();
		
		VerificationBuilder verificationBuilder = Verification.builder()
				.requestIp(requestIp)
				.recipient(recipientEmail)
				.verificationCode(verificationCode);
		
		try {
			int sendStatus = naverMailClient.sendMail(naverMailPayload);
			verificationBuilder.sendStatus(sendStatus);
			if((sendStatus/100)!=2) {
				log.error("인증번호 발송 요청 응답코드 실패: SendStatus: [{}], RecipientEmail: [{}] , RequestIp: [{}]", sendStatus, recipientEmail, requestIp);
				Verification verification = verificationBuilder.build();
				verificationRepository.save(verification);
			}
		} catch (Exception e) {
			log.error("인증번호 발송 요청 실패: RecipientEmail: [{}] , RequestIp: [{}]", recipientEmail, requestIp, e);
			verificationBuilder.sendStatus(500);
			Verification verification = verificationBuilder.build();
			verificationRepository.save(verification);
			throw e;
		}
		
		Verification verification = verificationBuilder.build();
		verificationRepository.save(verification);
		
		return obfuscator.encode(verification.getVerificationId());
	}
	
	/** 휴대폰 인증번호 검증
	 * @param encodedVerificationId
	 * @param phone
	 * @param reqCode
	 * @param requestIp
	 * 
	 * @throws NoSuchDataException {@link #getVerificationAndValidate} 최근 인증 요청 없음
	 * @throws VerificationExpiredException {@link #getVerificationAndValidate} 인증 시간 초과
	 * 
	 * @throws IllegalVerificationException {@link #compareCode} 수신자 불일치
	 * @throws MismatchCodeException {@link #compareCode} 인증 번호 불일치
	 * 
     * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
	 * 
	 * @return 휴대폰 인증 완료 토큰
	 */
	@Transactional
	public String compareSms(String encodedVerificationId, String phone, String reqCode, String requestIp) {
		
		int verificationId = obfuscator.decode(Integer.parseInt(encodedVerificationId));
		
		log.debug("문자 인증번호 검증. VerificationId: [{}], RequestIp: [{}]",
				LogMaskingUtil.maskId(verificationId, MaskLevel.MEDIUM),
				requestIp
				);
		
		compareCode(verificationId, phone, reqCode, requestIp);
		
		return tokenProvider.createToken(new VerificationPhoneTokenDto(verificationId+"", phone), VerificationPhoneTokenDto.TOKENTYPE);
	}
	
	/** 이메일 인증번호 검증
	 * @param encodedVerificationId
	 * @param email
	 * @param reqCode
	 * @param requestIp
	 * 
	 * @throws NoSuchDataException {@link #getVerificationAndValidate} 최근 인증 요청 없음
	 * @throws VerificationExpiredException {@link #getVerificationAndValidate} 인증 시간 초과
	 * 
	 * @throws IllegalVerificationException {@link #compareCode} 수신자 불일치
	 * @throws MismatchCodeException {@link #compareCode} 인증 번호 불일치
	 * 
     * @throws EncryptException {@link TokenProvider#createToken} 암호화 실패
     * @throws RedisSetException {@link TokenProvider#createToken} Redis 저장 실패
	 * 
	 * @return 이메일 인증 완료 토큰
	 */
	@Transactional
	public String compareEmail(String encodedVerificationId, String email, String reqCode, String requestIp) {
		
		int verificationId = obfuscator.decode(Integer.parseInt(encodedVerificationId));
		
		log.debug("이메일 인증번호 검증. VerificationId: [{}], RequestIp: [{}]",
				LogMaskingUtil.maskId(verificationId, MaskLevel.MEDIUM),
				requestIp
				);
		
		compareCode(verificationId, email, reqCode, requestIp);
		
		return tokenProvider.createToken(new VerificationEmailTokenDto(verificationId+"", email), VerificationEmailTokenDto.TOKENTYPE);
	}
	
	/** 인증후, 인증 요청 찾기 및 검증 
	 * @param verificationId
	 * @throws NoSuchDataException {@link #isVerify} DB verificationId 일치하는 인증 요청 없음.
	 * @throws VerificationExpiredException {@link #isVerify} DB 미인증된 인증 요청(시관 초과된 인증).
	 * @throws NotVerifyException {@link #isVerify} DB 미인증된 인증 요청.
	 * @return
	 */
	public void isVerify(int verificationId) {
		
		VerificationProjection verificationProjection = verificationRepository.findVerificationWithValidity(verificationId)
				.orElseThrow(() -> new NoSuchDataException(VERIFICATION_NOT_FOUND, "인증 요청 없음"));
		
		if(!verificationProjection.getVerify()) {
			if (!verificationProjection.isValid()) {
				throw new VerificationExpiredException(VERIFICATION_EXPIRED, "미인증된 인증 요청(시관 초과된 인증)");
			}
			throw new NotVerifyException(NOT_VERIFY,"미인증된 인증 요청");
		}
		
	}
	
	/** 인증번호 검증
	 * @param verification
	 * @param reqCode
	 * @param requestIp
	 * @throws IllegalVerificationException {@link #compareCode} 수신자 불일치
	 * @throws MismatchCodeException {@link #compareCode} 인증 번호 불일치
	 */
	private void compareCode(int verificationId, String recipient, String reqCode, String requestIp) {
		
		VerificationProjection verificationProjection = verificationRepository.findVerificationWithValidity(verificationId)
				.orElseThrow(() -> new NoSuchDataException(VERIFICATION_NOT_FOUND, "인증 요청 없음"));
		
		if (!verificationProjection.isValid()) {
			throw new VerificationExpiredException(VERIFICATION_EXPIRED, "시간 초과된 인증 요청");
		}
		
//		 수신자 검사
		if(!verificationProjection.getRecipient().equals(recipient)) {
			throw new IllegalVerificationException(MISMATCH_RECIPIENT,"수신자 불일치. reqRecipient: "+recipient);
		}
		
//		 인증번호 일치 결과
		if(!verificationProjection.getVerificationCode().equals(reqCode)) {
			throw new MismatchCodeException(MISMATCH_VERIFICATION_CODE,"인증 번호 불일치. reqCode: "+reqCode);
		}
		
//		 DB `Verifivation` 인증 성공 기록
		verificationRepository.markAsVerified(verificationId);
	}
	
	/** 랜덤 인증번호 생성
	 * @return 인증번호
	 */
	private String generateVerificationCode() {
		return String.format("%06d", new Random().nextInt(1000000)); // 6자리 인증번호 생성
	}
	
	
}
