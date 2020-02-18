package com.quakoo.framework.ext.recommend.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SearchRes implements Serializable {

    private long id;

    private double score;

    private long time;

    private Map<String, String> columns;

    public SearchRes() {
    }

    public SearchRes(long id, double score, long time) {
        this.id = id;
        this.score = score;
        this.time = time;
    }

}
