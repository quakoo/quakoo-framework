package com.quakoo.baseFramework.serialize;


/**
 *
 * 注意：
 *  扩展的时候和protobuf的扩展规则基本一致
 * 1.扩展的时候只能往尾部追加属性（新加的属性的index必须大于之前的一个）。
 * 2.不能删除之前属性的SerializableProperty注解。
 * 3.不加SerializableProperty注解的属性，不会进行序列化和反序列化。
 * 4.不支持Long,Integer等类型。这些类型请用other。
 *
 *
 * Created by 136249 on 2015/3/14.
 *
 *
 */
public interface ScloudSerializable {


}
