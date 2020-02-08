package com.quakoo.framework.ext.recommend.context.handle;

import com.quakoo.framework.ext.recommend.AbstractRecommendInfo;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public abstract class BaseContextHandle implements InitializingBean {

    @Resource
    protected AbstractRecommendInfo recommendInfo;

    protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

}
