package net.chamman.moonnight.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

//DB상 status == DELETE인 경우
public class StatusDeleteException extends CustomException{

	public StatusDeleteException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public StatusDeleteException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}

	
  
}
