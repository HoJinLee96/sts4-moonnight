package net.chamman.moonnight.infra.naver.sms;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:application.properties")
public class GuidanceService {
  
  private final NaverSmsClient naverSmsClient;
  
  @Value("${naver-sms.senderPhone}")
  private String senderPhone;
  @Value("${naver-sms.adminPhone}")
  private String adminPhone;
  
  public void sendEstimateInfoSms(String recipientPhone, String estimateId) throws InvalidKeyException, JsonProcessingException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException {
    String body = "[ 견적 번호 : " + estimateId + " ]\n"+
        "달밤청소 문의 주셔서 감사합니다.\n"+
        "빠른 시일 내에 연락 드리겠습니다.";
    
    // 수신자 설정
    SmsRecipientPayload smsRecipientPayload = new SmsRecipientPayload(recipientPhone.replaceAll("[^0-9]", ""),body);
    List<SmsRecipientPayload> messages = List.of(smsRecipientPayload);

    // 요청 데이터 생성
    NaverSmsPayload naverSmsPayload = NaverSmsPayload.builder()
            .type("SMS")
            .contentType("COMM")
            .countryCode("82")
            .from(senderPhone)
            .content("달밤청소 견적서")
//            .content("[ 달밤청소 가입 인증번호 ]\n[" + verificationCode + "]를 입력해주세요")
            .messages(messages)
            .build();
    
    naverSmsClient.sendVerificationCode(naverSmsPayload);
  }
  
  public void sendSecurityAlert(String message) throws InvalidKeyException, JsonProcessingException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException {
    
    // 수신자 설정
    SmsRecipientPayload smsRecipientPayload = new SmsRecipientPayload(adminPhone.replaceAll("[^0-9]", ""),message);
    List<SmsRecipientPayload> messages = List.of(smsRecipientPayload);

    // 요청 데이터 생성
    NaverSmsPayload naverSmsPayload = NaverSmsPayload.builder()
            .type("SMS")
            .contentType("COMM")
            .countryCode("82")
            .from(senderPhone)
            .content("달밤청소 견적서")
//            .content("[ 달밤청소 가입 인증번호 ]\n[" + verificationCode + "]를 입력해주세요")
            .messages(messages)
            .build();
    
    naverSmsClient.sendVerificationCode(naverSmsPayload);
  }
  
}
