package com.quakoo.baseFramework.excel.test;

import com.quakoo.baseFramework.excel.ExcelList;
import com.quakoo.baseFramework.excel.ExcelObj;
import com.quakoo.baseFramework.excel.ExcelReadUtil;
import com.quakoo.baseFramework.jackson.JsonUtils;

import java.io.File;
import java.util.List;

public class KeChengs extends ExcelObj {

    @ExcelList(clazz = KeCheng.class, size = 5, step = 12)
    private List<KeCheng> keChengs;

    public List<KeCheng> getKeChengs() {
        return keChengs;
    }

    public void setKeChengs(List<KeCheng> keChengs) {
        this.keChengs = keChengs;
    }

    public static void main(String[] args)  throws Exception {
        File file = new File("/Users/lihao/Desktop/kecheng1.xlsx");
        List<KeChengs> keChengsList = ExcelReadUtil.readExcel(file, KeChengs.class);
        for(KeChengs keChengs : keChengsList) {
            System.out.println(JsonUtils.toJson(keChengs));
        }
    }

}
