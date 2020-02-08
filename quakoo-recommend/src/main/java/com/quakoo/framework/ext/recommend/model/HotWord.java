package com.quakoo.framework.ext.recommend.model;

import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class HotWord implements Comparable<HotWord>, Serializable  {

    private long id;

    private String word;

    private double weight;

    private int num;

    private long sort; //yyyyMMddHHnum(0-9)

    @Override
    public int compareTo(HotWord o) {
        int res = o.num - this.num;
        if(res == 0) {
            res = o.weight - this.weight > 0 ? 1: -1;
        }
        return res;
    }

    public static void main(String[] args) {
        HotWord a = new HotWord();
        a.setId(1);
        a.setWord("a");
        a.setNum(1);
        a.setWeight(0.113);

        HotWord b = new HotWord();
        b.setId(2);
        b.setWord("b");
        b.setNum(1);
        b.setWeight(0.113);

        List<HotWord> list = Lists.newArrayList();
        list.add(b);
        list.add(a);

        Collections.sort(list);

        System.out.println(list.toString());
    }

}
