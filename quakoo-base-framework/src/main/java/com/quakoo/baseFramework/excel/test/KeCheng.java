package com.quakoo.baseFramework.excel.test;

import com.quakoo.baseFramework.excel.*;
import com.quakoo.baseFramework.jackson.JsonUtils;

import java.io.File;
import java.util.List;

@ExcelMulti(step = 12)
public class KeCheng extends ExcelObj {

    @ExcelInfo(readType = ExcelReadType.common, readRow = 0, readCol = 1)
    private String xueQi;

    @ExcelList(clazz = Jie.class, size = 10, step = 1)
    private List<Jie> jies;

    public String getXueQi() {
        return xueQi;
    }

    public void setXueQi(String xueQi) {
        this.xueQi = xueQi;
    }

    public List<Jie> getJies() {
        return jies;
    }

    public void setJies(List<Jie> jies) {
        this.jies = jies;
    }

    @Override
    public String toString() {
        return "KeCheng{" +
                "xueQi='" + xueQi + '\'' +
                ", jies=" + jies +
                ", step=" + step +
                '}';
    }

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/lihao/Desktop/kecheng1.xlsx");
        List<KeCheng> keChengs = ExcelReadUtil.readExcel(file, KeCheng.class);
        for(KeCheng keCheng : keChengs) {
            System.out.println(JsonUtils.toJson(keCheng));
        }
    }

}
