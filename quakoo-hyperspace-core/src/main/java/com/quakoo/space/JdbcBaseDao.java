package com.quakoo.space;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.space.helper.BaseJdbcHelper;
import com.quakoo.space.mapper.HyperspaceBeanPropertySqlParameterSource;
import com.quakoo.space.model.FieldInfo;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.quakoo.baseFramework.reflect.ReflectUtil;
import com.quakoo.baseFramework.util.StringUtil;
import com.quakoo.space.annotation.domain.CombinationKey;
import com.quakoo.space.annotation.domain.HyperspaceColumn;
import com.quakoo.space.annotation.domain.HyperspaceDomain;
import com.quakoo.space.annotation.domain.PrimaryKey;
import com.quakoo.space.annotation.domain.ShardingKey;
import com.quakoo.space.annotation.domain.SortKey;
import com.quakoo.space.enums.HyperspaceDomainType;
import com.quakoo.space.enums.IdentityType;

/**
 * 1.从库还未完全开放出去。<br>
 * 对于插入后立即查询的问题，由于插入后立即进入缓存，所以对于我们目前这种项目不会有问题。<br>
 * 如果要求十分严格，考虑在把分布式事务添加进来之后对外暴露从库。
 *
 * @author haoli yongbiaoli
 * @param <T>
 */
