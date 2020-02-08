package com.quakoo.framework.ext.recommend.model;

import com.google.common.collect.Lists;
import com.quakoo.framework.ext.recommend.bean.ESField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SyncInfo implements Serializable {

    private long id;

    private String sql;

    private String esIndex;

    private List<ESField> esFields = Lists.newArrayList();

    private String trackingColumn;

    private int batchSize;

    private String esId;

    private long lastTrackingValue;

}
