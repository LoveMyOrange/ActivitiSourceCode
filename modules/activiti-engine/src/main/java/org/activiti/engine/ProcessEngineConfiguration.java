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

package org.activiti.engine;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.cfg.MailServerInfo;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.cfg.BeansConfigurationHelper;
import org.activiti.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.runtime.Clock;
import org.activiti.image.ProcessDiagramGenerator;


/** Configuration information from which a process engine can be build.
 * 
 * <p>Most common is to create a process engine based on the default configuration file:
 * <pre>ProcessEngine processEngine = ProcessEngineConfiguration
 *   .createProcessEngineConfigurationFromResourceDefault()
 *   .buildProcessEngine();
 * </pre>
 * </p>
 * 
 * <p>To create a process engine programatic, without a configuration file, 
 * the first option is {@link #createStandaloneProcessEngineConfiguration()}
 * <pre>ProcessEngine processEngine = ProcessEngineConfiguration
 *   .createStandaloneProcessEngineConfiguration()
 *   .buildProcessEngine();
 * </pre>
 * This creates a new process engine with all the defaults to connect to 
 * a remote h2 database (jdbc:h2:tcp://localhost/activiti) in standalone 
 * mode.  Standalone mode means that Activiti will manage the transactions 
 * on the JDBC connections that it creates.  One transaction per 
 * service method.
 * For a description of how to write the configuration files, see the 
 * userguide.
 * </p>
 * 
 * <p>The second option is great for testing: {@link #createStandalonInMemeProcessEngineConfiguration()}
 * <pre>ProcessEngine processEngine = ProcessEngineConfiguration
 *   .createStandaloneInMemProcessEngineConfiguration()
 *   .buildProcessEngine();
 * </pre>
 * This creates a new process engine with all the defaults to connect to 
 * an memory h2 database (jdbc:h2:tcp://localhost/activiti) in standalone 
 * mode.  The DB schema strategy default is in this case <code>create-drop</code>.  
 * Standalone mode means that Activiti will manage the transactions 
 * on the JDBC connections that it creates.  One transaction per 
 * service method.
 * </p>
 * 
 * <p>On all forms of creating a process engine, you can first customize the configuration 
 * before calling the {@link #buildProcessEngine()} method by calling any of the 
 * setters like this:
 * <pre>ProcessEngine processEngine = ProcessEngineConfiguration
 *   .createProcessEngineConfigurationFromResourceDefault()
 *   .setMailServerHost("gmail.com")
 *   .setJdbcUsername("mickey")
 *   .setJdbcPassword("mouse")
 *   .buildProcessEngine();
 * </pre>
 * </p>
 * 
 * @see ProcessEngines 
 * @author Tom Baeyens
 * @desc
 *  该抽象类实现 EngineServices 接口 提供了一系列创建流程引擎配置类  ProcessEngineConfiguration 对象的()
 *
 *
 *
 *  该类中提供了一系列创建流程引擎配置类 实例对象的静态() 从而方便客户端获取流程引擎实例对象
 *  而无需关心内部实现细节
 *  疑问:
 *  这个类中  很多 () 都是创建 ProcessEngineConfiguration 对象 并没有提供构造 ProcessEngine 对象的()
 *  因为只要能够 获取  ProcessEngineConfiguration 对象 就可以直接调用该对象的 buildProcessEngine() 创建ProcessEngine实例
 *
 */
public abstract class ProcessEngineConfiguration implements EngineServices {
  
  /** Checks the version of the DB schema against the library when 
   * the process engine is being created and throws an exception
   * if the versions don't match.
   * cn: 当正在创建进程引擎 如果版本不匹配。引发异常
   * */
  public static final String DB_SCHEMA_UPDATE_FALSE = "false";
  
  /** Creates the schema when the process engine is being created and 
   * drops the schema when the process engine is being closed. */
  public static final String DB_SCHEMA_UPDATE_CREATE_DROP = "create-drop";

  /** Upon building of the process engine, a check is performed and 
   * an update of the schema is performed if it is necessary. */
  public static final String DB_SCHEMA_UPDATE_TRUE = "true";
  
