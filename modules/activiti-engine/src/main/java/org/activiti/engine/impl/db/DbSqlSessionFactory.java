/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.db;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.activiti.engine.impl.cfg.IdGenerator;
import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntity;
import org.apache.ibatis.session.SqlSessionFactory;


/**
 * @author Tom Baeyens
 * 该类非常重要 ,定义了一系列 获取SQL 执行值 id的 ()
 * eg: <insert id="bulkInsertMembership"   > </insert>
 * 则这里的SQL 执行 id值为 bulkInsertMembership
 * 该类中的静态代码块非常重要, 这些管理类都需要实现Session接口
 *
 * 因此 程序和DB 的会话是短暂的, 所以SqlSession的生命周期 与 会话绑定在一起,
 * 每次访问DB时都需要创建SqlSession实例 ,会话关闭,
 * 如果需要再次与DB 会话, 就需要重新创建 SqlSession实例
 * SqLSession 可以执行单一的一条SQL  也可以执行多条
 *
 * SqlSession类中定义了一系列的执行SQL语句的()
 * 开发人员 可以直接操作 SqlSessionFactory 对象
 * 这样 DbsQLSessionFactory 就可以通过 SqlSessionFactory 获取 SqlSession
 * 然后 对DB 中的表进行操作
 */
public class DbSqlSessionFactory implements SessionFactory {
  /*
  * SQL语句适配器,
  * MyBatis 中的映射 时如何定义的???
  *     现在定义一个查询语句, 可以直接将SQL 语句在Select语句中  然后为这个seect标签定义一个全局唯一的id值
  * 如果SQl执行过程中需要外部传入参数,则直接使用parmeterType 属性定义即可
  * 查询到的结果 可以使用 resultType 和 resultMap  定义
  * 这样只需要查询标签中的 id 值就可以定位并执行SQL 语句
  *
  * 既然Activiti 使用mybatis 作为ORM  ,则映射文件的定义步骤必不可少,
  *
  * 思考一个问题
  * Activiti可以使用mysql, oracle 也可以使用 xx  这些不同的DB 的 sqk 语法完全不同,
  * 如何屏蔽差异 ???
  * 那可以直接在映射文件中  定义2个insert标签
  * 一遍针对mysql  另一边 oracle
  * Activiti确实是这么做的, 要是这样设计 Activiti 在运行的时候就必须知道 DB的类型
  * 进而在操作的时候才能根据DB的类型选择不同的SQL 语句
  * 看到这里就会明白 processEngineConfiguration   初始化dataSurce的过程中
  * 需要获取 DB的类型  databaseType值,   此值 就是为这里服务的
  *
  *
  * /////////
  * 此 是一个 静态变量,
  * 因此从该集合中获取数据 时,  会触发当前类中的    静态代码块
  * */

  protected static final Map<String, Map<String, String>> databaseSpecificStatements = new HashMap<String, Map<String,String>>();
  
  public static final Map<String, String> databaseSpecificLimitBeforeStatements = new HashMap<String, String>();
  public static final Map<String, String> databaseSpecificLimitAfterStatements = new HashMap<String, String>();
  public static final Map<String, String> databaseSpecificLimitBetweenStatements = new HashMap<String, String>();
  public static final Map<String, String> databaseSpecificOrderByStatements = new HashMap<String, String>();
  public static final Map<String, String> databaseOuterJoinLimitBetweenStatements = new HashMap<String, String>();
  public static final Map<String, String> databaseSpecificLimitBeforeNativeQueryStatements = new HashMap<String, String>();

