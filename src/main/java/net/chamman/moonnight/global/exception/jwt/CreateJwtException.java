package net.chamman.moonnight.global.exception.jwt;

import net.chamman.moonnight.global.exception.CriticalException;
import net.chamman.moonnight.global.exception.HttpStatusCode;

public class CreateJwtException extends CriticalException{

	public CreateJwtException(HttpStatusCode httpStatusCode, Exception e) {
		super(httpStatusCode, e);
		// TODO Auto-generated constructor stub
	}

	public CreateJwtException(HttpStatusCode httpStatusCode, String message, Exception e) {
		super(httpStatusCode, message, e);
		// TODO Auto-generated constructor stub
	}

	public CreateJwtException(HttpStatusCode httpStatusCode, String message) {
		super(httpStatusCode, message);
		// TODO Auto-generated constructor stub
	}

	public CreateJwtException(HttpStatusCode httpStatusCode) {
		super(httpStatusCode);
		// TODO Auto-generated constructor stub
	}
	
	

}