  /** The tenant id indicating 'no tenant' */
  public static final String NO_TENANT_ID = "";

  protected String processEngineName = ProcessEngines.NAME_DEFAULT;
  protected int idBlockSize = 2500;

  /*
  * 配置历史
可以选择定制历史存储的配置。你可以通过配置影响引擎的历史功能。 参考历史配置。
  * */
  protected String history = HistoryLevel.AUDIT.getKey();
  /*
  * JobExecutor是管理一系列线程的组件，可以触发定时器（也包含后续的异步消息）。 在单元测试场景下，很难使用多线程。
  * 因此API允许查询(ManagementService.createJobQuery)和执行job (ManagementService.executeJob)，
  * 所以job可以在单元测试中控制。 要避免与job执行器冲突，可以关闭它。
默认，JobExecutor在流程引擎启动时就会激活。 如果不想在流程引擎启动后自动激活JobExecutor，可以设置
  * */
  protected boolean jobExecutorActivate;//
  protected boolean asyncExecutorEnabled;
  protected boolean asyncExecutorActivate;
/*
* 配置邮件服务器
可以选择配置邮件服务器。Activiti支持在业务流程中发送邮件。 想真正的发送一个email，必须配置一个真实的SMTP邮件服务器。 参考e-mail任务。
* */
  protected String mailServerHost = "localhost";
  protected String mailServerUsername; // by default no name and password are provided, which 
  protected String mailServerPassword; // means no authentication for mail server
  protected int mailServerPort = 25;
  protected boolean useSSL = false;
  protected boolean useTLS = false;
  protected String mailServerDefaultFrom = "activiti@localhost";
  protected String mailSessionJndi;
  protected Map<String,MailServerInfo> mailServers = new HashMap<String,MailServerInfo>();
  protected Map<String, String> mailSessionsJndi = new HashMap<String, String>();
  /*
  * 一般不用设置，因为可以自动通过数据库连接的元数据获取。 只有自动检测失败时才需要设置。 可能的值有：{h2, mysql, oracle, postgres, mssql, db2}。 如果没使用默认的H2数据库就必须设置这项。
  *  这个配置会决定使用哪些创建/删除脚本和查询语句。
  * 但是我们应该 直接在配置类表明 这个哪个 DB类型, 因为如果不写, 它会自己进行一次 JDBC连接,得到DatabaseMeta 从而得到DB类型
  * 我们直接写上 她就不用简历连接了,   优化点
  * */
  protected String databaseType;
  protected String databaseSchemaUpdate = DB_SCHEMA_UPDATE_FALSE;
  protected String jdbcDriver = "org.h2.Driver";// DB的驱动类
  protected String jdbcUrl = "jdbc:h2:tcp://localhost/~/activiti";//DB对应的JDBC URL
  protected String jdbcUsername = "sa";//用户名
  protected String jdbcPassword = "";//密码
  /*
  * 默认，Activiti的数据库配置会放在web应用的WEB-INF/classes目录下的db.properties文件中。 这样做比较繁琐，
  * 因为要用户在每次发布时，都修改Activiti源码中的db.properties并重新编译war文件， 或者解压缩war文件，
  * 修改其中的db.properties。
    使用JNDI（Java命名和目录接口）来获取数据库连接， 连接是由servlet容器管理的，
  * 可以在war部署外边管理配置。 与db.properties相比， 它也允许对连接进行更多的配置。
  * */
  protected String dataSourceJndiName = null;//
  protected boolean isDbIdentityUsed = true;
  protected boolean isDbHistoryUsed = true;
  protected HistoryLevel historyLevel;
  protected int jdbcMaxActiveConnections;//连接池中处于被使用状态的连接的最大值 默认为10
  protected int jdbcMaxIdleConnections;//连接池中处于空闲状态的连接的最大值
  protected int jdbcMaxCheckoutTime;//连接被取出使用的最长时间，超过时间会被强制回收。 默认为20000（20秒）。
  protected int jdbcMaxWaitTime;//这是一个底层配置，让连接池可以在长时间无法获得连接时， 打印一条日志，并重新尝试获取一个连接。（避免因为错误配置导致沉默的操作失败）。 默认为20000（20秒）。
  protected boolean jdbcPingEnabled = false;
  protected String jdbcPingQuery = null;
  protected int jdbcPingConnectionNotUsedFor;
  protected int jdbcDefaultTransactionIsolationLevel;
  protected DataSource dataSource;
  protected boolean transactionsExternallyManaged = false;
  
