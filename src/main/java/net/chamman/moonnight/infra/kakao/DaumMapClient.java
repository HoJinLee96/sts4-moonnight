package net.chamman.moonnight.infra.kakao;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESS_INVALID_VALUE;

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

import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.exception.infra.DaumStateException;
import net.chamman.moonnight.global.exception.infra.InvalidMainAddressException;
import net.chamman.moonnight.global.util.LogMaskingUtil;
import net.chamman.moonnight.global.util.LogMaskingUtil.MaskLevel;

@Component
@Slf4j
@PropertySource("classpath:application.properties")
public class DaumMapClient {

	@Value("${kakao-addressSearch.baseUrl}")
	private String baseUrl;
	@Value("${kakao-api.restApiKey}")
	private String kakaoApiRestApiKey;
	private RestTemplate restTemplate = new RestTemplate();

	/**
	 * @param postcode
	 * @param mainAddress
	 * 
	 * @throws IllegalAddressValueException {@link #validateAddress} 일치하는 주소가 없음
	 * @throws DaumStateException           {@link #validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean validateAddress(String postcode, String mainAddress) {
		log.debug("* 주소 검증 시작. mainAddress: [{}]", LogMaskingUtil.maskAddress(mainAddress, MaskLevel.MEDIUM));

		log.debug("[주소검증] 원본 주소: {}", mainAddress);

		String cleanedAddress = mainAddress.trim().replaceAll("\\s+", " ");
		log.debug("[주소검증] 전처리 주소: {}", cleanedAddress);

		// 1차: exact 시도
		if (queryKakao(postcode, mainAddress, "exact"))
			return true;

		// 2차: similar 시도
		if (queryKakao(postcode, mainAddress, "similar"))
			return true;

		// 3차: mainAddress가 길거나 숫자로 끝나는 경우 제거해서 다시 시도
		String fallbackAddress = mainAddress.replaceAll("\\s+\\d.*$", ""); // ex) "동교로 7" → "동교로"
		if (queryKakao(postcode, fallbackAddress, "similar"))
			return true;

		throw new InvalidMainAddressException(ADDRESS_INVALID_VALUE, "입력한 주소와 우편번호가 일치하지 않거나, 올바른 주소 형식이 아닙니다.");
	}

	private boolean queryKakao(String postcode, String address, String analyzeType) {
		try {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).queryParam("query", address)
					.queryParam("analyze_type", analyzeType).build();

			log.debug("[주소검증] 요청 URI: {}", uriComponents.toUriString());

			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "KakaoAK " + kakaoApiRestApiKey);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<Map> response = restTemplate.exchange(uriComponents.toUriString(), HttpMethod.GET, entity,
					Map.class);

			if (response.getStatusCode() != HttpStatus.OK)
				return false;

			Map<String, Object> body = response.getBody();
			log.debug("[주소검증] 카카오 응답 전체: {}", body);
			List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("documents");

			if (docs == null || docs.isEmpty())
				return false;

			for (Map<String, Object> doc : docs) {
				Map<String, Object> roadAddress = (Map<String, Object>) doc.get("road_address");
				if (roadAddress != null && postcode.equals(roadAddress.get("zone_no"))) {
					return true;
				}
			}
		} catch (Exception e) {
			log.warn("카카오 주소 검색 실패. address: [{}], analyzeType: [{}]", address, analyzeType, e);
		}
		return false;
	}

}
