package net.chamman.moonnight.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// DB Status == STAY
@ResponseStatus(HttpStatus.FORBIDDEN)
public class StatusStayException extends CustomException{

	public StatusStayException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusStayException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusStayException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusStayException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
  
}
