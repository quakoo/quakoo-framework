package com.quakoo.framework.ext.recommend.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class RecallItem implements Serializable {

    private long id;

    private int type;

    public RecallItem() {
    }

    public RecallItem(long id, int type) {
        this.id = id;
        this.type = type;
    }

}
