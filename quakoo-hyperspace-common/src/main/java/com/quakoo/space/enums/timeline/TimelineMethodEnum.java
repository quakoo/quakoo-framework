package com.quakoo.space.enums.timeline;

/**
 * timeline方法类型
 * @author LiYongbiao1
 *
 */
public enum TimelineMethodEnum {
	/**
	 * 分页获取数据
	 */
    getPageList(0, "getPageList"),//
    /**
     * 获取记数
     */
    getCount(1, "getCount"),//
    /**
     * 重建timeline
     */
    rebuild(2, "rebuild");//


    TimelineMethodEnum(int id, String name) {
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
