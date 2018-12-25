package com.quakoo.baseFramework.ip;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author liyongbiao
 *
 */
public class HostParser
{
	static Logger logger=LoggerFactory.getLogger(HostParser.class);
	public static String[] server2IPPort(String serverHost)
	{
		String[] ret = new String[2];
		String[] temp=serverHost.split("/");
		String ipPort=null;
		if(temp!=null&&temp.length>=3)
		{
			ipPort=temp[2];
		}
		else return null;
		ret = ipPort.split(":");	
		try{
			Integer.parseInt(ret[1]);
		}catch(Exception e)
		{
			logger.error(e.getMessage(), e);
			return null;
		}
		return ret;
		
	}

	public static  List<String[]> server2IPPort(List<String>serverHosts)
	{
		ArrayList<String[]> retArray=new ArrayList<String[]>();
		for(String serverHost:serverHosts)
		{
			String[] ret = new String[2];
			String[] temp=serverHost.split("/");
			String ipPort=null;
			if(temp!=null&&temp.length>=3)
			{
				ipPort=temp[2];
			}
			else return null;
			ret = ipPort.split(":");	
			try{
				Integer.parseInt(ret[1]);
			}catch(Exception e)
			{
				logger.error(e.getMessage(), e);
				return null;
			}
			retArray.add(ret);
		}
		
		return retArray;
	}
	
//	public static void main(String[] args)
//	{
//		String[] ret=HostParser.server2IPPort("http://10.0.3.69:6004/");
//		for(String s:ret)
//		{
//			System.out.println(s);
//		}
//		List<String[]> rets = HostParser.server2IPPort(new ArrayList<String>(){
//			{
//				add("http://10.0.3.69:6005/");
//				add("http://10.0.3.69:6006/");
//			}
//			
//		});
//		for(String[] ss:rets)
//		{
//			for(String s:ss)
//			{
//				System.out.println(s);
//			}
//		}
//	}
}
