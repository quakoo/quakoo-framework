package com.quakoo.space.annotation;

/**
 * 
 */
public class HyperspacePrimaryId implements HyperspaceId {

    /**
     * 
     */
    private static final long serialVersionUID = -7315799754307130311L;

    private long id;

    private long sharding;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSharding() {
        return sharding;
    }

    public void setSharding(long sharding) {
        this.sharding = sharding;
    }

    public HyperspacePrimaryId(long id) {
        this.id = id;
    }

    public HyperspacePrimaryId(long id, long sharding) {
        this.id = id;
        this.sharding = sharding;
    }

    @Override
    public String toString() {
        return "HyperspacePrimaryId [id=" + id + ", sharding=" + sharding + "]";
    }

}
