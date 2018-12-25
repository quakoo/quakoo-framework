package com.quakoo.baseFramework.redis.util;

/**
 * 
 * @author LiYongbiao
 *
 */
public class Test {

	
	static void OnException (Test opo){
		opo=null;
	}
	
	static void test(){
		Test test=null;
		try{
			test=new Test();
			throw new RuntimeException();
		}catch(Exception e){
			OnException(test);
		}finally{
			System.out.print(test);
		}
		
	}

    public static void main(String... avgs){
    	test();

    }
}
