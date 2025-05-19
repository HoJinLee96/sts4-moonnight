package net.chamman.moonnight.auth.verification;

import static net.chamman.moonnight.global.exception.HttpStatusCode.VERIFY_EXPIRED;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.TokenProvider;
import net.chamman.moonnight.auth.verification.Verification.VerificationBuilder;
import net.chamman.moonnight.global.exception.ExpiredException;
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
  private final TokenProvider tokenStore;
  
  @Value("${naver-sms.senderPhone}")
  private String senderPhone;
  
  @Value("${naver-email.senderEmail}")
  private String senderEmail;
  
  @Transactional
  public void sendSmsVerificationCode(String recipientPhone, String requestIp) {
   
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
        log.info("인증번호 발송 실패: sendStatus: {}, phone: {} , ip: {}", sendStatus, recipientPhone, requestIp);
      }
    } catch (Exception e) {
      verificationBuilder.sendStatus(500);
      e.printStackTrace();
      log.error("인증번호 발송 실패: phone: {} , ip: {}", recipientPhone, requestIp);
      throw new IllegalStateException("인증번호 발송 실패. 나중에 다시 시도해주세요.");
    }finally {
      verificationRepository.save(verificationBuilder.build());
    }
  }
  
  @Transactional
  public void sendEmailVerificationCode(String recipientEmail, String requestIp) {
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
      }
    } catch (Exception e) {
      verificationBuilder.sendStatus(500);
      e.printStackTrace();
      log.error("인증번호 발송 실패: phone: {} , ip: {}", recipientEmail, requestIp);
      throw new IllegalStateException("인증번호 발송 실패. 나중에 다시 시도해주세요.");
    }finally {
      verificationRepository.save(verificationBuilder.build());
    }
  }
  
  @Transactional
  public String compareSms(String phone, String reqCode, String requestIp) {
    Verification ver = findVerification(phone, requestIp);
    compareCode(ver, reqCode, requestIp);
    return tokenStore.createVerificationPhoneToken(phone);
  }
  
  @Transactional
  public String compareEmail(String email, String reqCode, String requestIp) {
    Verification ver = findVerification(email, requestIp);
    compareCode(ver, reqCode, requestIp);
    return tokenStore.createVerificationEmailToken(email);
  }
  
  //  (to 기준, 10분 이내 요청한, 제일 최근에 요청한, 인증 여부)
  //  인증 요청이후 5분이내에 성공했지만, 인증 성공 uuid의 유효시간이 5분이기에 최대 10분으로 조회
  public boolean validateVerify(String to) {
    return verificationRepository.findRecentVerificationWithin10Min(to)
        .map(verification -> Boolean.TRUE.equals(verification.getVerify()))
        .orElseThrow(()->new BadCredentialsException("인증 되지 않았습니다."));
  }
  
  public Verification findVerification(String recipient, String requestIp) {
    // ======= 수신자에 일치하는 DB 찾기 =======
    Verification verification = verificationRepository.findTopByRecipientOrderByCreatedAtDesc(recipient)
        .orElseThrow(() -> {
          log.info("인증 비교 실패 : 존재하지 않는 인증을 인증 요청함. recipient: {}, requestIp: {}",recipient, requestIp);
          return new NoSuchElementException("존재하지 않는 인증을 인증 요청함");
          });
    
    // ======= 해당 데이터가 3분 이내인지 여부 확인 =======
    boolean withinTimeResult = verificationRepository.isWithinVerificationTime(verification.getVerificationId()) == 1L;
    System.out.println("인증시퀀스키값: "+ verification.getVerificationId()+", 3분이내 시퀀스인지 조회 결과: "+withinTimeResult);
    if (!withinTimeResult) {
      verification.setVerify(false);
      verificationRepository.flush();
      throw new ExpiredException(VERIFY_EXPIRED,"인증 시간이 초과 되었습니다.");
    }
    
    return verification;
  }
  
  private void compareCode(Verification verification, String reqCode, String requestIp) {
    // ======= 인증번호 일치 결과 =======
    if(!verification.getVerificationCode().equals(reqCode)) {
      log.info("인증 번호 불일치 : recipient: {}, reqCode: {}, requestIp: {}",verification.getRecipient(), reqCode, requestIp);
      throw new BadCredentialsException("인증 비교 실패 : 인증 번호 불일치");
    }
    
    // ======= DB `Verifivation` 인증 완료 기록 =======
    verificationRepository.markAsVerified(verification.getVerificationId());
    verificationRepository.flush();
  }
  
  private String generateVerificationCode() {
    return String.format("%06d", new Random().nextInt(1000000)); // 6자리 인증번호 생성
  }
    
}
