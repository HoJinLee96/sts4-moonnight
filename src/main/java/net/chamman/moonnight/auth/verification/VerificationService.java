package net.chamman.moonnight.auth.verification;

import static net.chamman.moonnight.global.exception.HttpStatusCode.MISMATCH_RECIPIENT;
import static net.chamman.moonnight.global.exception.HttpStatusCode.MISMATCH_VERIFICATION_CODE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.NOT_VERIFY;
import static net.chamman.moonnight.global.exception.HttpStatusCode.SMS_SEND_FAIL;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERIFICATION_EXPIRED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.VERIFICATION_NOT_FOUND;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.crypto.dto.VerificationEmailTokenDto;
import net.chamman.moonnight.auth.crypto.dto.VerificationPhoneTokenDto;
import net.chamman.moonnight.auth.verification.Verification.VerificationBuilder;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.crypto.EncryptException;
import net.chamman.moonnight.global.exception.infra.SmsSendException;
import net.chamman.moonnight.global.exception.redis.RedisSetException;
import net.chamman.moonnight.global.exception.verification.IllegalVerificationException;
import net.chamman.moonnight.global.exception.verification.MismatchCodeException;
import net.chamman.moonnight.global.exception.verification.NotVerifyException;
import net.chamman.moonnight.global.exception.verification.VerificationExpiredException;
import net.chamman.moonnight.infra.naver.mail.MailRecipientPayload;
import net.chamman.moonnight.infra.naver.mail.NaverMailClient;
import net.chamman.moonnight.infra.naver.mail.NaverMailPayload;
import net.chamman.moonnight.infra.naver.sms.NaverSmsClient;
import net.chamman.moonnight.infra.naver.sms.NaverSmsPayload;
import net.chamman.moonnight.infra.naver.sms.SmsRecipientPayload;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableAsync
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
	@Async
	public int sendSmsVerificationCode(String recipientPhone, String requestIp) {
		
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
			int sendStatus = naverSmsClient.sendVerificationCode(naverSmsPayload);
			verificationBuilder.sendStatus(sendStatus);
			if((sendStatus/100)!=2) {
				log.error("인증번호 발송 실패: sendStatus: {}, phone: {} , ip: {}", sendStatus, recipientPhone, requestIp);
				verificationRepository.save(verificationBuilder.build());
				throw new SmsSendException(SMS_SEND_FAIL, "인증번호 발송 실패: sendStatus: "+sendStatus);
			}
		} catch (Exception e) {
			log.error("인증번호 발송 실패: phone: {} , ip: {}", recipientPhone, requestIp);
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
	@Async
	public int sendEmailVerificationCode(String recipientEmail, String requestIp) {
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
			int sendStatus = naverMailClient.sendVerificationCode(naverMailPayload);
			verificationBuilder.sendStatus(sendStatus);
			if((sendStatus/100)!=2) {
				System.out.println("인증번호 발송 실패 (sendStatus/100)!=2: "+(sendStatus/100));
				log.info("인증번호 발송 실패: sendStatus: {}, phone: {} , ip: {}", sendStatus, recipientEmail, requestIp);
				Verification verification = verificationBuilder.build();
				verificationRepository.save(verification);
			}
		} catch (Exception e) {
			verificationBuilder.sendStatus(500);
			log.error("인증번호 발송 실패: phone: {} , ip: {}", recipientEmail, requestIp);
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
	 * @throws EncryptException {@link TokenProvider#createVerificationPhoneToken} 암호화 실패
	 * @throws RedisSetException {@link TokenProvider#createVerificationPhoneToken} Redis 저장 실패
	 * 
	 * @return 휴대폰 인증 완료 토큰
	 */
	public String compareSms(String encodedVerificationId, String phone, String reqCode, String requestIp) {
		
		int verificationId = obfuscator.decode(Integer.parseInt(encodedVerificationId));
		Verification ver = getVerificationAndValidate(verificationId);
		
		compareCode(ver, phone, reqCode, requestIp);
		
		return tokenProvider.createToken(new VerificationPhoneTokenDto(ver.getVerificationId()+"", phone), VerificationPhoneTokenDto.TOKENTYPE);
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
	 * @throws EncryptException {@link TokenProvider#createVerificationEmailToken} 암호화 실패
	 * @throws RedisSetException {@link TokenProvider#createVerificationEmailToken} Redis 저장 실패
	 * 
	 * @return 이메일 인증 완료 토큰
	 */
	public String compareEmail(String encodedVerificationId, String email, String reqCode, String requestIp) {
		
		int verificationId = obfuscator.decode(Integer.parseInt(encodedVerificationId));
		Verification ver = getVerificationAndValidate(verificationId);
		
		compareCode(ver, email, reqCode, requestIp);
		
		return tokenProvider.createToken(new VerificationEmailTokenDto(ver.getVerificationId()+"", email), VerificationEmailTokenDto.TOKENTYPE);
	}
	
	//  (to 기준, 10분 이내 요청한, 제일 최근에 요청한, 인증 여부)
	//  인증 요청이후 5분이내에 성공했지만, 인증 성공 uuid의 유효시간이 5분이기에 최대 10분으로 조회
	/**
	 * @param to
	 * @return
	 */
	public boolean validateVerify(String to) {
		return verificationRepository.findRecentVerificationWithin10Min(to)
				.map(verification -> Boolean.TRUE.equals(verification.isVerify()))
				.orElseThrow(()->new BadCredentialsException("인증 되지 않았습니다."));
	}
	
	/** 인증후, 인증 요청 찾기 및 검증 
	 * @param verificationId
	 * @return
	 */
	public Verification isVerify(int verificationId) {
		
		Object[] result = verificationRepository.findVerificationWithValidity(verificationId)
				.orElseThrow(() -> new NoSuchDataException(VERIFICATION_NOT_FOUND, "인증 요청 없음"));
		
		Verification verification = convertToVerification(result); 
		boolean isValid = ((Number) result[result.length - 1]).intValue() == 1;
		
		if(!verification.isVerify()) {
			if (!isValid) {
				verification.setVerify(false);
				verificationRepository.save(verification);
				throw new VerificationExpiredException(VERIFICATION_EXPIRED, "시간 초과된 인증 요청");
			}
			throw new NotVerifyException(NOT_VERIFY,"미인증");
		}
		
		return verification;
	}
	
	/** 인증전, 인증 요청 찾기 및 시간초과 검증
	 * @param recipient
	 * @param requestIp
	 * @throws NoSuchDataException {@link #findVerification} 최근 인증 요청 없음
	 * @throws VerificationExpiredException {@link #findVerification} 인증 시간 초과
	 * @return Verification
	 */
	private Verification getVerificationAndValidate(int verificationId) {
		
		Object[] result = verificationRepository.findVerificationWithValidity(verificationId)
				.orElseThrow(() -> new NoSuchDataException(VERIFICATION_NOT_FOUND, "인증 요청 없음"));
		
		Verification verification = convertToVerification(result); 
		boolean isValid = ((Number) result[result.length - 1]).intValue() == 1;
		
		if (!isValid) {
			verification.setVerify(false);
			verificationRepository.save(verification);
			throw new VerificationExpiredException(VERIFICATION_EXPIRED, "시간 초과된 인증 요청");
		}
		
		return verification;
	}
	
	/** 인증번호 검증
	 * @param verification
	 * @param reqCode
	 * @param requestIp
	 * @throws IllegalVerificationException {@link #compareCode} 수신자 불일치
	 * @throws MismatchCodeException {@link #compareCode} 인증 번호 불일치
	 */
	private void compareCode(Verification verification, String recipient, String reqCode, String requestIp) {
		
//		 수신자 검사
		if(!verification.getRecipient().equals(recipient)) {
			throw new IllegalVerificationException(MISMATCH_RECIPIENT,"수신자 불일치. reqRecipient: "+recipient);
		}
		
//		 인증번호 일치 결과
		if(!verification.getVerificationCode().equals(reqCode)) {
			throw new MismatchCodeException(MISMATCH_VERIFICATION_CODE,"인증 번호 불일치. reqCode: "+reqCode);
		}
		
//		 DB `Verifivation` 인증 성공 기록
		verificationRepository.markAsVerified(verification.getVerificationId());
		verificationRepository.save(verification);
	}
	
	/** 랜덤 인증번호 생성
	 * @return 인증번호
	 */
	private String generateVerificationCode() {
		return String.format("%06d", new Random().nextInt(1000000)); // 6자리 인증번호 생성
	}
	
	/** Object 배열 -> Verification 매핑
	 * @param result
	 * @return 인증 요청 객체
	 */
	private Verification convertToVerification(Object[] result) {
		Verification v = new Verification();
		v.setVerificationId(((Number) result[0]).intValue());
		v.setRequestIp((String) result[1]);
		v.setRecipient((String) result[2]);
		v.setVerificationCode((String) result[3]);
		v.setSendStatus(((Number) result[4]).intValue());
		v.setCreatedAt(((Timestamp) result[5]).toLocalDateTime());
		v.setVerify(result[6] != null ? ((Boolean) result[6]) : null);
		v.setVerifyAt(result[7] != null ? ((Timestamp) result[7]).toLocalDateTime() : null);
		return v;
	}
	
	
}
