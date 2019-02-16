package com.quakoo.space.annotation.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quakoo.space.enums.JsonTypeReference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * domain的属性的标识
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HyperspaceColumn {



    /**
     * 是否映射数据库
     * 
     * @return
     */
    boolean isDbColumn() default true;

    /**
     * 数据库的字段名，如果名字一样 这个可以不填。
     * 
     * @return
     */
    String column() default "";


    /**
     * 映射json字段，填写了对应的json字段后。
     *
     * 从数据库转换的时候，会自动把json字符串转换为对应的对象
     * 保存到数据库的时候，会自动把要转换的对象转换为json字符串
     *
     *
     * 例子：
     *  1.转为常见的集合类型
     *
     * 	@HyperspaceColumn(jsonTypeReference = JsonTypeReference.list_long)
     * 	private List<Long> uids;//用户
     *
     *
     *  2.转为具体的java类
     *  @HyperspaceColumn(jsonType = MockQuestionList.class)
     * 	private MockQuestionList questions;//场景问题
     *
     *
     *  参考类:
     *  public class MockQuestionList implements Serializable {
     *     private List<MockQuestion> mockQuestions=new ArrayList<>();
     *     ...
     *      getsetMethod
     *     ...
     *  }
     *
     *  public class MockQuestion implements Serializable{
     *
     * 	    private String question;//场景问题
     * 	    private String answer;//场景答案
     * 	    private int spaceTime;//间隔时间
     * 	    private int requestTime;//响应时间
     * 	    ...
     *      getsetMethod
     *      ...
     *  }
     *
     *
     *
     */
    Class jsonType() default Object.class;


    /**
     * 映射json字段，填写了对应的json字段后。
     *
     * 从数据库转换的时候，会自动把json字符串转换为对应的对象
     * 保存到数据库的时候，会自动把要转换的对象转换为json字符串
     *
     *
     * 例子：
     *  1.转为常见的集合类型
     *
     * 	@HyperspaceColumn(jsonTypeReference = JsonTypeReference.list_long)
     * 	private List<Long> uids;//用户
     *
     *
     *  2.转为具体的java类
     *  @HyperspaceColumn(jsonType = MockQuestionList.class)
     * 	private MockQuestionList questions;//场景问题
     *
     *
     *  参考类:
     *  public class MockQuestionList implements Serializable {
     *     private List<MockQuestion> mockQuestions=new ArrayList<>();
     *     ...
     *      getsetMethod
     *     ...
     *  }
     *
     *  public class MockQuestion implements Serializable{
     *
     * 	    private String question;//场景问题
     * 	    private String answer;//场景答案
     * 	    private int spaceTime;//间隔时间
     * 	    private int requestTime;//响应时间
     * 	    ...
     *      getsetMethod
     *      ...
     *  }
     *
     *
     *
     */
    JsonTypeReference jsonTypeReference() default JsonTypeReference.type_null;

}
