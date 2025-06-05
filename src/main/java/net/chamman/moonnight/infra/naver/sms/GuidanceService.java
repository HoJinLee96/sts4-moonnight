package net.chamman.moonnight.infra.naver.sms;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@PropertySource("classpath:application.properties")
@Slf4j
public class GuidanceService {
	
	private final NaverSmsClient naverSmsClient;
	
	@Value("${naver-sms.senderPhone}")
	private String senderPhone;
	@Value("${naver-sms.adminPhone}")
	private String adminPhone;
	
	/** 견적 신청 확인 문자 안내
	 * @param recipientPhone
	 * @param estimateId
	 */
	public void sendEstimateInfoSms(String recipientPhone, String estimateId) {
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
				.messages(messages)
				.build();
		try {
			int statusCode = naverSmsClient.sendSms(naverSmsPayload);
			if((statusCode/100)!=2) {
				log.error("안내 문자 발송 실패. statusCode: {}, phone: {}", statusCode, recipientPhone);
			}
		} catch (Exception e) {
			log.error("안내 문자 발송 실패. phone: {}, e: {}", recipientPhone, e);
		}
	}
	
	/** 서버 문제 발생 관리자 문자 알람
	 * @param message
	 */
	public void sendAdminAlert(String message) {
		
		// 수신자 설정
		SmsRecipientPayload smsRecipientPayload = new SmsRecipientPayload(adminPhone.replaceAll("[^0-9]", ""),message);
		List<SmsRecipientPayload> messages = List.of(smsRecipientPayload);
		
		// 요청 데이터 생성
		NaverSmsPayload naverSmsPayload = NaverSmsPayload.builder()
				.type("SMS")
				.contentType("COMM")
				.countryCode("82")
				.from(senderPhone)
				.content("서버 문제 발생.")
				.messages(messages)
				.build();
		
		try {
			int statusCode = naverSmsClient.sendSms(naverSmsPayload);
			if((statusCode/100)!=2) {
				log.error("서버 문제 발생. 관리자 문자 알람 발송 실패.");
			}
		} catch (Exception e) {
			log.error("서버 문제 발생. 관리자 문자 알람 발송 실패. e: {}", e);
		}
	}
	
}
