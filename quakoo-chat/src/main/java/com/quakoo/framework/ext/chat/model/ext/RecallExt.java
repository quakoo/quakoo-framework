package com.quakoo.framework.ext.chat.model.ext;

import com.google.common.collect.Maps;

import java.util.Map;

public class RecallExt {

    private int type = 100;

    private Map<String, Long> extra;

    public RecallExt() {
    }

    public RecallExt(long mid) {
        this.extra = Maps.newHashMap();
        this.extra.put("mid", mid);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Map<String, Long> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Long> extra) {
        this.extra = extra;
    }

}
