package com.quakoo.space.enums;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public enum JsonTypeReference {

    type_null(0,null),

    list_long(1, new TypeReference<List<Long>>() {}), //
    list_integer(2, new TypeReference<List<Integer>>() {}), //
    list_string(3, new TypeReference<List<String>>() {}), //
    list_object(4, new TypeReference<List<Object>>() {}), //

    map_long_long(5, new TypeReference<Map<Long,Long>>() {}), //
    map_long_integer(6, new TypeReference<Map<Long,Integer>>() {}), //
    map_long_string(7, new TypeReference<Map<Long,String>>() {}), //
    map_long_object(8, new TypeReference<Map<Long,Object>>() {}), //

    map_integer_long(9, new TypeReference<Map<Integer,Long>>() {}), //
    map_integer_integer(10, new TypeReference<Map<Integer,Integer>>() {}), //
    map_integer_string(11, new TypeReference<Map<Integer,String>>() {}), //
    map_integer_object(12, new TypeReference<Map<Integer,Object>>() {}), //

    map_string_long(13, new TypeReference<Map<String,Long>>() {}), //
    map_string_integer(14, new TypeReference<Map<String,Integer>>() {}), //
    map_string_string(15, new TypeReference<Map<String,String>>() {}), //
    map_string_object(16, new TypeReference<Map<String,Object>>() {}), //

    map_object_long(17, new TypeReference<Map<Object,Long>>() {}), //
    map_object_integer(18, new TypeReference<Map<Object,Integer>>() {}), //
    map_object_string(19, new TypeReference<Map<Object,String>>() {}), //
    map_object_object(20, new TypeReference<Map<Object,Object>>() {}); //

    JsonTypeReference(int id,  TypeReference type) {
        this.id = id;
        this.type = type;
    }

    private final int id;

    private final TypeReference type;

    public int getId() {
        return id;
    }

    public TypeReference getType() {
        return type;
    }

}
