package com.quakoo.baseFramework.excel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.reflect.ReflectUtil;
import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ExcelReadUtil {

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            return cell.getCellFormula();
        } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            double value = cell.getNumericCellValue();
            if ((double)((int)value) == value) {
                return String.valueOf((int)value);
            } else {
                if (String.valueOf(value).contains("E")) {
                    BigDecimal bigDecimal = new BigDecimal(String.valueOf(value));
                    return bigDecimal.toPlainString();
                }
                return String.valueOf(value);
            }

        } else {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            return cell.getStringCellValue();
        }
    }

    private static Map<String, PictureData> getSheetPictrues03(int sheetNum, HSSFSheet sheet, HSSFWorkbook workbook) {
        Map<String, PictureData> sheetIndexPicMap = Maps.newHashMap();
        List<HSSFPictureData> pictures = workbook.getAllPictures();
        if (pictures.size() != 0) {
            for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren()) {
                HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
                if (shape instanceof HSSFPicture) {
                    HSSFPicture pic = (HSSFPicture) shape;
                    int pictureIndex = pic.getPictureIndex() - 1;
                    HSSFPictureData picData = pictures.get(pictureIndex);
                    String picIndex = String.valueOf(sheetNum) + "_"
                            + String.valueOf(anchor.getRow1()) + "_"
                            + String.valueOf(anchor.getCol1());
                    sheetIndexPicMap.put(picIndex, picData);
                }
            }
        }
        return sheetIndexPicMap;
    }

    private static Map<String, PictureData> getSheetPictrues07(int sheetNum, XSSFSheet sheet, XSSFWorkbook workbook) {
        Map<String, PictureData> sheetIndexPicMap = Maps.newHashMap();
        for (POIXMLDocumentPart dr : sheet.getRelations()) {
            if (dr instanceof XSSFDrawing) {
                XSSFDrawing drawing = (XSSFDrawing) dr;
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    XSSFPicture pic = (XSSFPicture) shape;
                    XSSFClientAnchor anchor = pic.getPreferredSize();
                    CTMarker ctMarker = anchor.getFrom();
                    String picIndex = String.valueOf(sheetNum) + "_"
                            + ctMarker.getRow() + "_" + ctMarker.getCol();
                    sheetIndexPicMap.put(picIndex, pic.getPictureData());
                }
            }
        }
        return sheetIndexPicMap;
    }

    private static WorkbookInfo getWeebWork(File file) throws IOException {
        WorkbookInfo res = null;
        if(null != file && file.exists()) {
            String filename = file.getName();
            int excelType = 0;
            String fileType=filename.substring(filename.lastIndexOf("."),filename.length());
            if(".xls".equals(fileType.trim().toLowerCase())) excelType = WorkbookInfo.type_03;
            else if(".xlsx".equals(fileType.trim().toLowerCase())) excelType = WorkbookInfo.type_07;
            if(excelType > 0) {
                Workbook workbook = null;
                FileInputStream fileStream = new FileInputStream(file);
                if(excelType == WorkbookInfo.type_03) workbook = new HSSFWorkbook(fileStream);
                else workbook = new XSSFWorkbook(fileStream);//创建 Excel 2007 工作簿对象
                Map<String, PictureData> allPictureDataMap = Maps.newHashMap();
                int sheetNum = workbook.getNumberOfSheets();
                for(int i = 0; i < sheetNum; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    Map<String, PictureData> pictureDataMap = Maps.newHashMap();
                    if(excelType == WorkbookInfo.type_03) pictureDataMap = getSheetPictrues03(i, (HSSFSheet) sheet, (HSSFWorkbook) workbook);
                    else pictureDataMap = getSheetPictrues07(i, (XSSFSheet) sheet, (XSSFWorkbook) workbook);
                    allPictureDataMap.putAll(pictureDataMap);
                }
                res = new WorkbookInfo();
                res.setWorkbook(workbook);
                res.setAllPictureDataMap(allPictureDataMap);
            }
        }
        return res;
    }


    private static int getStep(Object object, PropertyDescriptor[] propertyDescriptors) throws Exception {
        int step = 0;
        for(PropertyDescriptor one : propertyDescriptors) {
            String fieldName = one.getName();
            if ("step".equals(fieldName)) {
                step = (Integer) one.getReadMethod().invoke(object);
            }
        }
        return step;
    }

    private static String readCell(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if(null != row) {
            Cell cell = row.getCell(colIdx);
            if(null != cell) return getCellValue(cell);
        }
        return null;
    }

    private static void handle(Map<String, PictureData> allPictureDataMap, int sheetIdx,
                               Sheet sheet, ExcelObj obj) throws Exception {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
            int step = getStep(obj, propertyDescriptors);
            for(PropertyDescriptor one : propertyDescriptors) {
                String fieldName = one.getName();
                if (!"class".equals(fieldName) && !"step".equals(fieldName)) {
                    Field field = ReflectUtil.getFieldByName(fieldName, obj.getClass());
                    ExcelInfo excelInfo = field.getAnnotation(ExcelInfo.class);
                    if(null != excelInfo) {
                        ExcelReadType readType = excelInfo.readType();
                        if(readType == ExcelReadType.common) {
                            String value = readCell(sheet, step + excelInfo.readRow(), excelInfo.readCol());
                            if(null != value) one.getWriteMethod().invoke(obj, value);
                        } else {
                            String picKey = String.valueOf(sheetIdx) + "_"
                                    + (step + excelInfo.readRow()) + "_" + excelInfo.readCol();
                            PictureData pictureData = allPictureDataMap.get(picKey);
                            one.getWriteMethod().invoke(obj, pictureData);
                        }
                        continue;
                    }
                    ExcelList excelList = field.getAnnotation(ExcelList.class);
                    if(null != excelList) {
                        int size = excelList.size();
                        Class<ExcelObj> subClass = excelList.clazz();
                        int oneStep = excelList.step();
                        List<ExcelObj> excelObjs = Lists.newArrayList();
                        for(int i = 0; i < size; i++) {
                            ExcelObj subObj = subClass.newInstance();
                            subObj.setStep(i * oneStep + step);
                            excelObjs.add(subObj);
                        }
                        one.getWriteMethod().invoke(obj, excelObjs);

                        for(ExcelObj excelObj : excelObjs) {
                            handle(allPictureDataMap, sheetIdx, sheet, excelObj);
                        }
                    }
                }
            }
    }

    public static void main(String[] args)throws Exception {

    }

    public static <T extends ExcelObj> List<T> readExcel(File file, Class<T> clazz) throws Exception {
        List<T> res = Lists.newArrayList();
        WorkbookInfo workbookInfo = getWeebWork(file);
        Map<String, PictureData> allPictureDataMap = workbookInfo.getAllPictureDataMap();
        int sheetNum = workbookInfo.getWorkbook().getNumberOfSheets();
        for(int i = 0; i < sheetNum; i++) {
            Sheet sheet = workbookInfo.getWorkbook().getSheetAt(i);
            ExcelMulti excelMulti = clazz.getAnnotation(ExcelMulti.class);
            if(null == excelMulti) {
                T obj = clazz.newInstance();
                handle(allPictureDataMap, i, sheet, obj);
                res.add(obj);
            } else {
                int allRowNum = sheet.getLastRowNum() + 1;
                int head = excelMulti.head();
                allRowNum = allRowNum - head;
                int size = allRowNum / excelMulti.step();
                List<T> excelObjs = Lists.newArrayList();
                for(int j = 0; j < size; j++) {
                    T oneObj = clazz.newInstance();
                    oneObj.setStep(j * excelMulti.step());
                    excelObjs.add(oneObj);
                }
                for(T excelObj : excelObjs) {
                    handle(allPictureDataMap, i, sheet, excelObj);
                    res.add(excelObj);
                }
            }
        }
        return res;
    }


}
