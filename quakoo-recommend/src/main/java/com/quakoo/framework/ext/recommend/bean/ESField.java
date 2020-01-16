package com.quakoo.framework.ext.recommend.bean;

import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;


@Data
public class ESField implements Serializable {

    private String name;

    private String index; //true false

    private String type; //long text

    private String analyzer; //jieba_index

    private String mysqlColumn; //mysql对应的列


    public ESField() {
    }

    public ESField(String name, String index, String type, String analyzer, String mysqlColumn) {
        this.name = name;
        this.index = index;
        this.type = type;
        this.analyzer = analyzer;
        this.mysqlColumn = mysqlColumn;
    }


}
