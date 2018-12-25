package com.quakoo.baseFramework.transaction;

public abstract class TranHandler {

	public abstract void commit_method() throws Exception;

	public abstract void callback_method() throws Exception;

}