  static {
    
    String defaultOrderBy = " order by ${orderByColumns} ";
    
    // h2
    databaseSpecificLimitBeforeStatements.put("h2", "");
    databaseSpecificLimitAfterStatements.put("h2", "LIMIT #{maxResults} OFFSET #{firstResult}");
    databaseSpecificLimitBetweenStatements.put("h2", "");
    databaseOuterJoinLimitBetweenStatements.put("h2", "");
    databaseSpecificOrderByStatements.put("h2", defaultOrderBy);

    // hsql
    databaseSpecificLimitBeforeStatements.put("hsql", "");
    databaseSpecificLimitAfterStatements.put("hsql", "LIMIT #{maxResults} OFFSET #{firstResult}");
    databaseSpecificLimitBetweenStatements.put("hsql", "");
    databaseOuterJoinLimitBetweenStatements.put("hsql", "");
    databaseSpecificOrderByStatements.put("hsql", defaultOrderBy);

    
	  //mysql specific
    databaseSpecificLimitBeforeStatements.put("mysql", "");
    databaseSpecificLimitAfterStatements.put("mysql", "LIMIT #{maxResults} OFFSET #{firstResult}");
    databaseSpecificLimitBetweenStatements.put("mysql", "");
    databaseOuterJoinLimitBetweenStatements.put("mysql", "");
    databaseSpecificOrderByStatements.put("mysql", defaultOrderBy);
    addDatabaseSpecificStatement("mysql", "selectProcessDefinitionsByQueryCriteria", "selectProcessDefinitionsByQueryCriteria_mysql");
    addDatabaseSpecificStatement("mysql", "selectProcessDefinitionCountByQueryCriteria", "selectProcessDefinitionCountByQueryCriteria_mysql");
    addDatabaseSpecificStatement("mysql", "selectDeploymentsByQueryCriteria", "selectDeploymentsByQueryCriteria_mysql");
    addDatabaseSpecificStatement("mysql", "selectDeploymentCountByQueryCriteria", "selectDeploymentCountByQueryCriteria_mysql");
    addDatabaseSpecificStatement("mysql", "selectModelCountByQueryCriteria", "selectModelCountByQueryCriteria_mysql");
    addDatabaseSpecificStatement("mysql", "updateExecutionTenantIdForDeployment", "updateExecutionTenantIdForDeployment_mysql");
    addDatabaseSpecificStatement("mysql", "updateTaskTenantIdForDeployment", "updateTaskTenantIdForDeployment_mysql");
    addDatabaseSpecificStatement("mysql", "updateJobTenantIdForDeployment", "updateJobTenantIdForDeployment_mysql");
    
    //postgres specific
    databaseSpecificLimitBeforeStatements.put("postgres", "");
    databaseSpecificLimitAfterStatements.put("postgres", "LIMIT #{maxResults} OFFSET #{firstResult}");
    databaseSpecificLimitBetweenStatements.put("postgres", "");
    databaseOuterJoinLimitBetweenStatements.put("postgres", "");
    databaseSpecificOrderByStatements.put("postgres", defaultOrderBy);
    addDatabaseSpecificStatement("postgres", "insertByteArray", "insertByteArray_postgres");
    addDatabaseSpecificStatement("postgres", "bulkInsertByteArray", "bulkInsertByteArray_postgres");
    addDatabaseSpecificStatement("postgres", "updateByteArray", "updateByteArray_postgres");
    addDatabaseSpecificStatement("postgres", "selectByteArray", "selectByteArray_postgres");
    addDatabaseSpecificStatement("postgres", "selectResourceByDeploymentIdAndResourceName", "selectResourceByDeploymentIdAndResourceName_postgres");
    addDatabaseSpecificStatement("postgres", "selectResourcesByDeploymentId", "selectResourcesByDeploymentId_postgres");
    addDatabaseSpecificStatement("postgres", "insertIdentityInfo", "insertIdentityInfo_postgres");
    addDatabaseSpecificStatement("postgres", "bulkInsertIdentityInfo", "bulkInsertIdentityInfo_postgres");
    addDatabaseSpecificStatement("postgres", "updateIdentityInfo", "updateIdentityInfo_postgres");
    addDatabaseSpecificStatement("postgres", "selectIdentityInfoById", "selectIdentityInfoById_postgres");
    addDatabaseSpecificStatement("postgres", "selectIdentityInfoByUserIdAndKey", "selectIdentityInfoByUserIdAndKey_postgres");
    addDatabaseSpecificStatement("postgres", "selectIdentityInfoByUserId", "selectIdentityInfoByUserId_postgres");
    addDatabaseSpecificStatement("postgres", "selectIdentityInfoDetails", "selectIdentityInfoDetails_postgres");
    addDatabaseSpecificStatement("postgres", "insertComment", "insertComment_postgres");
    addDatabaseSpecificStatement("postgres", "bulkInsertComment", "bulkInsertComment_postgres");
    addDatabaseSpecificStatement("postgres", "selectComment", "selectComment_postgres");
    addDatabaseSpecificStatement("postgres", "selectCommentsByTaskId", "selectCommentsByTaskId_postgres");
    addDatabaseSpecificStatement("postgres", "selectCommentsByProcessInstanceId", "selectCommentsByProcessInstanceId_postgres");
    addDatabaseSpecificStatement("postgres", "selectCommentsByProcessInstanceIdAndType", "selectCommentsByProcessInstanceIdAndType_postgres");
    addDatabaseSpecificStatement("postgres", "selectCommentsByType", "selectCommentsByType_postgres");
    addDatabaseSpecificStatement("postgres", "selectCommentsByTaskIdAndType", "selectCommentsByTaskIdAndType_postgres");
    addDatabaseSpecificStatement("postgres", "selectEventsByTaskId", "selectEventsByTaskId_postgres");
    addDatabaseSpecificStatement("postgres", "insertEventLogEntry", "insertEventLogEntry_postgres");
    addDatabaseSpecificStatement("postgres", "bulkInsertEventLogEntry", "bulkInsertEventLogEntry_postgres");
    addDatabaseSpecificStatement("postgres", "selectAllEventLogEntries", "selectAllEventLogEntries_postgres");
    addDatabaseSpecificStatement("postgres", "selectEventLogEntries", "selectEventLogEntries_postgres");
    addDatabaseSpecificStatement("postgres", "selectEventLogEntriesByProcessInstanceId", "selectEventLogEntriesByProcessInstanceId_postgres");
        
    // oracle
    databaseSpecificLimitBeforeStatements.put("oracle", "select * from ( select a.*, ROWNUM rnum from (");
    databaseSpecificLimitAfterStatements.put("oracle", "  ) a where ROWNUM < #{lastRow}) where rnum  >= #{firstRow}");
    databaseSpecificLimitBetweenStatements.put("oracle", "");
    databaseOuterJoinLimitBetweenStatements.put("oracle", "");
    databaseSpecificOrderByStatements.put("oracle", defaultOrderBy);
    addDatabaseSpecificStatement("oracle", "selectExclusiveJobsToExecute", "selectExclusiveJobsToExecute_integerBoolean");
    addDatabaseSpecificStatement("oracle", "selectUnlockedTimersByDuedate", "selectUnlockedTimersByDuedate_oracle");
    addDatabaseSpecificStatement("oracle", "insertEventLogEntry", "insertEventLogEntry_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertVariableInstance", "bulkInsertVariableInstance_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertUser", "bulkInsertUser_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertTask", "bulkInsertTask_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertResource", "bulkInsertResource_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertProperty", "bulkInsertProperty_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertProcessDefinition", "bulkInsertProcessDefinition_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertModel", "bulkInsertModel_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertMembership", "bulkInsertMembership_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertTimer", "bulkInsertTimer_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertMessage", "bulkInsertMessage_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertIdentityInfo", "bulkInsertIdentityInfo_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertIdentityLink", "bulkInsertIdentityLink_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertMembership", "bulkInsertMembership_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertTimer", "bulkInsertTimer_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertMessage", "bulkInsertMessage_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricVariableInstance", "bulkInsertHistoricVariableInstance_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricTaskInstance", "bulkInsertHistoricTaskInstance_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricProcessInstance", "bulkInsertHistoricProcessInstance_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricIdentityLink", "bulkInsertHistoricIdentityLink_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricDetailVariableInstanceUpdate", "bulkInsertHistoricDetailVariableInstanceUpdate_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricFormProperty", "bulkInsertHistoricFormProperty_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertHistoricActivityInstance", "bulkInsertHistoricActivityInstance_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertGroup", "bulkInsertGroup_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertExecution", "bulkInsertExecution_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertMessageEventSubscription", "bulkInsertMessageEventSubscription_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertSignalEventSubscription", "bulkInsertSignalEventSubscription_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertCompensateEventSubscription", "bulkInsertCompensateEventSubscription_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertEventLogEntry", "bulkInsertEventLogEntry_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertDeployment", "bulkInsertDeployment_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertComment", "bulkInsertComment_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertByteArray", "bulkInsertByteArray_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertEventLogEntry", "bulkInsertEventLogEntry_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertDeployment", "bulkInsertDeployment_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertComment", "bulkInsertComment_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertByteArray", "bulkInsertByteArray_oracle");
    addDatabaseSpecificStatement("oracle", "bulkInsertAttachment", "bulkInsertAttachment_oracle");

    // db2
    databaseSpecificLimitBeforeStatements.put("db2", "SELECT SUB.* FROM (");
    databaseSpecificLimitAfterStatements.put("db2", ")RES ) SUB WHERE SUB.rnk >= #{firstRow} AND SUB.rnk < #{lastRow}");
    databaseSpecificLimitBetweenStatements.put("db2", ", row_number() over (ORDER BY ${orderByColumns}) rnk FROM ( select distinct RES.* ");
    databaseOuterJoinLimitBetweenStatements.put("db2", ", row_number() over (ORDER BY ${mssqlOrDB2OrderBy}) rnk FROM ( select distinct ");
    databaseSpecificOrderByStatements.put("db2", "");
    databaseSpecificLimitBeforeNativeQueryStatements.put("db2", "SELECT SUB.* FROM ( select RES.* , row_number() over (ORDER BY ${orderByColumns}) rnk FROM (");
    addDatabaseSpecificStatement("db2", "selectExclusiveJobsToExecute", "selectExclusiveJobsToExecute_integerBoolean");
    addDatabaseSpecificStatement("db2", "selectExecutionByNativeQuery", "selectExecutionByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricActivityInstanceByNativeQuery", "selectHistoricActivityInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricProcessInstanceByNativeQuery", "selectHistoricProcessInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricTaskInstanceByNativeQuery", "selectHistoricTaskInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectTaskByNativeQuery", "selectTaskByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectProcessDefinitionByNativeQuery", "selectProcessDefinitionByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectDeploymentByNativeQuery", "selectDeploymentByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectGroupByNativeQuery", "selectGroupByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectUserByNativeQuery", "selectUserByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectModelByNativeQuery", "selectModelByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricDetailByNativeQuery", "selectHistoricDetailByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricVariableInstanceByNativeQuery", "selectHistoricVariableInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectTaskWithVariablesByQueryCriteria", "selectTaskWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectProcessInstanceWithVariablesByQueryCriteria", "selectProcessInstanceWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricProcessInstancesWithVariablesByQueryCriteria", "selectHistoricProcessInstancesWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("db2", "selectHistoricTaskInstancesWithVariablesByQueryCriteria", "selectHistoricTaskInstancesWithVariablesByQueryCriteria_mssql_or_db2");

    // mssql
    databaseSpecificLimitBeforeStatements.put("mssql", "SELECT SUB.* FROM (");
    databaseSpecificLimitAfterStatements.put("mssql", ")RES ) SUB WHERE SUB.rnk >= #{firstRow} AND SUB.rnk < #{lastRow}");
    databaseSpecificLimitBetweenStatements.put("mssql", ", row_number() over (ORDER BY ${orderByColumns}) rnk FROM ( select distinct RES.* ");
    databaseOuterJoinLimitBetweenStatements.put("mssql", ", row_number() over (ORDER BY ${mssqlOrDB2OrderBy}) rnk FROM ( select distinct ");
    databaseSpecificOrderByStatements.put("mssql", "");
    databaseSpecificLimitBeforeNativeQueryStatements.put("mssql", "SELECT SUB.* FROM ( select RES.* , row_number() over (ORDER BY ${orderByColumns}) rnk FROM (");
    addDatabaseSpecificStatement("mssql", "selectExclusiveJobsToExecute", "selectExclusiveJobsToExecute_integerBoolean");
    addDatabaseSpecificStatement("mssql", "selectExecutionByNativeQuery", "selectExecutionByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricActivityInstanceByNativeQuery", "selectHistoricActivityInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricProcessInstanceByNativeQuery", "selectHistoricProcessInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricTaskInstanceByNativeQuery", "selectHistoricTaskInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectTaskByNativeQuery", "selectTaskByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectProcessDefinitionByNativeQuery", "selectProcessDefinitionByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectDeploymentByNativeQuery", "selectDeploymentByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectGroupByNativeQuery", "selectGroupByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectUserByNativeQuery", "selectUserByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectModelByNativeQuery", "selectModelByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricDetailByNativeQuery", "selectHistoricDetailByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricVariableInstanceByNativeQuery", "selectHistoricVariableInstanceByNativeQuery_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectTaskWithVariablesByQueryCriteria", "selectTaskWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectProcessInstanceWithVariablesByQueryCriteria", "selectProcessInstanceWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricProcessInstancesWithVariablesByQueryCriteria", "selectHistoricProcessInstancesWithVariablesByQueryCriteria_mssql_or_db2");
    addDatabaseSpecificStatement("mssql", "selectHistoricTaskInstancesWithVariablesByQueryCriteria", "selectHistoricTaskInstancesWithVariablesByQueryCriteria_mssql_or_db2");
  }
  
  
  /**
   * A map {class, boolean}, to indicate whether or not a certain {@link PersistentObject} class can be bulk inserted.
   */
  protected static Map<Class<? extends PersistentObject>, Boolean> bulkInsertableMap;
  
  protected String databaseType;
  protected String databaseTablePrefix = "";
  private boolean tablePrefixIsSchema;

  protected String databaseCatalog;
  /**
   * In some situations you want to set the schema to use for table checks /
   * generation if the database metadata doesn't return that correctly, see
   * https://activiti.atlassian.net/browse/ACT-1220,
   * https://activiti.atlassian.net/browse/ACT-1062
   */
  protected String databaseSchema;
  protected SqlSessionFactory sqlSessionFactory;
  protected IdGenerator idGenerator;
  protected Map<String, String> statementMappings;
  protected Map<Class<?>,String>  insertStatements = new ConcurrentHashMap<Class<?>, String>();
  protected Map<Class<?>,String>  bulkInsertStatements = new ConcurrentHashMap<Class<?>, String>();
  protected Map<Class<?>,String>  updateStatements = new ConcurrentHashMap<Class<?>, String>();
  protected Map<Class<?>,String>  deleteStatements = new ConcurrentHashMap<Class<?>, String>();
  protected Map<Class<?>,String>  bulkDeleteStatements = new ConcurrentHashMap<Class<?>, String>();
  protected Map<Class<?>,String>  selectStatements = new ConcurrentHashMap<Class<?>, String>();
  protected boolean isDbIdentityUsed = true;
  protected boolean isDbHistoryUsed = true;
  protected int maxNrOfStatementsInBulkInsert = 100;


  public Class< ? > getSessionType() {
    return DbSqlSession.class;
  }
  /*
  *创建SqlSession 对象
  * */
  public Session openSession() {
    return new DbSqlSession(this);
  }
  
  // insert, update and delete statements /////////////////////////////////////
  
  public String getInsertStatement(PersistentObject object) { //查询insert语句
    return getStatement(object.getClass(), insertStatements, "insert");
  }
  
  
  public String getInsertStatement(Class<? extends PersistentObject> clazz) {
    return getStatement(clazz, insertStatements, "insert");
  }
  
  public String getBulkInsertStatement(Class clazz) {
    return getStatement(clazz, bulkInsertStatements, "bulkInsert");
  }

  public String getUpdateStatement(PersistentObject object) {
    return getStatement(object.getClass(), updateStatements, "update");
  }

  public String getDeleteStatement(Class<?> persistentObjectClass) {
    return getStatement(persistentObjectClass, deleteStatements, "delete");
  }
  
  public String getBulkDeleteStatement(Class<?> persistentObjectClass) {
    return getStatement(persistentObjectClass, bulkDeleteStatements, "bulkDelete");
  }

  public String getSelectStatement(Class<?> persistentObjectClass) {
    return getStatement(persistentObjectClass, selectStatements, "select");
  }
  /*
  * SQL 文件内 id值生成规则
  * cachedStatements是Map结构  ,因此存在引用传递的概念
  * */
  private String getStatement(Class<?> persistentObjectClass, Map<Class<?>,String> cachedStatements, String prefix) {
    //1)首先从缓存中获取 ,如果存在就直接返回
    String statement = cachedStatements.get(persistentObjectClass);
    if (statement!=null) {
      return statement;
    }
    //prefix为 insert,  getSimpleName 为  实体类的名称 ,比如 Group表 就是 GroupEntity
    statement = prefix + persistentObjectClass.getSimpleName();
    statement = statement.substring(0, statement.length()-6); // removing 'entity'   值 inertGroup
    cachedStatements.put(persistentObjectClass, statement);//添加到缓存中
    return statement;
  }

  // db specific mappings /////////////////////////////////////////////////////
  /*
  *
  * */
  protected static void addDatabaseSpecificStatement(String databaseType, String activitiStatement, String ibatisStatement) {
    Map<String, String> specificStatements = databaseSpecificStatements.get(databaseType);
    if (specificStatements == null) {
      specificStatements = new HashMap<String, String>();
      //databaseType 代表表示DB 类型
      databaseSpecificStatements.put(databaseType, specificStatements);
    }
    //那么 这两个参数是什么意思呢 ???  打开任意一个mybatis 的 SQL  XML
//    第一个  id 代表的
//    "selectProcessDefinitionsByQueryCriteria", "selectProcessDefinitionsByQueryCriteria_mysql"
    specificStatements.put(activitiStatement, ibatisStatement);
  }
  /*
  *
  * */
  public String mapStatement(String statement) {
    //此属性 statementMappings 在DBSqlSessionFactory类的 setDatabaseType () 中进行初始化
    if (statementMappings==null) {
      return statement;
    }
    //
    String mappedStatement = statementMappings.get(statement);
    return (mappedStatement!=null ? mappedStatement : statement);
  }
  
  // customized getters and setters ///////////////////////////////////////////
  
  public void setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    this.statementMappings = databaseSpecificStatements.get(databaseType);
  }
  
