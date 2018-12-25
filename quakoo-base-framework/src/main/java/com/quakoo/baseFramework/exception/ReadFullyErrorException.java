package com.quakoo.baseFramework.exception;

import java.io.IOException;

public class ReadFullyErrorException extends IOException{

	public ReadFullyErrorException() {
		super();
	}

	public ReadFullyErrorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReadFullyErrorException(String message) {
		super(message);
	}

	public ReadFullyErrorException(Throwable cause) {
		super(cause);
	}
	

}
