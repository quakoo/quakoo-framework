package com.quakoo.baseFramework.exception;

/**
 * 
 * @author liyongbiao
 *
 */
public class BaseBusinessException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public String errorCode="500";
	public String completeUrl;
	public boolean printStack=true;


	public BaseBusinessException() {
	}

	public BaseBusinessException(String message) {
		super(message);
	}

	public BaseBusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public BaseBusinessException(Throwable cause) {
		super(cause);
	}

	public BaseBusinessException(String message, Throwable cause, String completeUrl) {
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
