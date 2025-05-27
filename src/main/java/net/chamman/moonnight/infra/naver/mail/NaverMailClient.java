package net.chamman.moonnight.infra.naver.mail;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
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

  public int sendVerificationCode(NaverMailPayload naverMailPayload) {

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
  }
}
