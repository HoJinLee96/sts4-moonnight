package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class DaumStateException extends CriticalException{

	public DaumStateException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public DaumStateException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public DaumStateException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public DaumStateException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
	

}
