package com.quakoo.space.enums.cache;

/**
 * dao方法参数的类型
 * @author LiYongbiao1
 *
 */
public enum CacheMethodParamEnum {
	/**
	 * 默认的类型（自动识别）
	 */
    NULL(-1, "null"), //
    /**
     * size
     */
    size(1, "size"), //
    /**
     * 游标
     */
    cursor(2, "cursor"), //
    
    item(3, "item"); //
  

    CacheMethodParamEnum(int id, String name) {
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
