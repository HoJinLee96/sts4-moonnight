package net.chamman.moonnight.global.exception;

public class DuplicationException extends CustomException{

	public DuplicationException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public DuplicationException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public DuplicationException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public DuplicationException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	

}