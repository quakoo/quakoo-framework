package com.quakoo.baseFramework.transaction;

public abstract class RetryActrion<T> {

	private int retry_num;// 重试次数

	public RetryActrion(int retry_num) {
		super();
		this.retry_num = retry_num;
	}
	
	public abstract T action() throws Exception;
	
	public T run() throws Exception{
		 T res = null;
		 boolean sign = true;
         int num = 0;
         Exception e = null;
         while(sign) {
         	if(num++ >= retry_num) break;
         	try {
         		res = action();
             	sign = false;
 			} catch (Exception e1) {
 				e = e1;
 			}
         }
         if(sign && null != e) throw e;
         return res;
	}
}
