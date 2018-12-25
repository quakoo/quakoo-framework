package com.quakoo.baseFramework.excel;

import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

public class WorkbookInfo {

    public static final int type_03 = 1;
    public static final int type_07 = 2;

    private Workbook workbook;

    private Map<String, PictureData> allPictureDataMap;

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    public Map<String, PictureData> getAllPictureDataMap() {
        return allPictureDataMap;
    }

    public void setAllPictureDataMap(Map<String, PictureData> allPictureDataMap) {
        this.allPictureDataMap = allPictureDataMap;
    }
}
