package com.quakoo.webframework;

/**
 * @author liyongbiao
 */
public class Config {

    public String logpath = ".";
    public int port = 8081;
    public int min_thread = 128;
    public int max_thread = 512;
    public int max_idle_time = 60000;
    public boolean log = false;
    public String web_xml = "webapp/WEB-INF/web.xml";
    public String webapp = "src/main/webapp";
    public String scanclass = "com.systoon";
    public String contextPath = "/api";
    public String tempDir = null;//
    public int maxFormContentSize=20000000;//form 最大
    public double minWorkNumFactor = 0; // 最小的work线程数目，相对于maxThread来说。0:可以到0,
    // 0.1:不能小于maxTHread的10%,
    // 0.01:不能小于maxTHread的1%
    public double maxQueueSizeFactor = 2; // 最大的queue数目，相对于maxThread来说。0:不能超过0,
    // 1:不能大于maxTHread的100%,
    // 2:不能大于maxTHread的200%
    public int checkTime = 500; //jetty 线程监控时间 默认500ms检查一次


    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public int getMin_thread() {
        return min_thread;
    }

    public void setMin_thread(int min_thread) {
        this.min_thread = min_thread;
    }

    public int getMax_thread() {
        return max_thread;
    }

    public void setMax_thread(int max_thread) {
        this.max_thread = max_thread;
    }

    public String getLogpath() {
        return logpath;
    }

    public void setLogpath(String logpath) {
        this.logpath = logpath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMAX_IDLE_TIME() {
        return max_idle_time;
    }

    public void setMAX_IDLE_TIME(int mAX_IDLE_TIME) {
        max_idle_time = mAX_IDLE_TIME;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public int getMax_idle_time() {
        return max_idle_time;
    }

    public void setMax_idle_time(int max_idle_time) {
        this.max_idle_time = max_idle_time;
    }

    public String getWeb_xml() {
        return web_xml;
    }

    public void setWeb_xml(String web_xml) {
        this.web_xml = web_xml;
    }

    public String getWebapp() {
        return webapp;
    }

    public void setWebapp(String webapp) {
        this.webapp = webapp;
    }

    public String getScanclass() {
        return scanclass;
    }

    public void setScanclass(String scanclass) {
        this.scanclass = scanclass;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public double getMinWorkNumFactor() {
        return minWorkNumFactor;
    }

    public void setMinWorkNumFactor(double minWorkNumFactor) {
        this.minWorkNumFactor = minWorkNumFactor;
    }

    public double getMaxQueueSizeFactor() {
        return maxQueueSizeFactor;
    }

    public void setMaxQueueSizeFactor(double maxQueueSizeFactor) {
        this.maxQueueSizeFactor = maxQueueSizeFactor;
    }

    public static void main(String[] sfe) {
        System.out.println(Config.class.getSimpleName());
    }

    public int getMaxFormContentSize() {
        return maxFormContentSize;
    }

    public void setMaxFormContentSize(int maxFormContentSize) {
        this.maxFormContentSize = maxFormContentSize;
    }
    
    
    
    	public int getCheckTime() {
        		return checkTime;
        	}
    
    	public void setCheckTime(int checkTime) {
        		this.checkTime = checkTime;
        	}
    
}
