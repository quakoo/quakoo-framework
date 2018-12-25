package com.quakoo.baseFramework.exception;

/**
 * 服务器繁忙
 * @author LiYongbiao1
 *
 */
public class ServerBusyException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -155133280318131205L;

	public ServerBusyException() {
		super();
	}

	public ServerBusyException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerBusyException(String message) {
		super(message);
	}

	public ServerBusyException(Throwable cause) {
		super(cause);
	}

	
}
