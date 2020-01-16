package com.quakoo.framework.ext.recommend.bean;

import lombok.Data;

@Data
public class Keyword implements Comparable<Keyword> {

    private double tfidfWeight;
    private String word;

    public Keyword(String word,double tfidfWeight) {
        this.word = word;
        // tfidf值只保留3位小数
        this.tfidfWeight=(double)Math.round(tfidfWeight*10000)/10000;
    }

    @Override
    public int compareTo(Keyword o) {
        return this.tfidfWeight - o.tfidfWeight > 0 ? -1 : 1;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((word == null) ? 0 : word.hashCode());
        long temp;
        temp = Double.doubleToLongBits(tfidfWeight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Keyword other = (Keyword) obj;
        if (word == null) {
            if (other.word != null)
                return false;
        }
        else if (!word.equals(other.word))
            return false;
        return true;
    }

}
