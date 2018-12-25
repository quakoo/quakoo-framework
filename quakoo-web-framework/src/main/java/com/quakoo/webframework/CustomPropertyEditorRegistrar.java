package com.quakoo.webframework;

import com.quakoo.baseFramework.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.util.List;

public class CustomPropertyEditorRegistrar extends WebMvcConfigurerAdapter implements PropertyEditorRegistrar {
    Logger logger = LoggerFactory.getLogger(CustomPropertyEditorRegistrar.class);

    private List<String> jsonClasses;

    public List<String> getJsonClasses() {
        return jsonClasses;
    }

    public void setJsonClasses(List<String> jsonClasses) {
        this.jsonClasses = jsonClasses;
    }


    public void addFormatters(FormatterRegistry registry) {
        if (jsonClasses != null && jsonClasses.size() > 0) {
            for (String jsonClass : jsonClasses) {
                try {
                    final Class requiredType = ClassUtils.forName(jsonClass, ClassUtils.getDefaultClassLoader());

                    //(Class<?> sourceType, Class<?> targetType, Converter<?, ?> converter);
                    registry.addConverter(String.class, requiredType, new Converter<String, Object>() {

                        @Override
                        public Object convert(String source) {
                            try {
                                return JsonUtils.parse(source, requiredType);
                            } catch (IOException e) {
                                throw new RuntimeException("json parse error:" + source, e);
                            }
                        }
                    });
                } catch (ClassNotFoundException e) {
                    logger.error("class not found", e);
                    throw new FatalBeanException("class not found" + jsonClass, e);
                }
            }
        }
    }

    public void registerCustomEditors(PropertyEditorRegistry registry) {
        // it is expected that new PropertyEditor instances are created
        if (jsonClasses != null && jsonClasses.size() > 0) {
            for (String jsonClass : jsonClasses) {
                try {
                    Class requiredType = ClassUtils.forName(jsonClass, ClassUtils.getDefaultClassLoader());
                    System.out.println(registry.getClass());
                    System.out.println(registry);
                    registry.registerCustomEditor(requiredType, new JsonPropertyEditor(requiredType));
                } catch (ClassNotFoundException e) {
                    logger.error("class not found", e);
                    throw new FatalBeanException("class not found" + jsonClass, e);
                }
            }
        }
        // you could register as many custom property editors as are required here...
    }
}