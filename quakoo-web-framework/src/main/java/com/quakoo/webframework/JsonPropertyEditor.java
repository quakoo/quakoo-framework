package com.quakoo.webframework;

import com.quakoo.baseFramework.exception.BaseBusinessException;
import com.quakoo.baseFramework.json.JsonUtils;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * Created by 136249 on 2015/2/13.
 */
public class JsonPropertyEditor extends PropertyEditorSupport {

    private Class targetClass;

    public JsonPropertyEditor(Class clazz) {
        this.targetClass = clazz;
    }

    @Override
    public void setAsText(String text) {
        if (!StringUtils.isEmpty(text)) {
            try {
                setValue(JsonUtils.parse(text, targetClass));
            } catch (IOException e) {
                throw new BaseBusinessException("param is error", e);
            }
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        if (getValue() == null) {
            return "";
        }
        try {
            return JsonUtils.format(getValue());
        } catch (IOException e) {
            throw new BaseBusinessException("param is error", e);
        }
    }

}
