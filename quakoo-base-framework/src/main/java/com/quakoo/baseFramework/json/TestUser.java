package com.quakoo.baseFramework.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestUser {
    private List<Map<Long,Integer>> sdf=new ArrayList<>();

    public List<Map<Long, Integer>> getSdf() {
        return sdf;
    }

    public void setSdf(List<Map<Long, Integer>> sdf) {
        this.sdf = sdf;
    }
}
