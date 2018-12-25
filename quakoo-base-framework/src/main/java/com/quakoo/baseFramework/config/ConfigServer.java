package com.quakoo.baseFramework.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quakoo.baseFramework.ip.LocalIps;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakoo.baseFramework.exception.ServerInitError;
import com.quakoo.baseFramework.http.MultiHttpPool;
import com.quakoo.baseFramework.json.JsonUtils;

/**
 * zk地址： projectName+rootPath+属性的注解path
 * 
 * zk存入的数据： json: zkResult.
 * 
 * 参考 TestConfig
 * 
 * @author LiYongbiao
 *
 */
public class ConfigServer {
	// static String
	// defaultCallBackUrl="http://10.0.3.153:10010/config/callback.go";
	static String defaultCallBackUrl = null;
	static MultiHttpPool pool = MultiHttpPool.getMultiHttpPool();
	Logger logger = LoggerFactory.getLogger(ConfigServer.class);

	Set<SingleProperty> singleProperties = new CopyOnWriteArraySet<SingleProperty>();

	private final String zkAddress;
	private final String rootPath;
	private final String projectName;
	private final CuratorFramework client;

	public ConfigServer(String zkAddress, String rootPath, String projectName) {
		this.zkAddress = zkAddress;
		if (!rootPath.startsWith("/")) {
			rootPath = "/" + rootPath;
		}
		if (rootPath.endsWith("/")) {
			rootPath = rootPath.substring(0, rootPath.length() - 1);
		}
		this.rootPath = rootPath;
		this.projectName = projectName;

		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
				.builder();

		String key = zkAddress + rootPath + projectName;
		CuratorFramework client = builder
				.connectString(zkAddress)
				.sessionTimeoutMs(10000)
				.connectionTimeoutMs(5000)
				.canBeReadOnly(false)
				.retryPolicy(
						new ExponentialBackoffRetry(1000, Integer.MAX_VALUE))
				.namespace(projectName).defaultData(null).build();
		client.start();
		try {
			PathChildrenCache cached = pathChildrenCache(client, rootPath, true);
			cached.start(StartMode.BUILD_INITIAL_CACHE);
		} catch (Exception e) {
			throw new ServerInitError(e);
		}

		this.client = client;

	}

