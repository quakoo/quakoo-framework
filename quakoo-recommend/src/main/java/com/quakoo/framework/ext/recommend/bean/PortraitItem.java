package com.quakoo.framework.ext.recommend.bean;

import com.google.common.collect.Maps;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class PortraitItem implements Serializable {

    private long uid;

    private Map<String, Double> weightMap = Maps.newHashMap();

}