  protected String jpaPersistenceUnitName;
  protected Object jpaEntityManagerFactory;
  protected boolean jpaHandleTransaction;
  protected boolean jpaCloseEntityManager;

  protected Clock clock;
  protected JobExecutor jobExecutor;
  protected AsyncExecutor asyncExecutor;
  
  /** 
   * Define the default lock time for an async job in seconds.
   * The lock time is used when creating an async job and when it expires the async executor
   * assumes that the job has failed. It will be retried again.  
   */
  protected int lockTimeAsyncJobWaitTime = 60;
  /** define the default wait time for a failed job in seconds */
  protected int defaultFailedJobWaitTime = 10;
  /** define the default wait time for a failed async job in seconds */
  protected int asyncFailedJobWaitTime = 10;

  /** process diagram generator. Default value is DefaulProcessDiagramGenerator */
  protected ProcessDiagramGenerator processDiagramGenerator;

  /**
   * Allows configuring a database table prefix which is used for all runtime operations of the process engine.
   * For example, if you specify a prefix named 'PRE1.', activiti will query for executions in a table named
   * 'PRE1.ACT_RU_EXECUTION_'. 
   * 
   * <p />
   * <strong>NOTE: the prefix is not respected by automatic database schema management. If you use 
   * {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_CREATE_DROP} 
   * or {@link ProcessEngineConfiguration#DB_SCHEMA_UPDATE_TRUE}, activiti will create the database tables 
   * using the default names, regardless of the prefix configured here.</strong>  
   * 
   * @since 5.9
   */
  protected String databaseTablePrefix = "";

  /**
   * Escape character for doing wildcard searches.
   * 
   * This will be added at then end of queries that include for example a LIKE clause.
   * For example: SELECT * FROM table WHERE column LIKE '%\%%' ESCAPE '\';
   */
  protected String databaseWildcardEscapeCharacter;
  
  /**
   * database catalog to use
   */
  protected String databaseCatalog = "";

  /**
   * In some situations you want to set the schema to use for table checks / generation if the database metadata
   * doesn't return that correctly, see https://activiti.atlassian.net/browse/ACT-1220,
   * https://activiti.atlassian.net/browse/ACT-1062
   */
  protected String databaseSchema = null;
  
  /**
   * Set to true in case the defined databaseTablePrefix is a schema-name, instead of an actual table name
   * prefix. This is relevant for checking if Activiti-tables exist, the databaseTablePrefix will not be used here
   * - since the schema is taken into account already, adding a prefix for the table-check will result in wrong table-names.
   * 
   *  @since 5.15
   */
  protected boolean tablePrefixIsSchema = false;
  
  protected boolean isCreateDiagramOnDeploy = true;
  
  protected String xmlEncoding = "UTF-8";
  
  protected String defaultCamelContext = "camelContext";
  
  protected String activityFontName = "Arial";
  protected String labelFontName = "Arial";
  protected String annotationFontName = "Arial";
  
  protected ClassLoader classLoader;
  /**
   * Either use Class.forName or ClassLoader.loadClass for class loading.
   * See http://forums.activiti.org/content/reflectutilloadclass-and-custom-classloader
   */
  protected boolean useClassForNameClassLoading = true;
  protected ProcessEngineLifecycleListener processEngineLifecycleListener;
  
  protected boolean enableProcessDefinitionInfoCache = false;

