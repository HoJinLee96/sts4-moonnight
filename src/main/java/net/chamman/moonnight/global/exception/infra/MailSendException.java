package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class MailSendException extends CriticalException{

	public MailSendException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);

	}

	public MailSendException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);

	}

	public MailSendException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);

	}

	public MailSendException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);

	}
	
}
