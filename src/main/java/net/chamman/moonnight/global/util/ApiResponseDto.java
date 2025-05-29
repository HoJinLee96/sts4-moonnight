package net.chamman.moonnight.global.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.chamman.moonnight.global.exception.HttpStatusCode;

@Schema(description = "기본 응답 포맷")
@Getter
@AllArgsConstructor
public class ApiResponseDto<T> {
	
	@Schema(description = "응답 코드")
	HttpStatusCode httpStatusCode;
	@Schema(description = "응답 메시지")
	String message;
	@Schema(description = "응답 데이터", nullable = true)
	private T data;
	
	public static <T> ApiResponseDto<T> of(HttpStatusCode httpStatusCode, T data) {
		
		return new ApiResponseDto<>(httpStatusCode, "", data);
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
}
