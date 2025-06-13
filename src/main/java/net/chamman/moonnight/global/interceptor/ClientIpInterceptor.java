package net.chamman.moonnight.global.interceptor;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ClientIpInterceptor implements HandlerInterceptor {
	
	private List<String> headers = List.of(
			"X-Forwarded-For",
			"Proxy-Client-IP",
			"WL-Proxy-Client-IP",
			"HTTP_CLIENT_IP",
			"HTTP_X_FORWARDED_FOR"
			);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) {
		String ip = extractClientIp(request);
		request.setAttribute("clientIp", ip); // 컨트롤러에서 꺼내쓸 수 있게 저장
		return true; // 계속 진행
	}
	
	public String extractClientIp(HttpServletRequest request) {
		for (String header : headers) {
			String ip = request.getHeader(header);
			if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
				return ip.split(",")[0].trim(); // 첫 번째 IP만 사용
			}
		}
		return request.getRemoteAddr(); // 기본 IP 반환
	}

}
