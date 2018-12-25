package com.quakoo.baseFramework.excel;

public class ExcelObj {

    protected int step;

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "ExcelObj{" +
                "step=" + step +
                '}';
    }
}