  public void setBulkInsertEnabled(boolean isBulkInsertEnabled, String databaseType) {
  	// If false, just keep don't initialize the map. Memory saved.
  	if (isBulkInsertEnabled) {
  		initBulkInsertEnabledMap(databaseType);
  	}
  }
  
  protected void initBulkInsertEnabledMap(String databaseType) {
  	bulkInsertableMap = new HashMap<Class<? extends PersistentObject>, Boolean>();
  	
  	for (Class<? extends PersistentObject> clazz : EntityDependencyOrder.INSERT_ORDER) {
  		bulkInsertableMap.put(clazz, Boolean.TRUE);
  	}

  	// Only Oracle is making a fuss in one specific case right now
		if ("oracle".equals(databaseType)) {
			bulkInsertableMap.put(EventLogEntryEntity.class, Boolean.FALSE);
		}
  }
  
  public Boolean isBulkInsertable(Class<? extends PersistentObject> persistentObjectClass) {
  	return bulkInsertableMap != null && bulkInsertableMap.containsKey(persistentObjectClass) && bulkInsertableMap.get(persistentObjectClass) == true;
  }

  // getters and setters //////////////////////////////////////////////////////
  
  public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }
  
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }
  
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
  
  public void setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  
  public String getDatabaseType() {
    return databaseType;
  }

  
  public Map<String, String> getStatementMappings() {
    return statementMappings;
  }

  
  public void setStatementMappings(Map<String, String> statementMappings) {
    this.statementMappings = statementMappings;
  }

  
  public Map<Class< ? >, String> getInsertStatements() {
    return insertStatements;
  }

  
  public void setInsertStatements(Map<Class< ? >, String> insertStatements) {
    this.insertStatements = insertStatements;
  }

  
  public Map<Class< ? >, String> getBulkInsertStatements() {
    return bulkInsertStatements;
  }

  
  public void setBulkInsertStatements(Map<Class< ? >, String> bulkInsertStatements) {
    this.bulkInsertStatements = bulkInsertStatements;
  }

  
  public Map<Class< ? >, String> getUpdateStatements() {
    return updateStatements;
  }

  
  public void setUpdateStatements(Map<Class< ? >, String> updateStatements) {
    this.updateStatements = updateStatements;
  }

  
  public Map<Class< ? >, String> getDeleteStatements() {
    return deleteStatements;
  }

  
  public void setDeleteStatements(Map<Class< ? >, String> deleteStatements) {
    this.deleteStatements = deleteStatements;
  }
  
  
  public Map<Class<?>, String> getBulkDeleteStatements() {
		return bulkDeleteStatements;
	}

	public void setBulkDeleteStatements(Map<Class<?>, String> bulkDeleteStatements) {
		this.bulkDeleteStatements = bulkDeleteStatements;
	}

	public Map<Class< ? >, String> getSelectStatements() {
    return selectStatements;
  }

  
  public void setSelectStatements(Map<Class< ? >, String> selectStatements) {
    this.selectStatements = selectStatements;
  }

  public boolean isDbIdentityUsed() {
    return isDbIdentityUsed;
  }
  
  public void setDbIdentityUsed(boolean isDbIdentityUsed) {
    this.isDbIdentityUsed = isDbIdentityUsed;
  }
  
  public boolean isDbHistoryUsed() {
    return isDbHistoryUsed;
  }
  
  public void setDbHistoryUsed(boolean isDbHistoryUsed) {
    this.isDbHistoryUsed = isDbHistoryUsed;
  }

  public void setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
  }
    
  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }

  public String getDatabaseCatalog() {
    return databaseCatalog;
  }

  public void setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
  }

  public String getDatabaseSchema() {
    return databaseSchema;
  }
  
  public void setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
  }

	public void setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
		this.tablePrefixIsSchema = tablePrefixIsSchema;
  }
	
	public boolean isTablePrefixIsSchema() {
	  return tablePrefixIsSchema;
  }

	public int getMaxNrOfStatementsInBulkInsert() {
		return maxNrOfStatementsInBulkInsert;
	}

	public void setMaxNrOfStatementsInBulkInsert(int maxNrOfStatementsInBulkInsert) {
		this.maxNrOfStatementsInBulkInsert = maxNrOfStatementsInBulkInsert;
	}
	
}
