package com.quakoo.space.aop;

import com.quakoo.space.annotation.dao.HyperspaceDao;
import com.quakoo.space.annotation.timeline.HyperspaceTimeline;
import com.quakoo.space.enums.HyperspaceType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class HyperspaceBeanPostProcessor implements BeanPostProcessor {

	private HyperspaceProxy hyProxy = new HyperspaceProxy();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		HyperspaceDao dao = bean.getClass().getAnnotation(HyperspaceDao.class);
		HyperspaceTimeline timeline= bean.getClass().getAnnotation(HyperspaceTimeline.class);
		if (timeline != null) {
			System.out.println("======================timeline: " + beanName);
			Object newBean = hyProxy.createTimelineProxy(bean.getClass());
			if (null != newBean) {
				try {
					hyProxy.copy(newBean, bean);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return newBean;
			} else {
				return bean;
			}
		}
		if (null != dao) {
			Object newBean = null;
			if (dao.type() == HyperspaceType.jdbc) {
				System.out.println("=======================jdbc: " + beanName);
				newBean = hyProxy.createJdbcProxy(bean.getClass());
			}
			if (dao.type() == HyperspaceType.cache) {
				System.out.println("======================cache: " + beanName);
				newBean = hyProxy.createCacheProxy(bean.getClass());
			}
			if(dao.type() == HyperspaceType.index) {
                System.out.println("======================index: " + beanName);
                newBean = hyProxy.createIndexProxy(bean.getClass());
            }
			if (null != newBean) {
				try {
					hyProxy.copy(newBean, bean);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return newBean;
			} else {
				return bean;
			}
		}

		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {

		return bean;
	}

}
