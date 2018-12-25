package com.quakoo.baseFramework.excel;



import java.io.File;
import java.util.List;

public class Template extends ExcelObj{

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 1)
    private String tianQi;

    @ExcelList(clazz = Can.class, size = 5, step = 16)
    private List<Can> cans;

    public String getTianQi() {
        return tianQi;
    }

    public void setTianQi(String tianQi) {
        this.tianQi = tianQi;
    }

    public List<Can> getCans() {
        return cans;
    }

    public void setCans(List<Can> cans) {
        this.cans = cans;
    }

    @Override
    public String toString() {
        return "Template{" +
                "tianQi='" + tianQi + '\'' +
                ", cans=" + cans +
                ", step=" + step +
                '}';
    }

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/lihao/Desktop/food.xlsx");
        List<Template> templates = ExcelReadUtil.readExcel(file, Template.class);
        for(Template template : templates) {
            System.out.println(template.toString());
        }
    }
}
