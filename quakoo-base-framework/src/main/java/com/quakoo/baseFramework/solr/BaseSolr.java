//package com.quakoo.baseFramework.solr;
//
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.impl.HttpSolrServer;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///*
// * kongdepeng
// * 2015年7月28日 上午10:02:00
// */
//@SuppressWarnings("deprecation")
//public class BaseSolr {
//
//	private HttpSolrServer server;
//
//	private static final Logger logger = LoggerFactory.getLogger(BaseSolr.class);
//
//	public BaseSolr(String httpSolr, String coreName) {
//		try {
//			server = new HttpSolrServer(httpSolr+coreName);
//			server.setConnectionTimeout(2000);
//			server.setDefaultMaxConnectionsPerHost(1000);
//			server.setMaxTotalConnections(1000);
//        } catch (Exception e) {
//        	logger.error(e.getMessage(), e);
//        }
//	}
//
//	public BaseSolr(String httpSolr, String coreName, String basicAuthUser, String basicAuthPass) {
//		try {
//			String prefix = "http://";
//			String suffix = StringUtils.substringAfter(httpSolr, prefix);
//			server = new HttpSolrServer(prefix + basicAuthUser + ":" + basicAuthPass +
//					"@" + suffix + coreName);
//			server.setConnectionTimeout(2000);
//			server.setDefaultMaxConnectionsPerHost(1000);
//			server.setMaxTotalConnections(1000);
//			server.setSoTimeout(1000);
//        } catch (Exception e) {
//        	logger.error(e.getMessage(), e);
//        }
//	}
//
//	public void delByIds(List<String> ids) {
//		try {
//			server.deleteById(ids);
//			server.commit();
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//	}
//
//	public void delById(String id) {
//		try {
//			server.deleteById(id);
//			server.commit();
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//	}
//
//	public QueryResponse queryTask(Map<String,String> map) {
//		SolrQuery params = new SolrQuery();
//		logger.info(map.toString());
//		for(String key:map.keySet()){
//			params.set(key, map.get(key));
//		}
//		logger.info(params.toString());
//		QueryResponse response = null;
//		try {
//			response = server.query(params);
//		} catch (Exception e) {
//			logger.error(e.getMessage(), e);
//		}
//		return response;
//	}
//
//}
