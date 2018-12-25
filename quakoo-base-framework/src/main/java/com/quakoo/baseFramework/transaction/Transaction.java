package com.quakoo.baseFramework.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;



public class Transaction {
	
	private int retry_num;
	
	private int sleep_time;

    private List<TranHandler> handlers;
    
    private Class<?> clazz;
	
	public Transaction(Class<?> clazz, List<TranHandler> handlers, int retry_num, int sleep_time){
		this.clazz = clazz;
		this.handlers = handlers;
		if(retry_num<2) this.retry_num = 1;
		else this.retry_num = retry_num;
		this.sleep_time = sleep_time;
	}   
	
	public boolean handle() {
		
		Logger log = LoggerFactory.getLogger(clazz);
		
		boolean res = true;
		for(int i = 0; i < retry_num; i++){
			List<TranHandler> over_handlers = new ArrayList<TranHandler>();
			for (TranHandler one : handlers) {
				try {
					one.commit_method();
					res = true;
				} catch (Exception e) {
					res = false;
					log.error("",e);
					break;
				}finally{
					over_handlers.add(one);
				}
			}
	        if(!res){
	        	for (TranHandler one : over_handlers) {
	        		try {
						one.callback_method();
					} catch (Exception e) {
						log.error("",e);
					}
	        	}
	        }else
	        	break;
	        
	        if(retry_num != 1 && i != retry_num-1){
	        	Uninterruptibles.sleepUninterruptibly(sleep_time, TimeUnit.MILLISECONDS);
	        }
		}
		return res;
	}

	public static void main(String[] args) {
		
		TranHandler h1 = new TranHandler() {

			@Override
			public void commit_method() {
				System.out.println("commit 1");
			}

			@Override
			public void callback_method() {
				System.out.println("callback 1");
			}
		};

		TranHandler h2 = new TranHandler() {
			
			@Override
			public void commit_method() throws Exception {
				System.out.println("commit 2");
				throw new Exception("test");
			}
			
			@Override
			public void callback_method() throws Exception {
				System.out.println("callback 2");
			}
		};
		
		List<TranHandler> list = Lists.newArrayList();
		list.add(h1);
		list.add(h2);
		Transaction t = new Transaction(Transaction.class, list, 2 , 2000);
		System.out.println("==== "+t.handle());
	}
}
