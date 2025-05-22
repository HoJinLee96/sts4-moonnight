package net.chamman.moonnight.infra.kakao;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESS_INVALID_VALUE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.INTERNAL_SERVER_ERROR;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import net.chamman.moonnight.global.exception.infra.DaumStateException;
import net.chamman.moonnight.global.exception.infra.IllegalAddressValueException;

@Component
@PropertySource("classpath:application.properties")
public class DaumMapClient {
	
	@Value("${kakao-addressSearch.baseUrl}")
	private String baseUrl;
	@Value("${kakao-api.restApiKey}")
	private String kakaoApiRestApiKey;
	
	/**
	 * @param postcode
	 * @param mainAddress
	 * 
	 * @throws IllegalAddressValueException {@link #validateAddress} 일치하는 주소가 없음
	 * @throws DaumStateException {@link #validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @return
	 */
	public boolean validateAddress(String postcode, String mainAddress) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "KakaoAK "+kakaoApiRestApiKey);
		
		UriComponents uriComponents = UriComponentsBuilder
				.fromUriString(baseUrl)
				.queryParam("query", mainAddress)
				.queryParam("analyze_type", "exact")
				.build();
		
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<>(headers);
		
		ResponseEntity<Map> response = restTemplate.exchange(
				uriComponents.toUriString(), 
				HttpMethod.GET, 
				entity, 
				Map.class
				);
		if(response.getStatusCode()==HttpStatus.OK) {
			Map<String, Object> responseBody = response.getBody();
			List<Map<String, Object>> documents = (List<Map<String, Object>>) responseBody.get("documents");
			
			for (Map<String, Object> document : documents) {
				Map<String, Object> roadAddress = (Map<String, Object>) document.get("road_address");
				
				if (roadAddress != null && roadAddress.containsKey("zone_no")) {
					String zoneNo = (String) roadAddress.get("zone_no");
					if (zoneNo.equals(postcode)) {
						return true;
					}
				}
			}
			// 반복문을 다 돌았지만 일치하는 주소가 없는 경우
			throw new IllegalAddressValueException(ADDRESS_INVALID_VALUE,"일치하는 주소가 없음.");
		}else {
			throw new DaumStateException(INTERNAL_SERVER_ERROR,"주소 검증 요청 실패 : 다음 서버에서 응답을 받을 수 없습니다.");
		}
	}
	
}
