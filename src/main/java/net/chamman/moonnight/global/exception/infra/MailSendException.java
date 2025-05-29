package net.chamman.moonnight.global.exception.infra;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class MailSendException extends CriticalException{

	public MailSendException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
		// TODO Auto-generated constructor stub
	}

	public MailSendException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
		// TODO Auto-generated constructor stub
	}

	public MailSendException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
		// TODO Auto-generated constructor stub
	}

	public MailSendException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
		// TODO Auto-generated constructor stub
	}
	
}
