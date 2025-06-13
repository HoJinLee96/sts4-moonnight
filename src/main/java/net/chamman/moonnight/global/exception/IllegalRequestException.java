package net.chamman.moonnight.global.exception;

// request value가 입력값에 대해 문제가 있을때
public class IllegalRequestException extends CustomException{

	public IllegalRequestException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public IllegalRequestException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public IllegalRequestException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public IllegalRequestException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

}
