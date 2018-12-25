package com.quakoo.webframework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.WebRequest;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 136249 on 2015/2/13.
 */
public class JsonCustomEditorConfigurer extends CustomEditorConfigurer implements WebBindingInitializer {
    Logger logger = LoggerFactory.getLogger(JsonCustomEditorConfigurer.class);

    private Map<Class<?>, Class<? extends PropertyEditor>> commonCustomEditors;

    private List<String> jsonClasses;

    public List<String> getJsonClasses() {
        return jsonClasses;
    }

    public void setJsonClasses(List<String> jsonClasses) {
        this.jsonClasses = jsonClasses;
    }

    public Map<Class<?>, Class<? extends PropertyEditor>> getCommonCustomEditors() {
        return commonCustomEditors;
    }

    public void setCommonCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> commonCustomEditors) {
        this.commonCustomEditors = commonCustomEditors;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (commonCustomEditors == null) {
            commonCustomEditors = new HashMap<Class<?>, Class<? extends PropertyEditor>>();
        }
        if (jsonClasses != null && jsonClasses.size() > 0) {
            for (String jsonClass : jsonClasses) {
                try {
                    Class<?> requiredType = ClassUtils.forName(jsonClass, ClassUtils.getDefaultClassLoader());
                    commonCustomEditors.put(requiredType, new JsonPropertyEditor(requiredType).getClass());
                } catch (ClassNotFoundException e) {
                    logger.error("class not found", e);
                    throw new FatalBeanException("class not found" + jsonClass, e);
                }
            }
        }
        if (commonCustomEditors.size() > 0) {
            super.setCustomEditors(commonCustomEditors);
        }
        super.postProcessBeanFactory(beanFactory);
    }

    @Override
    public void initBinder(WebDataBinder binder) {
        if (jsonClasses != null && jsonClasses.size() > 0) {
            for (String jsonClass : jsonClasses) {
                try {
                    Class requiredType = ClassUtils.forName(jsonClass, ClassUtils.getDefaultClassLoader());
                    binder.registerCustomEditor(requiredType, new JsonPropertyEditor(requiredType));
                } catch (ClassNotFoundException e) {
                    logger.error("class not found", e);
                    throw new FatalBeanException("class not found" + jsonClass, e);
                }
            }
        }
    }

    public static void main(String[] fwef) throws ClassNotFoundException {
        Config[] fds=new Config[1];
        System.out.println(fds.getClass().getName());
        //String name=fds.getClass().getName();
        String name="[Lcom.systoon.scloud.webframework.Config;";
        Class requiredType = ClassUtils.forName(name, ClassUtils.getDefaultClassLoader());
        System.out.println(requiredType);
    }


}
