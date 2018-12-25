package com.quakoo.baseFramework.exception;

/**
 * 服务初始化错误
 * @author LiYongbiao1
 *
 */
public class ServerInitError extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -155133280318131205L;

	public ServerInitError() {
		super();
	}

	public ServerInitError(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerInitError(String message) {
		super(message);
	}

	public ServerInitError(Throwable cause) {
		super(cause);
	}

	
}
