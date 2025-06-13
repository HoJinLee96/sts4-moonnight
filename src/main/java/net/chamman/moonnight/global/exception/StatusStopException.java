package net.chamman.moonnight.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class StatusStopException extends CustomException{

	public StatusStopException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusStopException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusStopException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusStopException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
}
