package com.quakoo.baseFramework.http;
/**
 * 
 * @author liyongbiao
 *
 */
public class HttpPoolParam {

    public enum ProxyType {
        http, socket;
    }

    /**
     * 地址
     */
    private String proxyAddress;

    /**
     * 端口
     */
    private int proxyPort;

    /**
     * 连接超时时间
     */
    private int soTime;

    /**
     * 响应超时时间
     */
    private int connectioneTime;

    /**
     * 用户名
     */
    private String proxyUserName;

    /**
     * 密码
     */
    private String proxyPassword;

    /**
     * 代理类型
     */
    private ProxyType proxyType;

    /**
     * 代表每个HttpPool中connManager总的socket连接数
     */
    private int connManagerMaxTotal = 2000;

    /**
     * 代表每个HttpPool中connManager每个目标地址能够使用的socket连接数
     */
    private int connManagerMaxPerRoute = 200;

    /**
     * 是否保存cookie
     */
    private boolean cookie = true;

    /**
     * 重试次数
     */
    private int reTryTimes = 3;


    /**
     * 处理重定向
     */
    private boolean followRedirects=true;

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public int getSoTime() {
        return soTime;
    }

    public void setSoTime(int soTime) {
        this.soTime = soTime;
    }

    public int getConnectioneTime() {
        return connectioneTime;
    }

    public void setConnectioneTime(int connectioneTime) {
        this.connectioneTime = connectioneTime;
    }

    public String getProxyUserName() {
        return proxyUserName;
    }

    public void setProxyUserName(String proxyUserName) {
        this.proxyUserName = proxyUserName;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType;
    }

    public int getConnManagerMaxTotal() {
        return connManagerMaxTotal;
    }

    public void setConnManagerMaxTotal(int connManagerMaxTotal) {
        this.connManagerMaxTotal = connManagerMaxTotal;
    }

    public int getConnManagerMaxPerRoute() {
        return connManagerMaxPerRoute;
    }

    public void setConnManagerMaxPerRoute(int connManagerMaxPerRoute) {
        this.connManagerMaxPerRoute = connManagerMaxPerRoute;
    }

    public boolean isCookie() {
        return cookie;
    }

    public void setCookie(boolean cookie) {
        this.cookie = cookie;
    }

    public HttpPoolParam(int time) {
        this.soTime = time;
        this.connectioneTime = time;
    }

    public HttpPoolParam(int soTime, int connectioneTime, int reTryTimes) {
        this.soTime = soTime;
        this.connectioneTime = connectioneTime;
        this.reTryTimes = reTryTimes;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public int getReTryTimes() {
        return reTryTimes;
    }

    public void setReTryTimes(int reTryTimes) {
        this.reTryTimes = reTryTimes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + connManagerMaxPerRoute;
        result = prime * result + connManagerMaxTotal;
        result = prime * result + connectioneTime;
        result = prime * result + (cookie ? 1231 : 1237);
        result = prime * result + ((proxyAddress == null) ? 0 : proxyAddress.hashCode());
        result = prime * result + ((proxyPassword == null) ? 0 : proxyPassword.hashCode());
        result = prime * result + proxyPort;
        result = prime * result + ((proxyType == null) ? 0 : proxyType.hashCode());
        result = prime * result + ((proxyUserName == null) ? 0 : proxyUserName.hashCode());
        result = prime * result + reTryTimes;
        result = prime * result + soTime;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpPoolParam other = (HttpPoolParam) obj;
        if (connManagerMaxPerRoute != other.connManagerMaxPerRoute) {
            return false;
        }
        if (connManagerMaxTotal != other.connManagerMaxTotal) {
            return false;
        }
        if (connectioneTime != other.connectioneTime) {
            return false;
        }
        if (cookie != other.cookie) {
            return false;
        }
        if (proxyAddress == null) {
            if (other.proxyAddress != null) {
                return false;
            }
        } else if (!proxyAddress.equals(other.proxyAddress)) {
            return false;
        }
        if (proxyPassword == null) {
            if (other.proxyPassword != null) {
                return false;
            }
        } else if (!proxyPassword.equals(other.proxyPassword)) {
            return false;
        }
        if (proxyPort != other.proxyPort) {
            return false;
        }
        if (proxyType != other.proxyType) {
            return false;
        }
        if (proxyUserName == null) {
            if (other.proxyUserName != null) {
                return false;
            }
        } else if (!proxyUserName.equals(other.proxyUserName)) {
            return false;
        }
        if (reTryTimes != other.reTryTimes) {
            return false;
        }
        if (soTime != other.soTime) {
            return false;
        }
        return true;
    }

}