  /** use one of the static createXxxx methods instead */
  protected ProcessEngineConfiguration() {
  }
  /*
  * 此（）用于创建 ProcessEngine 对象  因为activiti.cfg.xml配置文件中定义的流程引擎配置类为
  * StaddaloneProcessEngineConfiguration ,但是该类中并没有定义 buildProcessEngine
  * 那么 此() 肯定在其父类中进行了实现
  * */
  public abstract ProcessEngine buildProcessEngine();
  /*
  该() 直接调用 createProcessEngineConfigurationFromResource
  传入了两个参数   得知
  该方式构造流程引擎配置类实例需要的配置文件名称 必须为 activiti.cfg.xml 并且位于classpath根目录
  流程引擎配置类的id 必须是 processEngineConfiguration
   */
  public static ProcessEngineConfiguration createProcessEngineConfigurationFromResourceDefault() {
    return createProcessEngineConfigurationFromResource("activiti.cfg.xml", "processEngineConfiguration");
  }
  /*
  xml文件名可以自定义
  beanName为 processEngineConfiguration 对应配置文件中 流程引擎配置类bean的id值
   */
  public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource) {
    return createProcessEngineConfigurationFromResource(resource, "processEngineConfiguration");
  }
  /*
    两个 参数都可以自定义
   */
  public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
    return BeansConfigurationHelper.parseProcessEngineConfigurationFromResource(resource, beanName);
  }
  /*
  inputstream 为 配置文件的数据流
  beanName 对应  配置文件中 流程引擎配置类中的bean的id值
   */
  public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream) {
    return createProcessEngineConfigurationFromInputStream(inputStream, "processEngineConfiguration");
  }

  public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
    return BeansConfigurationHelper.parseProcessEngineConfigurationFromInputStream(inputStream, beanName);
  }

  public static ProcessEngineConfiguration createStandaloneProcessEngineConfiguration() {
    return new StandaloneProcessEngineConfiguration();
  }
  /*
  该() 主要用于创建 StandaloneInMemProcessEngineConfiguration 对象
   */
  public static ProcessEngineConfiguration createStandaloneInMemProcessEngineConfiguration() {
    return new StandaloneInMemProcessEngineConfiguration();
  }

