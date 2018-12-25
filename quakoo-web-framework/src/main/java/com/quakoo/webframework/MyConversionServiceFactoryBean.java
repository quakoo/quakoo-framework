package com.quakoo.webframework;

import com.quakoo.baseFramework.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by 136249 on 2015/2/27.
 */
public class MyConversionServiceFactoryBean extends ConversionServiceFactoryBean {

    Logger logger= LoggerFactory.getLogger(MyConversionServiceFactoryBean.class);

    private List<String> jsonClasses;

    public List<String> getJsonClasses() {
        return jsonClasses;
    }

    public void setJsonClasses(List<String> jsonClasses) {
        this.jsonClasses = jsonClasses;
    }

    private GenericConversionService conversionService;

    @Override
    public void afterPropertiesSet() {
        conversionService = createConversionService();

        if (jsonClasses != null && jsonClasses.size() > 0) {
            for (String jsonClass : jsonClasses) {
                try {
                    final Class requiredType = ClassUtils.forName(jsonClass, ClassUtils.getDefaultClassLoader());
                    conversionService.addConverter(String.class, requiredType, new Converter<String, Object>() {
                        @Override
                        public Object convert(String source) {
                            try {
                                return JsonUtils.parse(source, requiredType);
                            } catch (IOException e) {
                                throw new RuntimeException("json parse error:"+source,e);
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

    public ConversionService getObject() {
        return this.conversionService;
    }


}
