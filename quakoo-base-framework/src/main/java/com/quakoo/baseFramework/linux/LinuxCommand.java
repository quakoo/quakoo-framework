package com.quakoo.baseFramework.linux;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * kongdepeng
 * 2016年1月23日 下午7:35:59
 */
public class LinuxCommand {
	static Logger logger = LoggerFactory.getLogger(LinuxCommand.class);
	public static String newline = "<br>";
	
	/**
	 * 执行shell命令 String[] cmd = { "sh", "-c", "lsmod |grep linuxVmux" }或者
	 * String[] cmd = { "sh", "-c", "./load_driver.sh" } int tp = 1 返回执行结果 非1
	 * 返回命令执行后的输出
	 */
	public static String runCommand(String[] cmd, int tp) {
		logger.info( "LinuxCommand arra :" + Arrays.asList(cmd));
		StringBuffer buf = new StringBuffer(1000);
		String rt = "-1";
		try {
			Process pos = Runtime.getRuntime().exec(cmd);
			pos.waitFor();
			if (tp == 1) {
				if (pos.exitValue() == 0) {
					rt = "1";
				}
			} else {
				InputStreamReader ir = new InputStreamReader(
						pos.getInputStream());
				LineNumberReader input = new LineNumberReader(ir);
				String ln = "";
				while ((ln = input.readLine()) != null) {
					buf.append(ln + newline);
				}
				rt = buf.toString();
				input.close();
				ir.close();
			}
		} catch (java.io.IOException e) {
			rt = e.toString();
		} catch (Exception e) {
			rt = e.toString();
		}
		return rt;
	}

	/**
	 * 执行简单命令 String cmd="ls" int tp = 1 返回执行结果 非1 返回命令执行后的输出
	 */
	public static String runCommand(String cmd, int tp) {
		logger.info( "LinuxCommand alone:" + cmd);
		StringBuffer buf = new StringBuffer(1000);
		String rt = "-1";
		try {
			Process pos = Runtime.getRuntime().exec(cmd);
//			pos.waitFor();
			if (tp == 1) {
				if (pos.exitValue() == 0) {
					rt = "1";
				}
			} else {
				InputStreamReader ir = new InputStreamReader(
						pos.getInputStream());
				LineNumberReader input = new LineNumberReader(ir);
				String ln = "";
				while ((ln = input.readLine()) != null) {
					buf.append(ln + newline);
				}
				rt = buf.toString();
				input.close();
				ir.close();
			}
		} catch (java.io.IOException e) {
			rt = e.toString();
		} catch (Exception e) {
			rt = e.toString();
		}
		return rt;
	}
	
	public static String apacheExec(String command) {  
        try {  
        	DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();  
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();  
            CommandLine commandline = CommandLine.parse(command);  
            DefaultExecutor exec = new DefaultExecutor();  
            exec.setExitValues(null);  
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream,errorStream);  
            exec.setStreamHandler(streamHandler);  
            exec.execute(commandline,resultHandler);  
            resultHandler.waitFor();
            String out = outputStream.toString("gbk");  
            String error = errorStream.toString("gbk");  
            return out+error;  
        } catch (Exception e) {  
            return e.toString();  
        }  
    }  
}
