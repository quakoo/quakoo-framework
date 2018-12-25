package com.quakoo.hbaseFramework;

/**
 * Created by 136249 on 2015/5/20.
 */
public class AsyncInsertValue {
    private String key;
    private Object value;
    private int expireSecond;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getExpireSecond() {
        return expireSecond;
    }

    public void setExpireSecond(int expireSecond) {
        this.expireSecond = expireSecond;
    }

    @Override
    public String toString() {
        return "AsyncInsertValue{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", expireSecond=" + expireSecond +
                '}';
    }
}
