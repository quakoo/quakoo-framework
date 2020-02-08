package com.quakoo.framework.ext.recommend.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchRes implements Serializable {

    private long id;

    private double score;

    private long time;

    public SearchRes() {
    }

    public SearchRes(long id, double score, long time) {
        this.id = id;
        this.score = score;
        this.time = time;
    }

}
