package com.quakoo.baseFramework.serialize;

/**
 * 
 * @author liyongbiao
 *
 */
public class SerializeException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public String errorCode="500";
	public String completeUrl;
	public boolean printStack=true;


	public SerializeException() {
	}

	public SerializeException(String message) {
		super(message);
	}

	public SerializeException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerializeException(Throwable cause) {
		super(cause);
	}

	public SerializeException(String message, Throwable cause, String completeUrl) {
		super(message, cause);
		this.completeUrl = completeUrl;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}



	public String getCompleteUrl() {
		return completeUrl;
	}

	public void setCompleteUrl(String completeUrl) {
		this.completeUrl = completeUrl;
	}

	public boolean isPrintStack() {
		return printStack;
	}

	public void setPrintStack(boolean printStack) {
		this.printStack = printStack;
	}
}
