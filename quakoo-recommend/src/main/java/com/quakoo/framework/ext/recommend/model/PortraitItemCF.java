package com.quakoo.framework.ext.recommend.model;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.PortraitWord;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户画像(itemCF)
 * class_name: Portrait
 * package: com.quakoo.framework.ext.recommend.model
 * creat_user: lihao
 * creat_date: 2020/1/10
 * creat_time: 17:19
 **/
@Data
public class PortraitItemCF implements Serializable {

    private long uid;

    private List<PortraitWord> words = Lists.newArrayList();

    private long utime;

}
