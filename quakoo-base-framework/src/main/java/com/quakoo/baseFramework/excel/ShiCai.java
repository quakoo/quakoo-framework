package com.quakoo.baseFramework.excel;

public class ShiCai extends ExcelObj{

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 4)
    private String name;

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 5)
    private String unit;

    @ExcelInfo(readType = ExcelReadType.common, readRow = 2, readCol = 6)
    private String weight;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "ShiCai{" +
                "name='" + name + '\'' +
                ", unit='" + unit + '\'' +
                ", weight='" + weight + '\'' +
                ", step=" + step +
                '}';
    }
}
