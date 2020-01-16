package com.quakoo.framework.ext.recommend.bean;


import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PortraitWord implements Comparable<PortraitWord>, Serializable {

    private String word;

    private double weight;

    @Override
    public int compareTo(PortraitWord o) {
        int res = o.weight - this.weight > 0 ? 1: -1;
        return res;
    }

    public static void main(String[] args) {
        List<PortraitWord> list = Lists.newArrayList();
        for(int i = 0; i < 1000; i ++) {
            PortraitWord portraitWord = new PortraitWord();
            portraitWord.setWord(String.valueOf(i));
            portraitWord.setWeight(0.01);
            list.add(portraitWord);
        }
        Collections.sort(list);
        for(PortraitWord one : list) {
            System.out.println(one.toString());
        }
    }

}
