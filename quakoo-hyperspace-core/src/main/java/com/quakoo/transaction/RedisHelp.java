package com.quakoo.transaction;

import com.google.common.collect.Lists;
import com.quakoo.space.model.transaction.JedisMethodInfo;

import java.util.List;

public class RedisHelp {

    private List<JedisMethodInfo> methodInfos = Lists.newArrayList();

    public List<JedisMethodInfo> getMethodInfos() {
        return methodInfos;
    }

    public void setMethodInfos(List<JedisMethodInfo> methodInfos) {
        this.methodInfos = methodInfos;
    }

}
