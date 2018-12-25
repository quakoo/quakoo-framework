package com.quakoo.space.annotation.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.HyperspaceDomainType;
import com.quakoo.space.enums.IdentityType;

/**
 * 用以标识domain类.
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HyperspaceDomain {

    /**
     * 是否缓存对象
     *
     * @return
     */
    boolean cacheObject() default true;

    /**
     * 本数据结构的类型
     *
     * @return
     */
    HyperspaceDomainType domainType();

    /**
     * 主键自增策略
     *
     * @return
     */
    IdentityType identityType() default IdentityType.identity;

    /**
     * 数据库对应的表名，如果名字不一样 就要填(暂时没有用，tablename使用配置文件)
     *
     * @return
     */
    String tableName() default "";

    /**
     * list数据结构不能动态分表分库，高水位属性无效<br>
     * 例子：@HyperspaceDomain(seeds={2})<br>
     * 分两个表
     *
     * 主数据结构 数据库分表<br>
     * 例子：@HyperspaceDomain(tableShardingHighWaters={10000,100000,
     * Long.MAX_VALUE},seeds={0,8,20})<br>
     * 分表字段小于10000分1个表<br>
     * 分表字段大于10000，并且于100000分8个表<br>
     * 分表字段大于100000分20个表<br>
     *
     *
     */
    long[] tableShardingHighWaters() default { Long.MAX_VALUE };

    /**
     * list数据结构只能填一个值
     *
     * @return
     */
    int[] tableShardingSeeds() default { 0 };

    /**
     * 和分表的规则一样
     *
     * @return
     */
    long[] DbShardingHighWaters() default { Long.MAX_VALUE };

    int[] DbShardingSeeds() default { 0 };
    
    boolean hibernateDbName() default false;

    boolean hasSort() default true;

}
