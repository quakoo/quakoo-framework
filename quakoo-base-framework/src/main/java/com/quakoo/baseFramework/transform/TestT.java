package com.quakoo.baseFramework.transform;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestT {

    private long id;
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestT{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public static void main(String[] args) {
        List<TestT> list = Lists.newArrayList();
        TestT a = new TestT();
        a.setId(1);
        a.setName("a");
        list.add(a);
        TestT b = new TestT();
        b.setId(3);
        b.setName("b");
        list.add(b);

        TransformMapUtils mapUtils = new TransformMapUtils(TestT.class);
        Map<Long, TestT> map = mapUtils.listToMap(list,"id");
        System.out.println(map.toString());

        TransformFieldSetUtils transformFieldListUtils = new TransformFieldSetUtils(TestT.class);
        Set<Long> ids = transformFieldListUtils.fieldList(list, "id");
        System.out.println(ids.toString());

    }
}
