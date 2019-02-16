package com.quakoo.space;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.quakoo.space.model.FieldInfo;
import com.quakoo.space.model.lbs.Coordinate;
import com.quakoo.space.model.lbs.Place;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.quakoo.baseFramework.reflect.ReflectUtil;

public class LbsMongodbBaseDao<T extends Place> implements InitializingBean {
	
	private static final int earth_radius= 6378137;
	
	@Autowired
    @Qualifier("mongoTemplate")
    protected MongoTemplate mongoTemplate;
	
	private final static String INDEX_INFO_NAME = "coordinate";
    /**
     * 实体类型
     */
    protected Class<T> entityClass;
    
    protected PropertyDescriptor[] propertyDescriptors;
    
    protected List<FieldInfo> fields = new ArrayList<FieldInfo>(); // 所有的列信息
    
    protected DBCollection collection;
    

	@Override
	public void afterPropertiesSet() throws Exception {
		this.init_entityClass();
		this.init_fields();
		this.init_collection();
		this.init_lbs_index();
	}
	
	
	public T insert(T model) throws Exception {
		this.mongoTemplate.insert(model);
		String id = model.getId();
		Query query = new Query();  
        query.addCriteria(new Criteria("_id").is(id));  
        model = this.mongoTemplate.findOne(query, entityClass);
        return model;
	}
	
	protected boolean update_field(Query query,Update update) {
		WriteResult res = this.mongoTemplate.updateFirst(query, update,entityClass);
		return res.isUpdateOfExisting();
	}
	
	public boolean update(T model) throws Exception {
		String id = model.getId();
		Query query = new Query();  
        query.addCriteria(new Criteria("_id").is(id));  
        DBObject obj = to_updae_DBObject(model);
        Update update = new Update();
        for (String key : obj.keySet()) {
            Object value = obj.get(key);
            update.set(key, value);
        }
        this.mongoTemplate.updateFirst(query, update, entityClass);
        return true;
    }
	
	public T load(String id) throws Exception{
		Query query = new Query();  
        query.addCriteria(new Criteria("_id").is(id));
        T model = this.mongoTemplate.findOne(query, entityClass);
        return model;
	}
	
	public boolean delete(String id) throws Exception {
		Query query = new Query();  
        query.addCriteria(new Criteria("_id").is(id));  
        T model = this.mongoTemplate.findOne(query, entityClass);
        if (model != null) {
            this.mongoTemplate.remove(query, entityClass);
            return true;
        }
        return false;
	}

	public List<T> around(Coordinate coordinate, double dist, int num , Query query) {
		List<T> res = new ArrayList<T>();
		Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
	    NearQuery nearQuery = NearQuery.near(point).spherical(true).num(num)
	    		.maxDistance(new Distance(dist/earth_radius));
	    if (query != null) nearQuery.query(query);
	    GeoResults<T> geoResults = mongoTemplate.geoNear(nearQuery, entityClass);
	    List<GeoResult<T>> list = geoResults.getContent();
	    for (GeoResult<T> result : list) {
	    	T obj = result.getContent();
            obj.setDistance(result.getDistance().getValue()*earth_radius);
            res.add(obj);
        }
	    return res;
	}
	
	public List<T> near(Coordinate coordinate, double dist, int num ,Query query) {
		List<T> res = new ArrayList<T>();
		Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
	    NearQuery nearQuery = NearQuery.near(point).spherical(true).num(num)
	    		.minDistance(new Distance(dist/earth_radius));
	    if (query != null) nearQuery.query(query);
	    GeoResults<T> geoResults = mongoTemplate.geoNear(nearQuery, entityClass);
	    List<GeoResult<T>> list = geoResults.getContent();
	    for (GeoResult<T> result : list) {
	    	T obj = result.getContent();
            obj.setDistance(result.getDistance().getValue()*earth_radius);
            res.add(obj);
        }
	    return res;
	}
	
	public List<T> box(Coordinate leftLowCoordinate, Coordinate rightUpperCoordinate, Query query){
		Box box = new Box(new Point(leftLowCoordinate.getLongitude(), leftLowCoordinate.getLatitude()), 
				new Point(rightUpperCoordinate.getLongitude(), rightUpperCoordinate.getLatitude()));
		 if (query != null) {
	            query.addCriteria(Criteria.where(INDEX_INFO_NAME).within(box));
	            return this.mongoTemplate.find(query, entityClass);
	        } else {
	            return this.mongoTemplate.find(new Query(Criteria.where(INDEX_INFO_NAME).within(box)), entityClass);
	        }
	}
	
	/**
	 * 初始化泛型的domain类
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void init_entityClass() throws Exception {
		entityClass = ReflectUtil.getGenericType(this.getClass(), 0);
		if (entityClass == null) {
			throw new IllegalClassException("EntityClass is error");
		}
		propertyDescriptors = Introspector.getBeanInfo(entityClass)
				.getPropertyDescriptors();
	}
	
	
	private void init_fields() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					String name = one.getName();
					String dbName = name;
					Method writeMethod = one.getWriteMethod();
					Method readMethod = one.getReadMethod();
					FieldInfo fieldInfo = new FieldInfo(field, name, dbName,
							writeMethod, readMethod,false);
					fields.add(fieldInfo);
				}
			}
		}
	}
	
	protected DBObject to_updae_DBObject(T model) throws Exception {
        DBObject obj = new BasicDBObject();
        for (FieldInfo info : this.fields) {
            Method method = info.getReadMethod();
            Id id = info.getField().getAnnotation(Id.class);
            Transient trans = info.getField().getAnnotation(Transient.class);
            if(null != id || null != trans) continue;
            obj.put(info.getDbName(), method.invoke(model));
        }
        return obj;
    }
	
	private void init_collection(){
		if (!this.mongoTemplate.collectionExists(entityClass)) {
			collection = this.mongoTemplate.createCollection(entityClass);
		}else{
			String collectionName = this.mongoTemplate.getCollectionName(entityClass);
			collection = this.mongoTemplate.getCollection(collectionName);
		}
	}
	
	private void init_lbs_index(){
		boolean hasIndex = false;
		List<DBObject> objs = collection.getIndexInfo();
		for(DBObject obj : objs){
			String indexName = obj.get("name") != null ? (String) obj.get("name") : "";
			if (StringUtils.isNotEmpty(indexName) && indexName.equals(INDEX_INFO_NAME+"_2dsphere")) {
                hasIndex = true;
                break;
            }
		}
		if (!hasIndex) {
            DBObject keys = new BasicDBObject();
            keys.put(INDEX_INFO_NAME, "2dsphere");
            collection.createIndex(keys);
        }
	}
	
}
