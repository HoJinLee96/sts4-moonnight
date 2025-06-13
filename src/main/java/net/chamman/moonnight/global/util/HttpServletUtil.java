package net.chamman.moonnight.global.util;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class HttpServletUtil {
	
	public static void resSetCookie(HttpServletResponse res, String name, String value, Duration duration) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.path("/")
				.maxAge(duration)
				.sameSite("Lax")
				.build();
		
		res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
	
	public static void setErrorResponse(HttpServletResponse res, HttpStatusCode httpStatusCode, String message) throws IOException {
		res.setStatus(httpStatusCode.getStatus());
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");
		
		Map<String, Object> body = Map.of(
				"code", httpStatusCode.getCode(),
				"message", message
				);
		res.getWriter().write(new ObjectMapper().writeValueAsString(body));
	}  
	
}
