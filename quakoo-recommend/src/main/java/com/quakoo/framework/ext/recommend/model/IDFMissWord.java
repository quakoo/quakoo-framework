package com.quakoo.framework.ext.recommend.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class IDFMissWord implements Serializable {

    private long id;

    private String word;

    private int num;

}
