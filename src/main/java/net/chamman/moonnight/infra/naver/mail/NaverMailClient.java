package net.chamman.moonnight.infra.naver.mail;

import static net.chamman.moonnight.global.exception.HttpStatusCode.MAIL_SEND_FAIL;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.exception.infra.MailSendException;
import net.chamman.moonnight.infra.naver.NaverSignatureGenerator;

@Component
@PropertySource("classpath:application.properties")
@RequiredArgsConstructor
public class NaverMailClient {
	
	private final NaverSignatureGenerator naverSignatureGenerator;
	
	@Value("${naver-api.accessKey}")
	private String accessKey;
	
	private final String mailApiUrl = "https://mail.apigw.ntruss.com";
	private final String mailEndpoint = "/api/v1";
	private final String sendMailUri = "/mails";
	
	/** 네이버 API 메일 발송 요청
	 * @param naverMailPayload
	 * @throws MailSendException {@link #sendMail}
	 * @return 메일 발송 요청 Response Status Code
	 */
	public int sendMail(NaverMailPayload naverMailPayload) {
		
		try {
			
			String time = Long.toString(System.currentTimeMillis());
			
			// 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-ncp-apigw-timestamp", time);
			headers.set("x-ncp-iam-access-key", accessKey);
			headers.set("x-ncp-apigw-signature-v2", naverSignatureGenerator.getNaverSignature("POST", mailEndpoint + sendMailUri, time));
			
			// HTTP 요청
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<NaverMailPayload> entity = new HttpEntity<>(naverMailPayload, headers);
			ResponseEntity<String> response = restTemplate.exchange(new URI(mailApiUrl + mailEndpoint + sendMailUri),
					HttpMethod.POST, entity, String.class);
			
			
			return response.getStatusCode().value();
		} catch (Exception e) {
			throw new MailSendException(MAIL_SEND_FAIL,e);
		}
	}
	
}
