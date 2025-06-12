package net.chamman.moonnight.infra.naver.sms;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SMS_SEND_FAIL;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.infra.SmsSendException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;
import net.chamman.moonnight.infra.naver.NaverSignatureGenerator;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("classpath:application.properties")
public class NaverSmsClient {
	
	private final NaverSignatureGenerator naverSignatureGenerator;
	
	@Value("${naver-api.accessKey}")
	private String accessKey;
	
	@Value("${naver-sms.serviceId}")
	private String serviceId;
	
	private final String smsApiUrl = "https://sens.apigw.ntruss.com";
	private final String smsEndpoint = "/sms/v2/services/";
	private final String sendSmsUri = "/messages";
	
	/** 네이버 API 문자 발송 요청
	 * @param naverSmsPayload
	 * @throws SmsSendException {@link #sendVerificationCode}
	 * @return 문자 발송 요청 Response Status Code
	 */
	public int sendSms(NaverSmsPayload naverSmsPayload) {
		String recipientPhone = naverSmsPayload.getMessages().isEmpty() ? "N/A" : naverSmsPayload.getMessages().get(0).getTo();
		log.debug("*Naver SMS 발송 요청. Recipient: [{}], Content: [{}]",
				LogMaskingUtil.maskPhone(recipientPhone, MaskLevel.MEDIUM),
				naverSmsPayload.getContent());
		
		try {
			String time = Long.toString(System.currentTimeMillis());
			
			// 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-ncp-apigw-timestamp", time);
			headers.set("x-ncp-iam-access-key", accessKey);
			headers.set("x-ncp-apigw-signature-v2", naverSignatureGenerator.getNaverSignature("POST", smsEndpoint + serviceId + sendSmsUri, time));
			
			// HTTP 요청
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<NaverSmsPayload> entity = new HttpEntity<>(naverSmsPayload, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(new URI(smsApiUrl + smsEndpoint + serviceId + sendSmsUri),
					entity, String.class);
			
			return response.getStatusCode().value();
		} catch (Exception e) {
			throw new SmsSendException(SMS_SEND_FAIL, e);
		}
	}
	
}
