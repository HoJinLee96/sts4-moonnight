package net.chamman.moonnight.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HttpStatusCode {
	
	// Success
	SUCCESS(200,"2000","message.success"),
	SUCCESS_NO_DATA(200,"2001","message.success.no_data"),
	READ_SUCCESS(200,"2002","message.success.read"),
	READ_SUCCESS_NO_DATA(200,"2003","message.success.read_no_data"),
	CREATE_SUCCESS(201,"2010","message.success.create"),
	UPDATE_SUCCESS(201,"2011","message.success.update"),
	DELETE_SUCCESS(201,"2012","message.success.delete"),
	
	// Common
	INVALID_VALUE(400, "4000", "message.common.invalid_input_value"),
	NULL_INPUT_VALUE(400, "4001", "message.common.null_input_value"),
	REQUEST_BODY_NOT_VALID(400, "4002", "message.common.request_body_not_valid"),
	METHOD_NOT_ALLOWED(405, "4050", "message.common.method_not_allowed"),
	INTERNAL_SERVER_ERROR(500, "5000", "message.common.internal_server_error"),
	TOO_MANY_REQUEST(429,"4290","요청 횟수를 초과했습니다.\n잠시 후 시도해 주세요."),

	
	// Sign 452
	USER_NOT_FOUND(404,"4520","message.sign.user_not_found"),
	SIGNIN_FAILED(404,"4521","message.sign.signin_failed"),
	SIGNIN_FAILED_OUT(404,"4522","message.sign.signin_failed_out"),
	AUTHENTICATION_FAILED(401,"4523","message.sign.authentication_failed"),
	AUTHORIZATION_FAILED(403,"4524","message.sign.authorization_failed"),
	
	// User 453
	EMAIL_NOT_EXISTS(200,"2070","message.user.email_not_exists"),
	PHONE_NOT_EXISTS(200,"2071","message.user.phone_not_exists"),
	EMAIL_ALREADY_EXISTS(200,"2072","message.user.email_already_exists"),
	PHONE_ALREADY_EXISTS(200,"2073","message.user.phone_already_exists"),
	USER_STATUS_STAY(403,"4530","message.user.status_stay"),
	USER_STATUS_STOP(401,"4531","message.user.status_stop"),
	USER_STATUS_DELETE(410,"4532","message.user.status_delete"),
	
	
	// Address 454
	ADDRESSS_NOT_FOUND(404,"4540","message.address.not_found"),
	ADDRESS_INVALID_VALUE(400,"4542","message.address.invalid_value"),
	
	// Comment 455
	COMMENT_NOT_FOUND(404,"4550","message.comment.not_found"),
	COMMENT_DELETED(400,"4551","message.comment.deleted"),

	
	// Estimate 456
	ESTIMATE_NOT_FOUND(404,"4560","message.estimate.not_found"),

	
	// JWT Token 457
	JWT_ILLEGAL(404,"4570","meesage.token.not_found"), // 토큰 없음
	JWT_EXPIRED(401,"4571","message.token.expired"), // 토큰 만료
	JWT_CREATE_FIAL(500,"5001","error.message"), // 토큰 생성 실패
	
	// Token 458
	TOKEN_NOT_FOUND(404,"4580","meesage.token.not_found"), // 토큰 없음
	TOKEN_ILLEGAL(400,"4581","message.token.illegal"), // 토큰 이상
	TOKEN_VALUE_MISMATCH(400,"4582","message.token.illegal"), // 토큰 값 불일치
	TOKEN_GET_FIAL(500,"5001","error.message"), // 토큰 조회 실패
	TOKEN_SET_FIAL(500,"5001","error.message"), // 토큰 Redis 저장 실패
	
	// Verification 459
	VERIFICATION_NOT_FOUND(404,"4590","message.verification.not_found"),
	VERIFICATION_EXPIRED(400,"4591","message.verification.expired"),
	TOO_MANY_VERIFY(429,"4592","message.verification.too_many_verify"),
	MISMATCH_VERIFICATION_CODE(400,"4593","message.verification.mismatch_code"),
	MISMATCH_RECIPIENT(400,"4594","message.verification.mismatch_recipient"),
	NOT_VERIFY(404,"4594","message.verification.not_verify"),
	
	
	// Crypto 460
	ENCRYPT_FAIL(500,"4601","message.common.internal_server_error"),
	DECRYPT_FAIL(500,"4602","message.common.internal_server_error"),
	
	// sms
	SMS_SEND_FAIL(500,"4602","message.common.internal_server_error")
	
	
	
	
	
	
	; 
	
	
	
	
	// 필요한 만큼 쭉쭉 추가하면 됨!
	
	private final int status;
	private final String code;
	private final String messageKey;

}
