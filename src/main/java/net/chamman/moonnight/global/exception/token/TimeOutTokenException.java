package net.chamman.moonnight.global.exception.token;

import net.chamman.moonnight.global.exception.CustomException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class TimeOutTokenException extends CustomException{

	public TimeOutTokenException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
		// TODO Auto-generated constructor stub
	}

	public TimeOutTokenException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
		// TODO Auto-generated constructor stub
	}

	public TimeOutTokenException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
		// TODO Auto-generated constructor stub
	}

	public TimeOutTokenException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
		// TODO Auto-generated constructor stub
	}

	
	
}
