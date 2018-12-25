package com.quakoo.baseFramework.redis;
import java.util.Map;

/**
 * 
 * @author LiYongbiao
 *
 */
public class JedisBean {

    public JedisBean() {
    }

    public Object getObject() throws Exception {
        return this;
    }

    public Class getObjectType() {
        return this.getClass();
    }

    public boolean isSingleton() {
        return true;
    }

    public Map<String, Integer> getDbIndexMap() {
        return dbIndexMap;
    }

    public void setDbIndexMap(Map<String, Integer> dbIndexMap) {
        this.dbIndexMap = dbIndexMap;
    }

    public String getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    

    public int getThreadPoolCoreSize() {
		return threadPoolCoreSize;
	}

	public void setThreadPoolCoreSize(int threadPoolCoreSize) {
		this.threadPoolCoreSize = threadPoolCoreSize;
	}

	public int getThreadPoolMaxSize() {
		return threadPoolMaxSize;
	}

	public void setThreadPoolMaxSize(int threadPoolMaxSize) {
		this.threadPoolMaxSize = threadPoolMaxSize;
	}



	private Map<String, Integer> dbIndexMap;

    private String masterAddress;

    private String password;
    
    private int threadPoolCoreSize=12;
    private int threadPoolMaxSize=128;
}
