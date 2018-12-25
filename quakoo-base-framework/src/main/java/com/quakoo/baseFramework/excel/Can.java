package com.quakoo.baseFramework.excel;

import java.util.List;

public class Can extends ExcelObj{

    @ExcelInfo(readRow = 2, readCol = 2, readType = ExcelReadType.common)
    private String type;

    @ExcelList(clazz = CaiPin.class, step = 4, size = 4)
    private List<CaiPin> caiPins;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<CaiPin> getCaiPins() {
        return caiPins;
    }

    public void setCaiPins(List<CaiPin> caiPins) {
        this.caiPins = caiPins;
    }

    @Override
    public String toString() {
        return "Can{" +
                "type='" + type + '\'' +
                ", caiPins=" + caiPins +
                ", step=" + step +
                '}';
    }
}