// TODO add later when we have test coverage for this
//  public static ProcessEngineConfiguration createJtaProcessEngineConfiguration() {
//    return new JtaProcessEngineConfiguration();
//  }
  

  // getters and setters //////////////////////////////////////////////////////
  
  public String getProcessEngineName() {
    return processEngineName;
  }

  public ProcessEngineConfiguration setProcessEngineName(String processEngineName) {
    this.processEngineName = processEngineName;
    return this;
  }
  
  public int getIdBlockSize() {
    return idBlockSize;
  }
  
  public ProcessEngineConfiguration setIdBlockSize(int idBlockSize) {
    this.idBlockSize = idBlockSize;
    return this;
  }
  
  public String getHistory() {
    return history;
  }
  
  public ProcessEngineConfiguration setHistory(String history) {
    this.history = history;
    return this;
  }
  
  public String getMailServerHost() {
    return mailServerHost;
  }
  
  public ProcessEngineConfiguration setMailServerHost(String mailServerHost) {
    this.mailServerHost = mailServerHost;
    return this;
  }
  
  public String getMailServerUsername() {
    return mailServerUsername;
  }
  
  public ProcessEngineConfiguration setMailServerUsername(String mailServerUsername) {
    this.mailServerUsername = mailServerUsername;
    return this;
  }
  
  public String getMailServerPassword() {
    return mailServerPassword;
  }
  
  public ProcessEngineConfiguration setMailServerPassword(String mailServerPassword) {
    this.mailServerPassword = mailServerPassword;
    return this;
  }

  public String getMailSessionJndi() {
    return mailSessionJndi;
  }
  
  public ProcessEngineConfiguration setMailSessionJndi(String mailSessionJndi) {
    this.mailSessionJndi = mailSessionJndi;
    return this;
  }

  public int getMailServerPort() {
    return mailServerPort;
  }
  
  public ProcessEngineConfiguration setMailServerPort(int mailServerPort) {
    this.mailServerPort = mailServerPort;
    return this;
  }
  
  public boolean getMailServerUseSSL() {
	  return useSSL;
  }
  
  public ProcessEngineConfiguration setMailServerUseSSL(boolean useSSL) {
	  this.useSSL = useSSL;
	  return this;
  }
  
  public boolean getMailServerUseTLS() {
    return useTLS;
  }
  
  public ProcessEngineConfiguration setMailServerUseTLS(boolean useTLS) {
    this.useTLS = useTLS;
    return this;
  }
  
  public String getMailServerDefaultFrom() {
    return mailServerDefaultFrom;
  }
  
  public ProcessEngineConfiguration setMailServerDefaultFrom(String mailServerDefaultFrom) {
    this.mailServerDefaultFrom = mailServerDefaultFrom;
    return this;
  }
  
  public MailServerInfo getMailServer(String tenantId) {
    return mailServers.get(tenantId);
  }
  
  public Map<String, MailServerInfo> getMailServers() {
    return mailServers;
  }
  
  public ProcessEngineConfiguration setMailServers(Map<String, MailServerInfo> mailServers) {
    this.mailServers.putAll(mailServers);
    return this;
  }
  
  public String getMailSessionJndi(String tenantId) {
    return mailSessionsJndi.get(tenantId);
  }
  
  public Map<String, String> getMailSessionsJndi() {
    return mailSessionsJndi;
  }
  
  public ProcessEngineConfiguration setMailSessionsJndi(Map<String, String> mailSessionsJndi) {
    this.mailSessionsJndi.putAll(mailSessionsJndi);
    return this;
  }
  
  public String getDatabaseType() {
    return databaseType;
  }
  
  public ProcessEngineConfiguration setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }

  public String getDatabaseSchemaUpdate() {
    return databaseSchemaUpdate;
  }
  
  public ProcessEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
    this.databaseSchemaUpdate = databaseSchemaUpdate;
    return this;
  }
  
  public DataSource getDataSource() {
    return dataSource;
  }
  
  public ProcessEngineConfiguration setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }
  
  public String getJdbcDriver() {
    return jdbcDriver;
  }
  
  public ProcessEngineConfiguration setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
    return this;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public ProcessEngineConfiguration setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
    return this;
  }
  
  public String getJdbcUsername() {
    return jdbcUsername;
  }
 
  public ProcessEngineConfiguration setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
    return this;
  }
  
  public String getJdbcPassword() {
    return jdbcPassword;
  }
 
  public ProcessEngineConfiguration setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
    return this;
  }
  
  public boolean isTransactionsExternallyManaged() {
    return transactionsExternallyManaged;
  }
  
  public ProcessEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
    this.transactionsExternallyManaged = transactionsExternallyManaged;
    return this;
  }
  
  public HistoryLevel getHistoryLevel() {
    return historyLevel;
  }
  
  public ProcessEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
    this.historyLevel = historyLevel;
    return this;
  }
  
  public boolean isDbIdentityUsed() {
    return isDbIdentityUsed;
  }
  
  public ProcessEngineConfiguration setDbIdentityUsed(boolean isDbIdentityUsed) {
    this.isDbIdentityUsed = isDbIdentityUsed;
    return this;
  }
  
  public boolean isDbHistoryUsed() {
    return isDbHistoryUsed;
  }
  
  public ProcessEngineConfiguration setDbHistoryUsed(boolean isDbHistoryUsed) {
    this.isDbHistoryUsed = isDbHistoryUsed;
    return this;
  }
  
  public int getJdbcMaxActiveConnections() {
    return jdbcMaxActiveConnections;
  }
  
  public ProcessEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
    this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
    return this;
  }
  
  public int getJdbcMaxIdleConnections() {
    return jdbcMaxIdleConnections;
  }
  
  public ProcessEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
    this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
    return this;
  }
  
  public int getJdbcMaxCheckoutTime() {
    return jdbcMaxCheckoutTime;
  }
  
  public ProcessEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
    this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
    return this;
  }
 
  public int getJdbcMaxWaitTime() {
    return jdbcMaxWaitTime;
  }
  
  public ProcessEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
    this.jdbcMaxWaitTime = jdbcMaxWaitTime;
    return this;
  }
  
  public boolean isJdbcPingEnabled() {
    return jdbcPingEnabled;
  }

  public ProcessEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
    this.jdbcPingEnabled = jdbcPingEnabled;
    return this;
  }

  public String getJdbcPingQuery() {
      return jdbcPingQuery;
  }

  public ProcessEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
    this.jdbcPingQuery = jdbcPingQuery;
    return this;
  }

  public int getJdbcPingConnectionNotUsedFor() {
      return jdbcPingConnectionNotUsedFor;
  }

  public ProcessEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
    this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
    return this;
  }

  public int getJdbcDefaultTransactionIsolationLevel() {
    return jdbcDefaultTransactionIsolationLevel;
  }

  public ProcessEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
    this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
    return this;
  }

  public boolean isJobExecutorActivate() {
    return jobExecutorActivate;
  }
  
  public ProcessEngineConfiguration setJobExecutorActivate(boolean jobExecutorActivate) {
    this.jobExecutorActivate = jobExecutorActivate;
    return this;
  }
  
  public boolean isAsyncExecutorEnabled() {
    return asyncExecutorEnabled;
  }

  public ProcessEngineConfiguration setAsyncExecutorEnabled(boolean asyncExecutorEnabled) {
    this.asyncExecutorEnabled = asyncExecutorEnabled;
    return this;
  }

  public boolean isAsyncExecutorActivate() {
    return asyncExecutorActivate;
  }
  
  public ProcessEngineConfiguration setAsyncExecutorActivate(boolean asyncExecutorActivate) {
    this.asyncExecutorActivate = asyncExecutorActivate;
    return this;
  }
  
  public ClassLoader getClassLoader() {
    return classLoader;
  }
  
  public ProcessEngineConfiguration setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public boolean isUseClassForNameClassLoading() {
    return useClassForNameClassLoading;
  }

  public ProcessEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
    this.useClassForNameClassLoading = useClassForNameClassLoading;
    return this;
  }

  public Object getJpaEntityManagerFactory() {
    return jpaEntityManagerFactory;
  }

  public ProcessEngineConfiguration setJpaEntityManagerFactory(Object jpaEntityManagerFactory) {
    this.jpaEntityManagerFactory = jpaEntityManagerFactory;
    return this;
  }

  public boolean isJpaHandleTransaction() {
    return jpaHandleTransaction;
  }

  public ProcessEngineConfiguration setJpaHandleTransaction(boolean jpaHandleTransaction) {
    this.jpaHandleTransaction = jpaHandleTransaction;
    return this;
  }
  
  public boolean isJpaCloseEntityManager() {
    return jpaCloseEntityManager;
  }

  public ProcessEngineConfiguration setJpaCloseEntityManager(boolean jpaCloseEntityManager) {
    this.jpaCloseEntityManager = jpaCloseEntityManager;
    return this;
  }

  public String getJpaPersistenceUnitName() {
    return jpaPersistenceUnitName;
  }

  public ProcessEngineConfiguration setJpaPersistenceUnitName(String jpaPersistenceUnitName) {
    this.jpaPersistenceUnitName = jpaPersistenceUnitName;
    return this;
  }

  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  public ProcessEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
    this.dataSourceJndiName = dataSourceJndiName;
    return this;
  }

  public String getDefaultCamelContext() {
    return defaultCamelContext;
  }
  
  public ProcessEngineConfiguration setDefaultCamelContext(String defaultCamelContext) {
    this.defaultCamelContext = defaultCamelContext;
    return this;
  }
  
  public boolean isCreateDiagramOnDeploy() {
    return isCreateDiagramOnDeploy;
  }

  public ProcessEngineConfiguration setCreateDiagramOnDeploy(boolean createDiagramOnDeploy) {
    this.isCreateDiagramOnDeploy = createDiagramOnDeploy;
    return this;
  }

  public String getActivityFontName() {
    return activityFontName;
  }

  public ProcessEngineConfiguration setActivityFontName(String activityFontName) {
    this.activityFontName = activityFontName;
    return this;
  }
  
  public ProcessEngineConfiguration setProcessEngineLifecycleListener(ProcessEngineLifecycleListener processEngineLifecycleListener) {
    this.processEngineLifecycleListener = processEngineLifecycleListener;
    return this;
  }
  
  public ProcessEngineLifecycleListener getProcessEngineLifecycleListener() {
    return processEngineLifecycleListener;
  }

  public String getLabelFontName() {
    return labelFontName;
  }

  public ProcessEngineConfiguration setLabelFontName(String labelFontName) {
    this.labelFontName = labelFontName;
    return this;
  }
  
  public String getAnnotationFontName() {
	  return annotationFontName;
  }
  
  public ProcessEngineConfiguration setAnnotationFontName(String annotationFontName) {
	  this.annotationFontName = annotationFontName;
	  return this;
  }
    
  public String getDatabaseTablePrefix() {
    return databaseTablePrefix;
  }
  
  public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
    this.databaseTablePrefix = databaseTablePrefix;
    return this;
  }
  
  public ProcessEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
	  this.tablePrefixIsSchema = tablePrefixIsSchema;
	  return this;
  }
  
  public boolean isTablePrefixIsSchema() {
	  return tablePrefixIsSchema;
  }

  public String getDatabaseWildcardEscapeCharacter() {
    return databaseWildcardEscapeCharacter;
  }

  public ProcessEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
    this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
    return this;
  }

  public String getDatabaseCatalog() {
    return databaseCatalog;
  }

  public ProcessEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
    this.databaseCatalog = databaseCatalog;
    return this;
  }

  public String getDatabaseSchema() {
    return databaseSchema;
  }
  
  public ProcessEngineConfiguration setDatabaseSchema(String databaseSchema) {
    this.databaseSchema = databaseSchema;
    return this;
  }
  
  public String getXmlEncoding() {
    return xmlEncoding;
  }

  public ProcessEngineConfiguration setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
    return this;
  }

  public Clock getClock() {
    return clock;
  }

  public ProcessEngineConfiguration setClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public ProcessDiagramGenerator getProcessDiagramGenerator() {
    return this.processDiagramGenerator;
  }

  public ProcessEngineConfiguration setProcessDiagramGenerator(ProcessDiagramGenerator processDiagramGenerator) {
    this.processDiagramGenerator = processDiagramGenerator;
    return this;
  }

  public JobExecutor getJobExecutor() {
    return jobExecutor;
  }
  
  public ProcessEngineConfiguration setJobExecutor(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
    return this;
  }
  
  public AsyncExecutor getAsyncExecutor() {
    return asyncExecutor;
  }
  
  public ProcessEngineConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
    this.asyncExecutor = asyncExecutor;
    return this;
  }

  public int getLockTimeAsyncJobWaitTime() {
    return lockTimeAsyncJobWaitTime;
  }

  public ProcessEngineConfiguration setLockTimeAsyncJobWaitTime(int lockTimeAsyncJobWaitTime) {
    this.lockTimeAsyncJobWaitTime = lockTimeAsyncJobWaitTime;
    return this;
  }

  public int getDefaultFailedJobWaitTime() {
    return defaultFailedJobWaitTime;
  }

  public ProcessEngineConfiguration setDefaultFailedJobWaitTime(int defaultFailedJobWaitTime) {
    this.defaultFailedJobWaitTime = defaultFailedJobWaitTime;
    return this;
  }

  public int getAsyncFailedJobWaitTime() {
    return asyncFailedJobWaitTime;
  }

  public ProcessEngineConfiguration setAsyncFailedJobWaitTime(int asyncFailedJobWaitTime) {
    this.asyncFailedJobWaitTime = asyncFailedJobWaitTime;
    return this;
  }

  public boolean isEnableProcessDefinitionInfoCache() {
    return enableProcessDefinitionInfoCache;
  }

  public ProcessEngineConfiguration setEnableProcessDefinitionInfoCache(boolean enableProcessDefinitionInfoCache) {
    this.enableProcessDefinitionInfoCache = enableProcessDefinitionInfoCache;
    return this;
  }
}
