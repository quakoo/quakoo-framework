package com.quakoo.webframework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.WebRequest;

public class MyWebBindingInitializer implements WebBindingInitializer {

    @Autowired
    private ConversionService conversionService;

    @Override
    public void initBinder(WebDataBinder binder) {
        binder.setConversionService(conversionService);
    }

}