	private PathChildrenCache pathChildrenCache(CuratorFramework client,
			String path, Boolean cacheData) throws Exception {
		final PathChildrenCache cached = new PathChildrenCache(client, path,
				cacheData);
		cached.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client,
					PathChildrenCacheEvent event) throws Exception {
				PathChildrenCacheEvent.Type eventType = event.getType();
				switch (eventType) {
				case CHILD_ADDED: {
					changeValue(ZKPaths.getNodeFromPath(event.getData()
							.getPath()), event.getData().getData() == null ? ""
							: new String(event.getData().getData(), "utf-8"));
					break;
				}
				case CHILD_UPDATED: {
					changeValue(ZKPaths.getNodeFromPath(event.getData()
							.getPath()), event.getData().getData() == null ? ""
							: new String(event.getData().getData(), "utf-8"));
					break;
				}
				case CHILD_REMOVED: {
					changeValue(ZKPaths.getNodeFromPath(event.getData()
							.getPath()), event.getData().getData() == null ? ""
							: new String(event.getData().getData(), "utf-8"));
					break;
				}
				default:
					break;
				}
			}
		});
		return cached;
	}

	/**
	 * 初始化静态类
	 * 
	 * @param clazz
	 */
	public void initStaticClass(Class clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			ConfigProperty configProperty = field
					.getAnnotation(ConfigProperty.class);
			if (configProperty == null) {
				continue;
			}
			if (!Modifier.isStatic(field.getModifiers())) {
				throw new ServerInitError("field is not static field:+"
						+ field.getName() + ".class:" + clazz.getName());
			}
			if (configProperty.path().contains("/")) {
				throw new ServerInitError("path contains /");
			}
			SingleProperty singleProperty = new SingleProperty(
					configProperty.path(), configProperty.json(), field, true,
					clazz, null, configProperty.typeReference());
			initSingleProperty(singleProperty);
			singleProperties.add(singleProperty);
		}
	}

	/**
	 * 初始化Object。实现接口ReloadInstance可以在属性发生变化的时候调用reload方法。
	 * 
	 * @param obj
	 * @throws Exception
	 */
	public void initObject(Object obj) throws Exception {
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields) {
			ConfigProperty configProperty = field
					.getAnnotation(ConfigProperty.class);
			if (configProperty == null) {
				continue;
			}
			SingleProperty singleProperty = new SingleProperty(
					configProperty.path(), configProperty.json(), field,
					Modifier.isStatic(field.getModifiers()), null, obj,
					configProperty.typeReference());
			initSingleProperty(singleProperty);
			singleProperties.add(singleProperty);
		}
		if (obj instanceof ReloadInstance)
			((ReloadInstance) obj).init();

	}

	private void initSingleProperty(SingleProperty singleProperty) {
		String path = null;
		try {
			path = rootPath + "/" + singleProperty.getPath();
			byte[] data = client.getData().forPath(path);
			if (data != null && data.length != 0) {
				String result = new String(data, "utf-8");
				ZkResult zkResult = JsonUtils.objectMapper.readValue(result,
						ZkResult.class);
				setValue(zkResult.getResult(), singleProperty);
				logger.info("init property,path:{},result:{}", path,
						zkResult.getResult());
			} else {
				setValue("", singleProperty);
				logger.info("init property,path:{},result:{}", path, "");
			}
		} catch (Exception e) {
			logger.error("initSingleProperty error,path:" + path, e);
			throw new ServerInitError("配置加载错误，property init error path:"
					+ singleProperty.getPath(), e);
		}
	}

	private void changeValue(String path, String result) {
		logger.info("get change event path:{},result:{},singleProperties:{}",
				new Object[] { path, result, singleProperties });
		String callBackUrl = null;
		try {
			for (SingleProperty singleProperty : singleProperties) {
				if (singleProperty.getPath().equals(path)) {
					if (StringUtils.isNotBlank(result)) {
						ZkResult zkResult = JsonUtils.objectMapper.readValue(
								result, ZkResult.class);
						callBackUrl = zkResult.getCallbackUrl();
						if (isAllow(zkResult.getNotAllowIps())) {
							boolean change = setValue(zkResult.getResult(),
									singleProperty);
							logger.info("change property,path:{},result:{}",
									path, zkResult.getResult());
							if (change
									&& singleProperty.getObj() != null
									&& (singleProperty.getObj() instanceof ReloadInstance)) {
								((ReloadInstance) singleProperty.getObj())
										.reloadOnPropertyChange();
								logger.info("reload object:{}",
										singleProperty.getObj());
							}
						}
					} else {
						boolean change = setValue("", singleProperty);
						logger.info("change property,path:{},result:{}", path,
								"");
						if (change
								&& singleProperty.getObj() != null
								&& (singleProperty.getObj() instanceof ReloadInstance)) {
							((ReloadInstance) singleProperty.getObj())
									.reloadOnPropertyChange();
							logger.info("reload object:{}",
									singleProperty.getObj());
						}
					}
				}
			}
			callback(callBackUrl, "success", path, result);
		} catch (Exception e) {
			logger.error("changeValue error", e);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				e.printStackTrace(new PrintStream(baos));
			} finally {
				try {
					baos.close();
				} catch (IOException e1) {
				}
			}
			String content = baos.toString();
			callback(callBackUrl, content, path, result);
		}
	}

	private void callback(String callBackUrl, String result, String path,
			String value) {
		if (callBackUrl == null || !callBackUrl.startsWith("http")) {
			callBackUrl = defaultCallBackUrl;
		}

		if (callBackUrl != null && callBackUrl.startsWith("http")) {
			Map<String, Object> map = new HashMap<String, Object>();
			String ip = LocalIps.getIp("10.0");
			map.put("result", ip + ":" + result);
			map.put("path", path);
			map.put("value", value);
			try {
				pool.httpQuery(callBackUrl, null, "post", null, map, null,
						false, true, true);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("changeValue callback error", e);
			}
		}
	}

	private boolean isAllow(List<String> notAllowIps) {
		if (notAllowIps == null || notAllowIps.size() == 0) {
			return true;
		}
		List<String> ips = LocalIps.getAllIpds();
		for (String ip : ips) {
			if (notAllowIps.contains(ip)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("rawtypes")
	private boolean setValue(String strValue, SingleProperty singleProperty)
			throws Exception {
		Object result = null;
		if (StringUtils.isNotBlank(strValue)) {
			if (singleProperty.isJson()) {
				if (singleProperty.getTypeReference() != DefaultTypeReferenceModel.class) {
					TypeReference typeReference = ((TypeReferenceModel) singleProperty
							.getTypeReference().newInstance()).get();
					result = JsonUtils.objectMapper.readValue(strValue,
							typeReference);
				} else
					result = JsonUtils.objectMapper.readValue(strValue,
							singleProperty.getField().getType());
			} else {
				Class type = singleProperty.getField().getType();
				if (type == Integer.class || type == int.class) {
					result = Integer.parseInt(strValue);
				} else if (type == String.class) {
					result = strValue;
				} else if (type == Long.class || type == long.class) {
					result = Long.parseLong(strValue);
				} else if (type == Double.class || type == double.class) {
					result = Double.parseDouble(strValue);
				} else if (type == Boolean.class || type == boolean.class) {
					result = Boolean.parseBoolean(strValue);
				} else if (type == Float.class || type == float.class) {
					result = Float.parseFloat(strValue);
				} else {
					throw new RuntimeException("not supper:" + type);
				}
			}
		}
		singleProperty.getField().setAccessible(true);
		Object oldValue = null;
		if (singleProperty.isIsstatic()) {
			oldValue = singleProperty.getField().get(singleProperty.getClazz());
			singleProperty.getField().set(singleProperty.getClazz(), result);
		} else {
			oldValue = singleProperty.getField().get(singleProperty.getObj());
			singleProperty.getField().set(singleProperty.getObj(), result);

		}

		if (result == null && oldValue == null) {
			return false;
		} else if (result != null && oldValue == null) {
			return true;
		} else if (result == null) {
			return true;
		} else if (result.equals(oldValue)) {
			return false;
		} else {
			return true;
		}

	}

}
