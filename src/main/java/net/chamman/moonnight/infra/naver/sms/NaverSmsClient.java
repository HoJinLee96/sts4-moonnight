package net.chamman.moonnight.infra.naver.sms;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.infra.naver.NaverSignatureGenerator;

@Component
@RequiredArgsConstructor
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

    public int sendVerificationCode(NaverSmsPayload naverSmsPayload)
            throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, JsonProcessingException, URISyntaxException {

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
    }
}
