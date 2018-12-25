package com.quakoo.baseFramework.serialize;

/**
 * Created by 136249 on 2015/3/14.
 */
public enum Type {
    /**
     * hexString
     */
    hexString,
    /**
     * 有符号int类型(非Integer)
     */
    int32,
    /**
     * 无符号int类型(非Integer)
     */
    uint32,
    /**
     * 有符号long类型
     */
    long64,
    /**
     * 无符号long类型(非Long)
     */
    ulong64,
    /**
     * String类型
     */
    string,
    /**
     * byte[]
     */
    byteArray,
    /**
     * float类型
     */
    floatType,
    /**
     * double类型
     */
    doubleType,
    /**
     * short类型
     */
    shortType,
    /**
     * booelan类型
     */
    booleanType,
    /**
     * char类型
     */
    characterType,

    /**
     * byte类型
     */
    byteType,

    /**
     * 其他可以序列化，反序列化的类
     */
    pojo,
    /**
     * 其他类型(hession压缩)
     */
    other
}
