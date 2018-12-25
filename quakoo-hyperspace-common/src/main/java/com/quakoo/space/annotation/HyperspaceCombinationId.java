package com.quakoo.space.annotation;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class HyperspaceCombinationId implements HyperspaceId {

    /**
     * 
     */
    private static final long serialVersionUID = 3523460801622710966L;

    private List<Object> list = new ArrayList<Object>();

    private long sharding;

    public long getSharding() {
        return sharding;
    }

    public void setSharding(long sharding) {
        this.sharding = sharding;
    }

    public List<Object> getList() {
        return list;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }

    public HyperspaceCombinationId() {
    }

    public HyperspaceCombinationId(Object... params) {
        if (params != null) {
            for (Object obj : params) {
                list.add(obj);
            }
        }
    }

    @Override
    public String toString() {
        return "HyperspaceCombinationId [list=" + list + ", sharding=" + sharding + "]";
    }

}