public class JdbcBaseDao<T> implements RowMapper<T>,
		InitializingBean {

	Logger logger = LoggerFactory.getLogger(JdbcBaseDao.class);

	/** 数据源们 */
	protected List<DataSource> dataSources;

	/** 从库的数据源们 */
	protected List<DataSource> slaveDataSources;

	/**
	 * 数据源对应的jdbc模板们
	 */
	protected NamedParameterJdbcTemplate[] jdbcTemplates;

	/**
	 * 备用数据源对应的jdbc模板们
	 */
	protected NamedParameterJdbcTemplate[] slaveJdbcTemplates;

	/**
	 * id自增器
	 */
	protected DataFieldMaxValueIncrementer incrementer;

	/**
	 * 表名
	 */
	protected String tableName = "";

	protected Class<?> entityClass;

	protected String daoClassName = this.getClass().getSimpleName() + " ";

	protected PropertyDescriptor[] propertyDescriptors;

	protected List<FieldInfo> fields = new ArrayList<FieldInfo>(); // 所有的列信息

	// List<FieldInfo> update_fields = new ArrayList<FieldInfo>(); // 需要更新的列信息

	protected FieldInfo primaryFieldInfo;

	protected FieldInfo shardingFieldInfo;

	protected FieldInfo sortFieldInfo;

	protected FieldInfo utimeFieldInfo;

	protected FieldInfo ctimeFieldInfo;

	protected List<FieldInfo> combinationFieldInfos = new ArrayList<FieldInfo>();

	protected BaseJdbcHelper jdbcHelper;

	protected HyperspaceDomainType domainType; // 修改的类型

	protected IdentityType identityType; // 自增类型

	protected HyperspaceDomain domain;// domai的注解

	protected boolean isShardingTable;// 是否分库分表

	
	protected boolean hibernateDbName;//驼峰转数据库下划线命名。
	
	public static String[] keywords = new String[] { "ADD", "ALL", "ALTER",
			"ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN",
			"BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE",
			"CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE",
			"COLUMN", "CONDITION", "CONNECTION", "CONSTRAINT", "CONTINUE",
			"CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME",
			"CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE",
			"DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE",
			"DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED",
			"DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT",
			"DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE",
			"ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN",
			"FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE",
			"FOREIGN", "FROM", "FULLTEXT", "GOTO", "GRANT", "GROUP", "HAVING",
			"HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND",
			"IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER", "INOUT",
			"INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4",
			"INT8", "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN",
			"KEY", "KEYS", "KILL", "LABEL", "LEADING", "LEAVE", "LEFT", "LIKE",
			"LIMIT", "LINEAR", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP",
			"LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY",
			"MATCH", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT",
			"MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES",
			"NATURAL", "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON",
			"OPTIMIZE", "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER",
			"OUTFILE", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "RAID0",
			"RANGE", "READ", "READS", "REAL", "REFERENCES", "REGEXP",
			"RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESTRICT",
			"RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS",
			"SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET",
			"SHOW", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION",
			"SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS",
			"SQL_SMALL_RESULT", "SSL", "STARTING", "STRAIGHT_JOIN", "TABLE",
			"TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO",
			"TRAILING", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK",
			"UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE",
			"UTC_TIME", "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR",
			"VARCHARACTER", "VARYING", "WHEN", "WHERE", "WHILE", "WITH",
			"WRITE", "X509", "XOR", "YEAR_MONTH", "ZEROFILL" };

	public List<DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(List<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	public List<DataSource> getSlaveDataSources() {
		return slaveDataSources;
	}

	public void setSlaveDataSources(List<DataSource> slaveDataSources) {
		this.slaveDataSources = slaveDataSources;
	}

	public NamedParameterJdbcTemplate[] getSlaveJdbcTemplates() {
		return slaveJdbcTemplates;
	}

	public void setSlaveJdbcTemplates(NamedParameterJdbcTemplate[] slaveJdbcTemplates) {
		this.slaveJdbcTemplates = slaveJdbcTemplates;
	}

	public String getTableName() {
		return tableName;
	}

	public DataFieldMaxValueIncrementer getIncrementer() {
		return incrementer;
	}

	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public BaseJdbcHelper getJdbcHelper() {
		return jdbcHelper;
	}

	public void setJdbcHelper(BaseJdbcHelper jdbcHelper) {
		this.jdbcHelper = jdbcHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		jdbcTemplates = new NamedParameterJdbcTemplate[dataSources.size()];
		for (int i = 0; i < dataSources.size(); i++) {
			jdbcTemplates[i] = new NamedParameterJdbcTemplate(dataSources.get(i));
		}

		if (slaveDataSources != null && dataSources.size() > 0) {
			if (slaveDataSources.size() != dataSources.size()) {
				throw new IllegalClassException(
						"slaveDataSources.size != dataSources.size");
			}
			slaveJdbcTemplates = new NamedParameterJdbcTemplate[slaveDataSources.size()];
			for (int i = 0; i < slaveDataSources.size(); i++) {
				slaveJdbcTemplates[i] = new NamedParameterJdbcTemplate(
						slaveDataSources.get(i));
			}

		}

		this.init_entityClass();
		logger.info(daoClassName + "entityClass: " + this.entityClass);
		this.init_object();
		logger.info(daoClassName + "domain: " + this.domain);
		this.init_fields();
		logger.info(daoClassName + "fields: " + this.fields);
		this.init_primaryFields();
		logger.info(daoClassName + "primary_field: " + this.primaryFieldInfo);
		this.init_shardingFields();
		logger.info(daoClassName + "sharding_field: " + this.shardingFieldInfo);
		this.init_combinationField();
		logger.info(daoClassName + "combination_field: "
				+ this.combinationFieldInfos);
		this.init_sortField();
		logger.info(daoClassName + "sortField: " + this.sortFieldInfo);

		// this.init_update_fields();
		// System.out.println("update_fields: " + this.update_fields);
		this.default_init_jdbc_helper();
		logger.info(daoClassName + "jdbcHelper: " + this.jdbcHelper);

		this.check();

	}

	/**
	 * 初始化泛型的domain类
	 *
	 * @throws Exception
	 */
	private void init_entityClass() throws Exception {
		entityClass = ReflectUtil.getGenericType(this.getClass(), 0);
		if (entityClass == null) {
			throw new IllegalClassException("EntityClass is error");
		}
		propertyDescriptors = Introspector.getBeanInfo(entityClass)
				.getPropertyDescriptors();
	}

	/**
	 * 初始化domain类的 类注解信息
	 *
	 * @throws Exception
	 */
	private void init_object() throws Exception {
		this.domain = entityClass.getAnnotation(HyperspaceDomain.class);
		if (null == domain) {
			throw new IllegalClassException("AutowareInit is null");
		}
		this.domainType = domain.domainType();
		int[] dbSeeds = domain.DbShardingSeeds();
		int[] tableSeeds = domain.tableShardingSeeds();
		this.identityType = domain.identityType();
		this.hibernateDbName=domain.hibernateDbName();
		if (dbSeeds.length == 1 && tableSeeds.length == 1 && tableSeeds[0] == 0
				&& dbSeeds[0] == 0) {
			this.isShardingTable = false;
		} else {
			this.isShardingTable = true;
		}

	}

	// private void init_update_fields() {
	// for (FieldInfo one : fields) {
	// if ((!this.primary_fields.values().contains(one)) &&
	// (!this.sharding_fields.values().contains(one))) {
	// if (null != this.identity && !this.identity.equals(one)) {
	// this.update_fields.add(one);
	// } else if (null == this.identity) {
	// this.update_fields.add(one);
	// }
	// }
	// }
	// }

	/**
	 * 初始化domain类的 各个属性
	 *
	 * @throws Exception
	 */
	private void init_fields() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					HyperspaceColumn autowareMap = field
							.getAnnotation(HyperspaceColumn.class);
					if (autowareMap != null && !autowareMap.isDbColumn()) {
						continue;
					}
					String name = one.getName();
					String dbName = name;
					Method writeMethod = one.getWriteMethod();
					Method readMethod = one.getReadMethod();
					boolean isJson=false;
					if(this.hibernateDbName){
						dbName=StringUtil.camelToUnderline(name);
					}
					
					if (autowareMap != null) {
						if(StringUtils.isNotBlank(autowareMap.column())){
							dbName = autowareMap.column();
						}
						isJson=autowareMap.isJson();
					}
					
					

					if (Arrays.asList(keywords).contains(dbName.toUpperCase())) {

						dbName = "`" + dbName + "`";
					}
					FieldInfo fieldInfo = new FieldInfo(field, name, dbName,
							writeMethod, readMethod,isJson);
					fields.add(fieldInfo);
					if (fieldName.equals("utime")) {
						utimeFieldInfo = fieldInfo;
					}
					if (fieldName.equals("ctime")) {
						ctimeFieldInfo = fieldInfo;
					}
				}
			}
		}
	}

	/**
	 * 初始化domain类的主键
	 *
	 * @throws Exception
	 */
	private void init_primaryFields() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					PrimaryKey primaryKey = field
							.getAnnotation(PrimaryKey.class);
					if (null != primaryKey) {
						String name = one.getName();
						String dbName = name;
						Method writeMethod = one.getWriteMethod();
						Method readMethod = one.getReadMethod();
						FieldInfo tempFieldInfo = new FieldInfo(field, name,
								dbName, writeMethod, readMethod,false);
						for (FieldInfo fieldInfo : fields) {
							if (fieldInfo.equals(tempFieldInfo)) {
								this.primaryFieldInfo = fieldInfo;
								break;
							}
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * 初始化domain类的分表字段
	 *
	 * @throws Exception
	 */
	private void init_shardingFields() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					ShardingKey shardingKey = field
							.getAnnotation(ShardingKey.class);
					if (null != shardingKey) {
						String name = one.getName();
						String dbName = name;
						Method writeMethod = one.getWriteMethod();
						Method readMethod = one.getReadMethod();

						FieldInfo tempFieldInfo = new FieldInfo(field, name,
								dbName, writeMethod, readMethod,false);
						for (FieldInfo fieldInfo : fields) {
							if (fieldInfo.equals(tempFieldInfo)) {
								this.shardingFieldInfo = fieldInfo;
								break;
							}
						}
						break;
					}
				}
			}
		}

		// 如果没有分表字段 默认分表字段为主键字段
		if (this.shardingFieldInfo == null) {
			this.shardingFieldInfo = primaryFieldInfo;
		}

	}

	/**
	 * 初始化domain类的组合键
	 *
	 * @throws Exception
	 */
	private void init_combinationField() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					CombinationKey combinationKey = field
							.getAnnotation(CombinationKey.class);
					if (null != combinationKey) {
						String name = one.getName();
						String dbName = name;
						Method writeMethod = one.getWriteMethod();
						Method readMethod = one.getReadMethod();
						FieldInfo tempFieldInfo = new FieldInfo(field, name,
								dbName, writeMethod, readMethod,false);
						for (FieldInfo fieldInfo : fields) {
							if (fieldInfo.equals(tempFieldInfo)) {
								this.combinationFieldInfos.add(fieldInfo);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 初始化sort属性
	 *
	 * @throws Exception
	 */
	private void init_sortField() throws Exception {
		for (PropertyDescriptor one : propertyDescriptors) {
			String fieldName = one.getName();
			if (!"class".equals(fieldName)) {
				Field field = ReflectUtil
						.getFieldByName(fieldName, entityClass);
				if (field != null) {
					SortKey sortKey = field.getAnnotation(SortKey.class);
					if (null != sortKey) {
						String name = one.getName();
						String dbName = name;
						Method writeMethod = one.getWriteMethod();
						Method readMethod = one.getReadMethod();

						FieldInfo tempFieldInfo = new FieldInfo(field, name,
								dbName, writeMethod, readMethod,false);
						for (FieldInfo fieldInfo : fields) {
							if (fieldInfo.equals(tempFieldInfo)) {
								this.sortFieldInfo = fieldInfo;
								break;
							}
						}
						break;
					}
				}
			}
		}

	}

	/**
	 * 初始化jdbc的部分语句
	 */
	private void default_init_jdbc_helper() {
		String columns = null;
		String insertBeanColumns = null;
		String updateBeanColumns = null;
		StringBuffer columnsBuffer = new StringBuffer(" ");
		StringBuffer insertBeanColumnsBuffer = new StringBuffer(" ");
		StringBuffer updateBeanColumnsBuffer = new StringBuffer(" ");
		for (FieldInfo one : fields) {
			String dbName = one.getDbName();
			String name = one.getName();
			columnsBuffer.append(dbName).append(",");
			insertBeanColumnsBuffer.append(":").append(name).append(",");
			if (!one.equals(primaryFieldInfo)
					&& !combinationFieldInfos.contains(one)
					&& !"ctime".equals(name)) {
				updateBeanColumnsBuffer.append(dbName).append("=:")
						.append(name).append(",");
			}
		}
		columns = columnsBuffer.substring(0, columnsBuffer.length() - 1) + " ";
		insertBeanColumns = insertBeanColumnsBuffer.substring(0,
				insertBeanColumnsBuffer.length() - 1) + " ";
		updateBeanColumns = updateBeanColumnsBuffer.substring(0,
				updateBeanColumnsBuffer.length() - 1) + " ";
		jdbcHelper = new BaseJdbcHelper(columns, columns, insertBeanColumns,
				updateBeanColumns);
		if (primaryFieldInfo != null) {
			String whereColumnsIdentity = " " + primaryFieldInfo.getDbName()
					+ "=:" + primaryFieldInfo.getName() + " ";
			jdbcHelper.setWhere_columns_identity(whereColumnsIdentity);
		}
		if (combinationFieldInfos.size() > 0) {
			String whereColumnsCombination = null;
			StringBuffer whereColumnsCombinationBuffer = new StringBuffer(" ");
			for (FieldInfo combinationFieldInfo : combinationFieldInfos) {
				whereColumnsCombinationBuffer
						.append(combinationFieldInfo.getDbName()).append("=:")
						.append(combinationFieldInfo.getName()).append(" and ");
			}
			whereColumnsCombination = whereColumnsCombinationBuffer.substring(
					0, whereColumnsCombinationBuffer.length() - 5) + " ";
			jdbcHelper.setWhere_columns_combination(whereColumnsCombination);

		}

	}

	/**
	 * 如果有primarykey,没有shardingKey，那么shardingKey默认为primarykey
	 * 如果没有primarykey，只有组合键，必须要指定shardingKey。
	 */
	private void check() {
		if (primaryFieldInfo == null && combinationFieldInfos.size() == 0) {
			throw new IllegalAccessError("系统初始化错误，没找到主键以及组合键:" + entityClass);
		}

		// 主键必须是long|int类型
		if (primaryFieldInfo != null
				&& primaryFieldInfo.getReadMethod().getReturnType() != Long.class
				&& primaryFieldInfo.getReadMethod().getReturnType() != long.class
				&& primaryFieldInfo.getReadMethod().getReturnType() != Integer.class
				&& primaryFieldInfo.getReadMethod().getReturnType() != int.class) {
			throw new IllegalAccessError("系统初始化错误，主键必须是long|int类型:"
					+ entityClass);
		}

		// shdingkey必须是long|int类型
		if (shardingFieldInfo != null
				&& shardingFieldInfo.getReadMethod().getReturnType() != Long.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != long.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != Integer.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != int.class) {
			throw new IllegalAccessError("系统初始化错误，分表字段必须是long|int类型:"
					+ entityClass);
		}

		// sortKey必须是long|int类型
		if (sortFieldInfo != null
				&& sortFieldInfo.getReadMethod().getReturnType() != Double.class
				&& sortFieldInfo.getReadMethod().getReturnType() != double.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != Long.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != long.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != Integer.class
				&& shardingFieldInfo.getReadMethod().getReturnType() != int.class) {
			throw new IllegalAccessError("系统初始化错误，排序字段必须是double,long,int类型:"
					+ entityClass);
		}

		if (shardingFieldInfo == null) {
			throw new IllegalAccessError("系统初始化错误，找不到分表字段:" + entityClass);
		}

		// 判断是否长度一致
		if (isShardingTable) {
			if (domain.DbShardingHighWaters().length != domain
					.DbShardingSeeds().length) {
				throw new IllegalAccessError("系统初始化错误，分库策略不对:" + entityClass);
			}
			if (domain.tableShardingHighWaters().length != domain
					.tableShardingSeeds().length) {
				throw new IllegalAccessError("系统初始化错误，分表策略不对:" + entityClass);
			}
		}

		if(domain.hasSort()) {
            if (domain.domainType() == HyperspaceDomainType.listDataStructure
                    && sortFieldInfo == null) {
                throw new IllegalAccessError("list结构必须有标识sort字段" + entityClass);
            }
        }

	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		try {
			Object o = entityClass.newInstance();
			for (FieldInfo info : fields) {
				String c = info.getDbName();
				if (c.startsWith("`") && c.endsWith("`")) {
					c = c.substring(1, c.length() - 1);
				}
				Method writeMethod = info.getWriteMethod();
				if(info.isJson()){
					String jsonString= (String) ReflectUtil.getValueFormRsByType(String.class, rs, c);
					if(StringUtils.isNotBlank(jsonString)){
						writeMethod.invoke(o, JsonUtils.parse(jsonString,info.getField().getGenericType()));
					}
				}else{
					Type type = info.getField().getGenericType();
					writeMethod.invoke(o, ReflectUtil.getValueFormRsByType(type, rs, c));
				}


			}
			return (T) o;
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	protected void afterWirterPrimaryValue(T model) {
	}

	/**
	 * 插入数据
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected T jdbc_insert(T model) throws DataAccessException {
		long startTime=System.currentTimeMillis();
		String time = String.valueOf(System.currentTimeMillis());
		String insertBeanColumns = this.jdbcHelper.getInsert_bean_columns()
				.replace(":ctime", time).replace(":utime", time);
		if (primaryFieldInfo != null
				&& this.identityType == IdentityType.identity) {
			try {
				this.primaryFieldInfo.getWriteMethod().invoke(model,
						this.nextIncrementer());
				afterWirterPrimaryValue(model);
			} catch (Exception e) {
				e.printStackTrace();
				throw new DataAccessResourceFailureException("", e);
			}
		}
		String insertColumns = this.jdbcHelper.getInsert_columns();
		if( this.identityType == IdentityType.origin_indentity && primaryFieldInfo != null ){
			insertColumns = insertColumns.replace(String.format(",%s,", primaryFieldInfo.getDbName()), ",");
			insertBeanColumns = insertBeanColumns.replace(String.format(":%s,", primaryFieldInfo.getName()), "");
		}
		String sql = String.format("INSERT INTO %s (%s) values( %s )",
				getTable(model), insertColumns,
				insertBeanColumns);

		int ret = -1;
		if (null != primaryFieldInfo
				&& this.identityType == IdentityType.origin_indentity) {
			KeyHolder key = new GeneratedKeyHolder();
			ret = getJdbcTemplate(model, false).update(sql,
							new HyperspaceBeanPropertySqlParameterSource(model), key);
			try {
				this.primaryFieldInfo.getWriteMethod().invoke(model,
						key.getKey().longValue());
			} catch (Exception e) {
				e.printStackTrace();
				throw new DataAccessResourceFailureException("", e);
			}
		} else {
			ret = getJdbcTemplate(model, false).update(sql,
							new HyperspaceBeanPropertySqlParameterSource(model));
		}

		try {

			if (utimeFieldInfo != null) {
				utimeFieldInfo.getWriteMethod().invoke(model,
						Long.valueOf(time));
			}
			if (ctimeFieldInfo != null) {
				ctimeFieldInfo.getWriteMethod().invoke(model,
						Long.valueOf(time));
			}
		} catch (Exception e) {
			throw new RuntimeException("", e);
		}

		logger.info(daoClassName + "insert sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));
		
		return ret >= -1 ? model : null;
	}

	/***
	 * 根据id主键进行更新数据，分表字段必须是id主键）
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_update_identity(T model) throws DataAccessException {

		long startTime=System.currentTimeMillis();
		if (isShardingTable && shardingFieldInfo != primaryFieldInfo) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String dbName = this.primaryFieldInfo.getDbName();
			Object value = this.primaryFieldInfo.getReadMethod().invoke(model);
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_identity();
			sql = sql.replaceAll(":" + primaryFieldInfo.getName() + " ",
					value.toString() + " ");
			sql = String.format(sql, this.getTable(model),
					this.jdbcHelper.getUpdate_bean_columns());

			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				sql = sql.replace(":utime ", Long.toString(utime) + " ");
				utimeFieldInfo.getWriteMethod().invoke(model, utime);
			}

			int ret = getJdbcTemplate(model, false).update(sql,
					new HyperspaceBeanPropertySqlParameterSource(model));

			logger.info(daoClassName + "update id sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));
			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}
	


	/**
	 * 根据组合键更新数据，组合键中必须包含有分表字段）
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_update_combination(T model)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		if (isShardingTable
				&& !combinationFieldInfos.contains(shardingFieldInfo)) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_combination();
			for (FieldInfo one : this.combinationFieldInfos) {
				String dbName = one.getDbName();
				Object value = one.getReadMethod().invoke(model, null);
				if (value instanceof String) {
					sql = sql.replaceAll(":" + one.getName() + " ",
							"'" + value.toString() + "' ");
				} else {
					sql = sql.replaceAll(":" + one.getName() + " ",
							value.toString() + " ");
				}
			}
			sql = String.format(sql, this.getTable(model),
					this.jdbcHelper.getUpdate_bean_columns());
			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				sql = sql.replace(":utime ", Long.toString(utime) + " ");
				utimeFieldInfo.getWriteMethod().invoke(model, utime);
			}

			int ret = getJdbcTemplate(model, false).update(sql,
					new HyperspaceBeanPropertySqlParameterSource(model));
			logger.info(daoClassName + "update combination sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}
	
	

	/***
	 * 根据id主键进行 数据某个字段自增(利用数据库事物保证原子性，不是先查出来，再更改的模式)
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_increment_identity(T model,String filedName,int incrementValue) throws DataAccessException {
		long startTime=System.currentTimeMillis();
		if (isShardingTable && shardingFieldInfo != primaryFieldInfo) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String dbName = this.primaryFieldInfo.getDbName();
			Object value = this.primaryFieldInfo.getReadMethod().invoke(model);
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_identity();
			sql = sql.replaceAll(":" + primaryFieldInfo.getName() + " ",
					value.toString()  + " ");
			
			String updatePartSql=filedName+"=("+filedName;
			if(incrementValue>0){
				updatePartSql=updatePartSql+"+"+incrementValue+")";
			}else{
				updatePartSql=updatePartSql+"-"+(0-incrementValue)+")";
			}
					
			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				updatePartSql = updatePartSql+(",utime="+ Long.toString(utime) + " ");
			}
			sql = String.format(sql, this.getTable(model),updatePartSql);
			int ret = getJdbcTemplate(model, false).getJdbcTemplate().update(sql);
			logger.info(daoClassName + "update increment id sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));
			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}


	/***
	 * 根据id主键进行 数据某个字段自增(利用数据库事物保证原子性，不是先查出来，再更改的模式)
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_increment_identity(T model,List<String> filedNameList,List<Integer> incrementValueList) throws DataAccessException {
		long startTime=System.currentTimeMillis();
		if (isShardingTable && shardingFieldInfo != primaryFieldInfo) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String dbName = this.primaryFieldInfo.getDbName();
			Object value = this.primaryFieldInfo.getReadMethod().invoke(model);
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_identity();
			sql = sql.replaceAll(":" + primaryFieldInfo.getName() + " ",
					value.toString()  + " ");

			String updatePartSql="";
			if(filedNameList.size()!=incrementValueList.size()){
				throw new IllegalAccessError("increment 操作，参数list数量不对等");
			}
			for(int i=0;i<filedNameList.size();i++){
				String filedName =filedNameList.get(i);
				int incrementValue=incrementValueList.get(i);
				updatePartSql=filedName+"=("+filedName;
				if(incrementValue>0){
					updatePartSql=updatePartSql+"+"+incrementValue+")";
				}else{
					updatePartSql=updatePartSql+"-"+(0-incrementValue)+")";
				}
				if(i!=filedNameList.size()-1){
					updatePartSql=updatePartSql+",";
				}
			}

			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				updatePartSql = updatePartSql+(",utime="+ Long.toString(utime) + " ");
			}
			sql = String.format(sql, this.getTable(model),updatePartSql);
			int ret = getJdbcTemplate(model, false).getJdbcTemplate().update(sql);
			logger.info(daoClassName + "update increment id sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));
			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}
	
	/***
	 * 根据组合键进行 数据某个字段自增(利用数据库事物保证原子性，不是先查出来，再更改的模式)
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_increment_combination(T model,String filedName,int incrementValue)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		if (isShardingTable
				&& !combinationFieldInfos.contains(shardingFieldInfo)) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_combination();
			for (FieldInfo one : this.combinationFieldInfos) {
				String dbName = one.getDbName();
				Object value = one.getReadMethod().invoke(model, null);
				if (value instanceof String) {
					sql = sql.replaceAll(":" + one.getName() + " ",
							"'" + value.toString() + "' ");
				} else {
					sql = sql.replaceAll(":" + one.getName() + " ",
							value.toString() + " ");
				}
			}
			
			String updatePartSql=filedName+"=("+filedName;
			if(incrementValue>0){
				updatePartSql=updatePartSql+"+"+incrementValue+")";
			}else{
				updatePartSql=updatePartSql+"-"+(0-incrementValue)+")";
			}
					
			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				updatePartSql = updatePartSql+(",utime="+ Long.toString(utime) + " ");
			}
			sql = String.format(sql, this.getTable(model),
					updatePartSql);

			int ret = getJdbcTemplate(model, false).getJdbcTemplate().update(sql);
			logger.info(daoClassName + "update increment combination sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}



	/***
	 * 根据组合键进行 数据某个字段自增(利用数据库事物保证原子性，不是先查出来，再更改的模式)
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_increment_combination(T model,List<String> filedNameList,List<Integer> incrementValueList)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		if (isShardingTable
				&& !combinationFieldInfos.contains(shardingFieldInfo)) {
			throw new IllegalAccessError("找不到分表字段");
		}
		try {
			String sql = "UPDATE %s set %s where "
					+ this.jdbcHelper.getWhere_columns_combination();
			for (FieldInfo one : this.combinationFieldInfos) {
				String dbName = one.getDbName();
				Object value = one.getReadMethod().invoke(model, null);
				if (value instanceof String) {
					sql = sql.replaceAll(":" + one.getName() + " ",
							"'" + value.toString() + "' ");
				} else {
					sql = sql.replaceAll(":" + one.getName() + " ",
							value.toString() + " ");
				}
			}

			String updatePartSql="";
			if(filedNameList.size()!=incrementValueList.size()){
				throw new IllegalAccessError("increment 操作，参数list数量不对等");
			}
			for(int i=0;i<filedNameList.size();i++){
				String filedName =filedNameList.get(i);
				int incrementValue=incrementValueList.get(i);
				updatePartSql=filedName+"=("+filedName;
				if(incrementValue>0){
					updatePartSql=updatePartSql+"+"+incrementValue+")";
				}else{
					updatePartSql=updatePartSql+"-"+(0-incrementValue)+")";
				}
				if(i!=filedNameList.size()-1){
					updatePartSql=updatePartSql+",";
				}
			}

			if (utimeFieldInfo != null) {
				long utime = new Date().getTime();
				updatePartSql = updatePartSql+(",utime="+ Long.toString(utime) + " ");
			}
			sql = String.format(sql, this.getTable(model),
					updatePartSql);

			int ret = getJdbcTemplate(model, false).getJdbcTemplate().update(sql);
			logger.info(daoClassName + "update increment combination sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}

	/***
	 * 根据id主键删除数据，分表字段必须是id主键
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_delete_identity(long id) throws DataAccessException {
		if (isShardingTable && shardingFieldInfo != primaryFieldInfo) {
			throw new IllegalAccessError("找不到分表字段");
		}
		return jdbc_delete_identity(id, id);
	}

	/**
	 * 根据组合键删除数据，组合键中必须包含有分表字段）
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_delete_combination(T model)
			throws DataAccessException {
		if (isShardingTable
				&& !combinationFieldInfos.contains(shardingFieldInfo)) {
			throw new IllegalAccessError("找不到分表字段");
		}
		long sharding = getReadMethodForLong(shardingFieldInfo.getReadMethod(),
				model);
		return jdbc_delete_combination(model, sharding);
	}

	/***
	 * 根据id主键加载数据，分表字段必须是id主键
	 *
	 * @return
	 * @throws DataAccessException
	 */
	protected T jdbc_load_identity(long id) throws DataAccessException {
		if (isShardingTable && shardingFieldInfo != primaryFieldInfo) {
			throw new IllegalAccessError("找不到分表字段");
		}
		return jdbc_load_identity(id, id);
	}

	/**
	 * 根据组合键加载数据，组合键中必须包含有分表字段）
	 *
	 * @param model
	 * @return
	 * @throws DataAccessException
	 */
	protected T jdbc_load_combination(T model) throws DataAccessException {
		if (isShardingTable
				&& !combinationFieldInfos.contains(shardingFieldInfo)) {
			throw new IllegalAccessError("找不到分表字段");
		}
		long sharding = getReadMethodForLong(shardingFieldInfo.getReadMethod(),
				model);
		return jdbc_load_combination(model, sharding);
	}

	/**
	 * 根据ID主键和分表字段 进行删除数据
	 *
	 * @param id
	 * @param sharding
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_delete_identity(long id, long sharding)
			throws DataAccessException {		
		long startTime=System.currentTimeMillis();
		String sql = "DELETE FROM %s WHERE "
				+ this.jdbcHelper.getWhere_columns_identity();
		sql = sql.replaceAll(":" + this.primaryFieldInfo.getName() + " ",
				String.valueOf(id) + " ");
		sql = String.format(sql, this.getTable(sharding));

		int ret = getJdbcTemplate(sharding, false).getJdbcTemplate().update(sql);
		logger.info(daoClassName + "delete id sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

		return ret >= 1 ? true : false;
	}

	/**
	 * 根据组合键和分表字段 进行删除数据
	 *
	 * @param sharding
	 * @return
	 * @throws DataAccessException
	 */
	protected boolean jdbc_delete_combination(T model, long sharding)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		try {
			String sql = "DELETE FROM %s WHERE "
					+ this.jdbcHelper.getWhere_columns_combination();
			for (FieldInfo one : this.combinationFieldInfos) {
				String dbName = one.getDbName();
				Object value = one.getReadMethod().invoke(model, null);
				if (value instanceof String) {
					sql = sql.replaceAll(":" + one.getName() + " ",
							"'" + value.toString() + "' ");
				} else {
					sql = sql.replaceAll(":" + one.getName() + " ",
							value.toString() + " ");
				}
			}
			sql = String.format(sql, this.getTable(sharding));

			int ret = getJdbcTemplate(sharding, false).getJdbcTemplate().update(sql);
			logger.info(daoClassName + "delete combination sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			return ret >= 1 ? true : false;
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
	}

	/**
	 * 根据ID主键和分表字段 进行加载数据
	 *
	 * @param id
	 * @param sharding
	 * @return
	 * @throws DataAccessException
	 */
	protected T jdbc_load_identity(long id, long sharding)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		List<T> list = null;
		String sql = "SELECT %s FROM %s WHERE "
				+ this.jdbcHelper.getWhere_columns_identity();
		sql = sql.replaceAll(":" + this.primaryFieldInfo.getName() + " ",
				String.valueOf(id) + " ");
		sql = String.format(sql, this.jdbcHelper.getColumns(),
				this.getTable(sharding));

		try {
			list = this.getJdbcTemplate(sharding, true).query(sql, this);
			logger.info(daoClassName + "load id sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			if (list != null && list.size() > 0) {
				T ret = list.get(0);
				return ret;
			}
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
		return null;
	}

	/**
	 * 根据组合键和分表字段 进行加载数据
	 *
	 * @param sharding
	 * @return
	 * @throws DataAccessException
	 */
	protected T jdbc_load_combination(T model, long sharding)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		List<T> list = null;
		try {
			String sql = "SELECT %s FROM %s WHERE "
					+ this.jdbcHelper.getWhere_columns_combination();
			for (FieldInfo one : this.combinationFieldInfos) {
				String dbName = one.getDbName();
				Object value = one.getReadMethod().invoke(model, null);
				if (value instanceof String) {
					sql = sql.replaceAll(":" + one.getName() + " ",
							"'" + value.toString() + "' ");
				} else {
					sql = sql.replaceAll(":" + one.getName() + " ",
							value.toString() + " ");
				}
			}
			sql = String.format(sql, this.jdbcHelper.getColumns(),
					this.getTable(sharding));

			list = this.getJdbcTemplate(sharding, true).query(sql, this);
			logger.info(daoClassName + "load combination sql:{},time:{}", sql,(System.currentTimeMillis()-startTime));

			if (list != null && list.size() > 0) {
				T ret = list.get(0);
				return ret;
			}
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("", e);
		}
		return null;
	}

	// ///////////////////////////////////////////////////////////////////

	public List<T> jdbc_getList(long shardingId, String sql, Object[] objects)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		List<T> result = this.getJdbcTemplate(shardingId, true).getJdbcTemplate().query(sql,
				this, objects);
		logger.info(daoClassName + "getlist sql:{},params:{},time:{}", new Object[]{sql, objects,(System.currentTimeMillis()-startTime)});
		if (null != result) {
			return result;
		} else {
			return new ArrayList<T>();
		}
	}

	public int jdbc_getCount(long shardingId, String sql, Object[] objects)
			throws DataAccessException {
		long startTime=System.currentTimeMillis();

		int count = 0;
		count = this.getJdbcTemplate(shardingId, true).getJdbcTemplate()
				.queryForObject(sql, objects, Integer.class);
		logger.info(daoClassName + "getCount sql:{},params:{},time:{}", new Object[]{sql, objects,(System.currentTimeMillis()-startTime)});
		return count;
	}

	/**
	 * 根据domain对象 获取分库的jdbc连接
	 *
	 * @param model
	 * @return
	 */
	public NamedParameterJdbcTemplate getJdbcTemplate(T model, boolean slave) {
        NamedParameterJdbcTemplate simpleJdbcTemplate;
		if (!isShardingTable) {
			simpleJdbcTemplate = getJdbcTemplate(0, slave);
		} else {
			long sharding = getReadMethodForLong(
					shardingFieldInfo.getReadMethod(), model);
			simpleJdbcTemplate = getJdbcTemplate(sharding, slave);
		}
		return simpleJdbcTemplate;
	}

	/**
	 * 根据domain对象 获取分表的表名
	 *
	 * @param model
	 * @return
	 */
	public String getTable(T model) {
		String tableName;
		if (!isShardingTable) {
			tableName = getTable(0);
		} else {
			long sharding = getReadMethodForLong(
					shardingFieldInfo.getReadMethod(), model);
			tableName = getTable(sharding);
		}
		return tableName;
	}

	/**
	 * 根据分表字段 获取分库的jdbc连接
	 *
	 * @return
	 */
	public NamedParameterJdbcTemplate getJdbcTemplate(long shardId, boolean slave) {
		int jdbcIndex = getJdbcTemplateIndex(shardId);
		if (slave && slaveJdbcTemplates != null) {
			return slaveJdbcTemplates[jdbcIndex];
		}
		return jdbcTemplates[jdbcIndex];
	}

	private int getJdbcTemplateIndex(long shardId) {
		int[] seeds = domain.DbShardingSeeds();
		long[] highWaters = domain.DbShardingHighWaters();
		int index = 0;
		for (int i = 0; i < highWaters.length; i++) {
			if (shardId <= highWaters[i]) {
				index = i;
				break;
			}
		}
		int seed = seeds[index];
		int tableIndex = 0;
		if (seed > 0) {
			tableIndex = (int) shardId % seed;
		}
		return tableIndex;
	}

	/**
	 * 根据分表字段 获取分表的表名
	 *
	 * @return
	 */
	public String getTable(long shardId) {
		int jdbcIndex = getJdbcTemplateIndex(shardId);

		int[] seeds = domain.tableShardingSeeds();
		long[] highWaters = domain.tableShardingHighWaters();
		int index = 0;
		for (int i = 0; i < highWaters.length; i++) {
			if (shardId <= highWaters[i]) {
				index = i;
				break;
			}
		}
		int tableIndex = 0;
		if (seeds[index] <= 0) {
			if (Arrays.asList(keywords).contains(tableName.toUpperCase())) {
				return  "`" + tableName + "`";
			}
			return tableName;
		} else {
			// 可以根据复杂公式（jdbcIndex） 均匀hash，但是不好查数据
			tableIndex = (int) shardId % seeds[index];
			if (tableIndex == 0) {
				if (Arrays.asList(keywords).contains(tableName.toUpperCase())) {
					return  "`" + tableName + "`";
				}
				return tableName;
			}
			return tableName + "_" + tableIndex;
		}
	}

	public long nextIncrementer() {
		long value = this.incrementer.nextLongValue();
		return value;
	}

	private long getReadMethodForLong(Method method, Object obj) {
		Class clazz = method.getReturnType();
		Object value;
		try {
			value = method.invoke(obj);
		} catch (Exception e) {
			throw new IllegalAccessError("无法调用反射方法");
		}
		if (clazz == Long.class || clazz == long.class) {
			return (Long) value;
		} else if (clazz == Integer.class || clazz == int.class) {
			return (Integer) value;
		} else {
			throw new IllegalAccessError("无法识别返回类型");
		}

	}

	public NamedParameterJdbcTemplate[] getJdbcTemplates() {
		return jdbcTemplates;
	}

	public void setJdbcTemplates(NamedParameterJdbcTemplate[] jdbcTemplates) {
		this.jdbcTemplates = jdbcTemplates;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public PropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	public void setPropertyDescriptors(PropertyDescriptor[] propertyDescriptors) {
		this.propertyDescriptors = propertyDescriptors;
	}

	public List<FieldInfo> getFields() {
		return fields;
	}

	public void setFields(List<FieldInfo> fields) {
		this.fields = fields;
	}

	public FieldInfo getPrimaryFieldInfo() {
		return primaryFieldInfo;
	}

	public void setPrimaryFieldInfo(FieldInfo primaryFieldInfo) {
		this.primaryFieldInfo = primaryFieldInfo;
	}

	public FieldInfo getShardingFieldInfo() {
		return shardingFieldInfo;
	}

	public void setShardingFieldInfo(FieldInfo shardingFieldInfo) {
		this.shardingFieldInfo = shardingFieldInfo;
	}

	public FieldInfo getSortFieldInfo() {
		return sortFieldInfo;
	}

	public void setSortFieldInfo(FieldInfo sortFieldInfo) {
		this.sortFieldInfo = sortFieldInfo;
	}

	public List<FieldInfo> getCombinationFieldInfos() {
		return combinationFieldInfos;
	}

	public void setCombinationFieldInfos(List<FieldInfo> combinationFieldInfos) {
		this.combinationFieldInfos = combinationFieldInfos;
	}

	public HyperspaceDomainType getDomainType() {
		return domainType;
	}

	public void setDomainType(HyperspaceDomainType domainType) {
		this.domainType = domainType;
	}

	public IdentityType getIdentityType() {
		return identityType;
	}

	public void setIdentityType(IdentityType identityType) {
		this.identityType = identityType;
	}

	public HyperspaceDomain getDomain() {
		return domain;
	}

	public void setDomain(HyperspaceDomain domain) {
		this.domain = domain;
	}

	public boolean isShardingTable() {
		return isShardingTable;
	}

	public void setShardingTable(boolean isShardingTable) {
		this.isShardingTable = isShardingTable;
	}

	
	public static void main(String[] sdg){
		System.out.println("''''");
	}
}
