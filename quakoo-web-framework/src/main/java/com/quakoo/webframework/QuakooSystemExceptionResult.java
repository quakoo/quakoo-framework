package com.quakoo.webframework;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuakooSystemExceptionResult implements Serializable {

    private boolean success = false;

    private  String msg;

    public QuakooSystemExceptionResult(String msg) {
        this.msg = msg;
    }
}
