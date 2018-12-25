package com.quakoo.baseFramework.util;

public class FormatUtils {

	public static String formatNum(int num){
		if(num>100000){
			num=num/10000;
			return Integer.toString(num)+"万";
		}else{
			return Integer.toString(num);
		}
	}
}
