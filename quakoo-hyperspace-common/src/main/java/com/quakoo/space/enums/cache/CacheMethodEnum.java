package com.quakoo.space.enums.cache;

/**
 * 缓存方法类型
 * @author LiYongbiao1
 *
 */
public enum CacheMethodEnum {
	/**
	 * 普通获取list。需要传入size，和分表字段
	 */
    getList(0, "getList"), //
    /**
     * 获取所有数据。需要传入分表字段
     */
    getAllList(1, "getAllList"), //
    /**
     * 获取分页的list。需要传入cursor和size，分表字段
     */
    getPageList(2, "getPageList"), //
    /**
     * 获取计数,需要传入分表字段
     */
    getCount(3, "getCount"), //
    /**
     * 获取所有的数据，不传入分表字段
     */
    getAllListWithoutSharding(4, "getAllListWithoutSharding"),//
    /**
     * 获取分页的list。需要传入cursor和size，不传入分表字段
     */
    getPageListWithoutSharding(5, "getPageListWithoutSharding"),
    /**
     * 获取计数,不传入分表字段
     */
    getCountWithoutSharding(6, "getCountWithoutSharding"),
    /**
     * 获取聚合或的list结构
     */
    getMergeList(7,"getMergeList"),
    // addOne(4, "addOne"), //
    // deleteOne(5, "deleteOne");
    
    getListWithoutSharding(8, "getListWithoutSharding"), 
    
    getRank(9, "getRank"); //

    CacheMethodEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    private final int id;

    private final String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
