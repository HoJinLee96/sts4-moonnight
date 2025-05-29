package net.chamman.moonnight.global.exception;

public abstract class CriticalException extends CustomException{

	public CriticalException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
		// TODO Auto-generated constructor stub
	}

	public CriticalException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
		// TODO Auto-generated constructor stub
	}

	public CriticalException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
		// TODO Auto-generated constructor stub
	}

	public CriticalException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
		// TODO Auto-generated constructor stub
	}

	
}
