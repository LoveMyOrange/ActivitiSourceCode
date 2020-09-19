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

package org.activiti.engine.impl.cfg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.cfg.ProcessEngineConfigurator;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventDispatcherImpl;
import org.activiti.engine.form.AbstractFormType;
import org.activiti.engine.impl.DynamicBpmnServiceImpl;
import org.activiti.engine.impl.FormServiceImpl;
import org.activiti.engine.impl.HistoryServiceImpl;
import org.activiti.engine.impl.IdentityServiceImpl;
import org.activiti.engine.impl.ManagementServiceImpl;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.RuntimeServiceImpl;
import org.activiti.engine.impl.ServiceImpl;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.asyncexecutor.ExecuteAsyncRunnableFactory;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.bpmn.deployer.BpmnDeployer;
import org.activiti.engine.impl.bpmn.parser.BpmnParseHandlers;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.bpmn.parser.factory.AbstractBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.activiti.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.activiti.engine.impl.bpmn.parser.handler.BoundaryEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.BusinessRuleParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CallActivityParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CancelEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.CompensateEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EndEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ErrorEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EventBasedGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.EventSubProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ExclusiveGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.InclusiveGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.IntermediateCatchEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.IntermediateThrowEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ManualTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.MessageEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ParallelGatewayParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ReceiveTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ScriptTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SendTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SequenceFlowParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SignalEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.StartEventParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.SubProcessParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TaskParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TimerEventDefinitionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.TransactionParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.UserTaskParseHandler;
import org.activiti.engine.impl.bpmn.webservice.MessageInstance;
import org.activiti.engine.impl.calendar.BusinessCalendarManager;
import org.activiti.engine.impl.calendar.CycleBusinessCalendar;
import org.activiti.engine.impl.calendar.DueDateBusinessCalendar;
import org.activiti.engine.impl.calendar.DurationBusinessCalendar;
import org.activiti.engine.impl.calendar.MapBusinessCalendarManager;
import org.activiti.engine.impl.cfg.standalone.StandaloneMybatisTransactionContextFactory;
import org.activiti.engine.impl.db.DbIdGenerator;
import org.activiti.engine.impl.db.DbSqlSessionFactory;
import org.activiti.engine.impl.db.IbatisVariableTypeHandler;
import org.activiti.engine.impl.delegate.DefaultDelegateInterceptor;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.event.CompensationEventHandler;
import org.activiti.engine.impl.event.EventHandler;
import org.activiti.engine.impl.event.MessageEventHandler;
import org.activiti.engine.impl.event.SignalEventHandler;
import org.activiti.engine.impl.event.logger.EventLogger;
import org.activiti.engine.impl.form.BooleanFormType;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.DoubleFormType;
import org.activiti.engine.impl.form.FormEngine;
import org.activiti.engine.impl.form.FormTypes;
import org.activiti.engine.impl.form.JuelFormEngine;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.history.parse.FlowNodeHistoryParseHandler;
import org.activiti.engine.impl.history.parse.ProcessHistoryParseHandler;
import org.activiti.engine.impl.history.parse.StartEventHistoryParseHandler;
import org.activiti.engine.impl.history.parse.UserTaskHistoryParseHandler;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.CommandContextFactory;
import org.activiti.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.interceptor.CommandInvoker;
import org.activiti.engine.impl.interceptor.DelegateInterceptor;
import org.activiti.engine.impl.interceptor.LogInterceptor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.activiti.engine.impl.jobexecutor.CallerRunsRejectedJobsHandler;
import org.activiti.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.DefaultJobExecutor;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.activiti.engine.impl.jobexecutor.RejectedJobsHandler;
import org.activiti.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.activiti.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.activiti.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.activiti.engine.impl.persistence.DefaultHistoryManagerSessionFactory;
import org.activiti.engine.impl.persistence.GenericManagerFactory;
import org.activiti.engine.impl.persistence.GroupEntityManagerFactory;
import org.activiti.engine.impl.persistence.MembershipEntityManagerFactory;
import org.activiti.engine.impl.persistence.UserEntityManagerFactory;
import org.activiti.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.deploy.ProcessDefinitionInfoCache;
import org.activiti.engine.impl.persistence.entity.AttachmentEntityManager;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.engine.impl.persistence.entity.CommentEntityManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityManager;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.persistence.entity.ModelEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.persistence.entity.TableDataManager;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.activiti.engine.impl.scripting.BeansResolverFactory;
import org.activiti.engine.impl.scripting.ResolverFactory;
import org.activiti.engine.impl.scripting.ScriptBindingsFactory;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.impl.scripting.VariableScopeResolverFactory;
import org.activiti.engine.impl.util.DefaultClockImpl;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.impl.variable.BooleanType;
import org.activiti.engine.impl.variable.ByteArrayType;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.activiti.engine.impl.variable.DateType;
import org.activiti.engine.impl.variable.DefaultVariableTypes;
import org.activiti.engine.impl.variable.DoubleType;
import org.activiti.engine.impl.variable.EntityManagerSession;
import org.activiti.engine.impl.variable.EntityManagerSessionFactory;
import org.activiti.engine.impl.variable.IntegerType;
import org.activiti.engine.impl.variable.JPAEntityListVariableType;
import org.activiti.engine.impl.variable.JPAEntityVariableType;
import org.activiti.engine.impl.variable.JsonType;
import org.activiti.engine.impl.variable.LongJsonType;
import org.activiti.engine.impl.variable.LongStringType;
import org.activiti.engine.impl.variable.LongType;
import org.activiti.engine.impl.variable.NullType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.engine.impl.variable.ShortType;
import org.activiti.engine.impl.variable.StringType;
import org.activiti.engine.impl.variable.UUIDType;
import org.activiti.engine.impl.variable.VariableType;
import org.activiti.engine.impl.variable.VariableTypes;
import org.activiti.engine.parse.BpmnParseHandler;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ProcessValidatorFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @desc
 *  该抽象类继承 ProcessEngineConfiguration  负责创建一系列服务类实例对象,  流程引擎实例对象
 *  以及ProcessEngineImpl对象
 *  该类可以通过 流程引擎配置文件 交给Spring容器管理 或者使用编程方式动态构造
 *
 *  //
 *
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration {  

  private static Logger log = LoggerFactory.getLogger(ProcessEngineConfigurationImpl.class);
  
  public static final int DEFAULT_GENERIC_MAX_LENGTH_STRING= 4000;
  public static final int DEFAULT_ORACLE_MAX_LENGTH_STRING= 2000;

  public static final String DB_SCHEMA_UPDATE_CREATE = "create";
  public static final String DB_SCHEMA_UPDATE_DROP_CREATE = "drop-create";

  public static final String DEFAULT_WS_SYNC_FACTORY = "org.activiti.engine.impl.webservice.CxfWebServiceClientFactory";
  
  public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/activiti/db/mapping/mappings.xml";

  // SERVICES /////////////////////////////////////////////////////////////////
  /*
   通常是根据EngineServices实例对象获取以上这些服务类的实例对象, 那么
   为什么这些服务类的实例化工作 在ProcessEngineConfigurationImpl类中进行呢???

   */
  protected RepositoryService repositoryService = new RepositoryServiceImpl();
  protected RuntimeService runtimeService = new RuntimeServiceImpl();
  protected HistoryService historyService = new HistoryServiceImpl(this);
  protected IdentityService identityService = new IdentityServiceImpl();
  protected TaskService taskService = new TaskServiceImpl(this);
  protected FormService formService = new FormServiceImpl();
  protected ManagementService managementService = new ManagementServiceImpl();
  protected DynamicBpmnService dynamicBpmnService = new DynamicBpmnServiceImpl(this);
  
  // COMMAND EXECUTORS ////////////////////////////////////////////////////////
  
  protected CommandConfig defaultCommandConfig;
  protected CommandConfig schemaCommandConfig;
  
  protected CommandInterceptor commandInvoker;
  
  /** the configurable list which will be {@link #initInterceptorChain(java.util.List) processed} to build the {@link #commandExecutor} */
  protected List<CommandInterceptor> customPreCommandInterceptors;
  protected List<CommandInterceptor> customPostCommandInterceptors;
  
  protected List<CommandInterceptor> commandInterceptors;

  /** this will be initialized during the configurationComplete() */
  protected CommandExecutor commandExecutor;
  
  // SESSION FACTORIES ////////////////////////////////////////////////////////

  protected List<SessionFactory> customSessionFactories;
  protected DbSqlSessionFactory dbSqlSessionFactory;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  
  // Configurators ////////////////////////////////////////////////////////////
  
  protected boolean enableConfiguratorServiceLoader = true; // serviceLoader  SPI机制  默认值 true
  protected List<ProcessEngineConfigurator> configurators; // Activiti的配置器 开关属性
  protected List<ProcessEngineConfigurator> allConfigurators; // 配置器集合
  
  // DEPLOYERS ////////////////////////////////////////////////////////////////

  protected BpmnDeployer bpmnDeployer;
  protected BpmnParser bpmnParser;
  protected List<Deployer> customPreDeployers;
  protected List<Deployer> customPostDeployers;
  protected List<Deployer> deployers;
  protected DeploymentManager deploymentManager;
  /*
  * 所有流程定义都被缓存了（解析之后）避免每次使用前都要访问数据库， 因为流程定义数据是不会改变的。 默认，不会限制这个缓存。如果想限制流程定义缓存，可以添加如下配置
  这个配置会把默认的hashmap缓存替换成LRU缓存，来提供限制。 当然，这个配置的最佳值跟流程定义的总数有关， 实际使用中会具体使用多少流程定义也有关。
  也你可以注入自己的缓存实现。这个bean必须实现 org.activiti.engine.impl.persistence.deploy.DeploymentCache接口：
  * <property name="processDefinitionCache">
  <bean class="org.activiti.MyCache" />
</property>
有一个类似的配置叫knowledgeBaseCacheLimit和knowledgeBaseCache， 它们是配置规则缓存的。只有流程中使用规则任务时才会用到。
   * */
  protected int processDefinitionCacheLimit = -1; // By default, no limit
  protected DeploymentCache<ProcessDefinitionEntity> processDefinitionCache;
  protected int bpmnModelCacheLimit = -1; // By default, no limit
  protected DeploymentCache<BpmnModel> bpmnModelCache;
  protected int processDefinitionInfoCacheLimit = -1; // By default, no limit
  protected ProcessDefinitionInfoCache processDefinitionInfoCache;
  
  protected int knowledgeBaseCacheLimit = -1;
  protected DeploymentCache<Object> knowledgeBaseCache;

  // JOB EXECUTOR /////////////////////////////////////////////////////////////
  
  protected List<JobHandler> customJobHandlers;
  protected Map<String, JobHandler> jobHandlers;
  
  // ASYNC EXECUTOR ///////////////////////////////////////////////////////////
  
  /**
   * The minimal number of threads that are kept alive in the threadpool for job execution. Default value = 2.
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorCorePoolSize = 2;
  
  /**
   * The maximum number of threads that are kept alive in the threadpool for job execution. Default value = 10.
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorMaxPoolSize = 10;
  
  /** 
   * The time (in milliseconds) a thread used for job execution must be kept alive before it is
   * destroyed. Default setting is 5 seconds. Having a setting > 0 takes resources,
   * but in the case of many job executions it avoids creating new threads all the time.
   * If 0, threads will be destroyed after they've been used for job execution. 
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected long asyncExecutorThreadKeepAliveTime = 5000L;
  
	/** 
	 * The size of the queue on which jobs to be executed are placed, before they are actually executed. Default value = 100.
	 * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
	 */
  protected int asyncExecutorThreadPoolQueueSize = 100;
  
  /** 
   * The queue onto which jobs will be placed before they are actually executed.
   * Threads form the async executor threadpool will take work from this queue.
   * 
   * By default null. If null, an {@link ArrayBlockingQueue} will be created of size {@link #asyncExecutorThreadPoolQueueSize}.
   * 
   * When the queue is full, the job will be executed by the calling thread (ThreadPoolExecutor.CallerRunsPolicy())
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected BlockingQueue<Runnable> asyncExecutorThreadPoolQueue;
  
  /** 
   * The time (in seconds) that is waited to gracefully shut down the threadpool used for job execution
   * when the a shutdown on the executor (or process engine) is requested. Default value = 60.
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected long asyncExecutorSecondsToWaitOnShutdown = 60L;
  
  /**
   * The number of timer jobs that are acquired during one query (before a job is executed, an acquirement thread 
   * fetches jobs from the database and puts them on the queue). 
   * 
   * Default value = 1, as this lowers the potential on optimistic locking exceptions. 
   * Change this value if you know what you are doing.
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorMaxTimerJobsPerAcquisition = 1;
  
  /**
   * The number of async jobs that are acquired during one query (before a job is executed, an acquirement thread 
   * fetches jobs from the database and puts them on the queue). 
   * 
   * Default value = 1, as this lowers the potential on optimistic locking exceptions. 
   * Change this value if you know what you are doing.
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorMaxAsyncJobsDuePerAcquisition = 1;
  
  /**
   * The time (in milliseconds) the timer acquisition thread will wait to execute the next acquirement query.
   * This happens when no new timer jobs were found or when less timer jobs have been fetched 
   * than set in {@link #asyncExecutorMaxTimerJobsPerAcquisition}. Default value = 10 seconds. 
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorDefaultTimerJobAcquireWaitTime = 10 * 1000;
  
  /**
   * The time (in milliseconds) the async job acquisition thread will wait to execute the next acquirement query.
   * This happens when no new async jobs were found or when less async jobs have been fetched 
   * than set in {@link #asyncExecutorMaxAsyncJobsDuePerAcquisition}. Default value = 10 seconds. 
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorDefaultAsyncJobAcquireWaitTime = 10 * 1000;
  
  /**
   * The time (in milliseconds) the async job (both timer and async continuations) acquisition thread will 
   * wait when the queueu is full to execute the next query. By default set to 0 (for backwards compatibility)
   */
  protected int asyncExecutorDefaultQueueSizeFullWaitTime = 0;
  
  /**
   * When a job is acquired, it is locked so other async executors can't lock and execute it.
   * While doing this, the 'name' of the lock owner is written into a column of the job.
   * 
   * By default, a random UUID will be generated when the executor is created.
   * 
   * It is important that each async executor instance in a cluster of Activiti engines
   * has a different name!
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected String asyncExecutorLockOwner;
  
  /**
   * The amount of time (in milliseconds) a timer job is locked when acquired by the async executor.
   * During this period of time, no other async executor will try to acquire and lock this job.
   * 
   * Default value = 5 minutes;
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorTimerLockTimeInMillis = 5 * 60 * 1000;
  
  /**
   * The amount of time (in milliseconds) an async job is locked when acquired by the async executor.
   * During this period of time, no other async executor will try to acquire and lock this job.
   * 
   * Default value = 5 minutes;
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorAsyncJobLockTimeInMillis = 5 * 60 * 1000;
  
  /**
   * The amount of time (in milliseconds) that is waited before trying locking again,
   * when an exclusive job is tried to be locked, but fails and the locking.
   * 
   * Default value = 500. If 0, this would stress database traffic a lot in case when a retry is needed,
   * as exclusive jobs would be constantly tried to be locked.
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected int asyncExecutorLockRetryWaitTimeInMillis = 500;
  
  /**
   * Allows to define a custom factory for creating the {@link Runnable} that is executed by the async executor.
   * 
   * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
   */
  protected ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory;

  // MYBATIS SQL SESSION FACTORY //////////////////////////////////////////////
  
  protected SqlSessionFactory sqlSessionFactory;
  protected TransactionFactory transactionFactory;
  
  protected Set<Class<?>> customMybatisMappers;
  protected Set<String> customMybatisXMLMappers;

  // ID GENERATOR /////////////////////////////////////////////////////////////
  
  protected IdGenerator idGenerator;
  protected DataSource idGeneratorDataSource;
  protected String idGeneratorDataSourceJndiName;
  
  // BPMN PARSER //////////////////////////////////////////////////////////////
  
  protected List<BpmnParseHandler> preBpmnParseHandlers;
  protected List<BpmnParseHandler> postBpmnParseHandlers;
  protected List<BpmnParseHandler> customDefaultBpmnParseHandlers;
  protected ActivityBehaviorFactory activityBehaviorFactory;
  protected ListenerFactory listenerFactory;
  protected BpmnParseFactory bpmnParseFactory;
  
  // PROCESS VALIDATION 
  
  protected ProcessValidator processValidator;

  // OTHER ////////////////////////////////////////////////////////////////////
  
  protected List<FormEngine> customFormEngines;
  protected Map<String, FormEngine> formEngines;

  protected List<AbstractFormType> customFormTypes;
  protected FormTypes formTypes;

  protected List<VariableType> customPreVariableTypes;
  protected List<VariableType> customPostVariableTypes;
  protected VariableTypes variableTypes;
  
  protected ExpressionManager expressionManager;
  protected List<String> customScriptingEngineClasses;
  protected ScriptingEngines scriptingEngines;
  protected List<ResolverFactory> resolverFactories;
  
  protected BusinessCalendarManager businessCalendarManager;
  
  protected int executionQueryLimit = 20000;
  protected int taskQueryLimit = 20000;
  protected int historicTaskQueryLimit = 20000;
  protected int historicProcessInstancesQueryLimit = 20000;

  protected String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;
  protected ConcurrentMap<QName, URL> wsOverridenEndpointAddresses = new ConcurrentHashMap<QName, URL>();

  protected CommandContextFactory commandContextFactory;
  protected TransactionContextFactory transactionContextFactory;
  
  protected Map<Object, Object> beans;
  
  protected DelegateInterceptor delegateInterceptor;

  protected RejectedJobsHandler customRejectedJobsHandler;
  
  protected Map<String, EventHandler> eventHandlers;
  protected List<EventHandler> customEventHandlers;

  protected FailedJobCommandFactory failedJobCommandFactory;
  
  /**
   * Set this to true if you want to have extra checks on the BPMN xml that is parsed.
   * See http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml-can-bring-down-your-server/
   * 
   * Unfortunately, this feature is not available on some platforms (JDK 6, JBoss),
   * hence the reason why it is disabled by default. If your platform allows 
   * the use of StaxSource during XML parsing, do enable it.
   */
  protected boolean enableSafeBpmnXml = false;
  
  /**
   * The following settings will determine the amount of entities loaded at once when the engine 
   * needs to load multiple entities (eg. when suspending a process definition with all its process instances).
   * 
   * The default setting is quite low, as not to surprise anyone with sudden memory spikes.
   * Change it to something higher if the environment Activiti runs in allows it.
   */
  protected int batchSizeProcessInstances = 25;
  protected int batchSizeTasks = 25;
  
  /**
   * If set to true, enables bulk insert (grouping sql inserts together).
   * Default true. For some databases (eg DB2 on Zos: https://activiti.atlassian.net/browse/ACT-4042) needs to be set to false
   */
  protected boolean isBulkInsertEnabled = true;
  
  /**
  * Some databases have a limit of how many parameters one sql insert can have (eg SQL Server, 2000 params (!= insert statements) ).
  * Tweak this parameter in case of exceptions indicating too much is being put into one bulk insert,
  * or make it higher if your database can cope with it and there are inserts with a huge amount of data.
  * 
  * By default: 100.
  */
  protected int maxNrOfStatementsInBulkInsert = 100;
  
  protected boolean enableEventDispatcher = true;
  protected ActivitiEventDispatcher eventDispatcher;
  protected List<ActivitiEventListener> eventListeners;//全局事件监听器
  protected Map<String, List<ActivitiEventListener>> typedEventListeners;//具体类型的事件监听器
  
  // Event logging to database
  protected boolean enableDatabaseEventLogging = false;
  
  /**
   * Using field injection together with a delegate expression for a service
   * task / execution listener / task listener is not thread-sade , see user
   * guide section 'Field Injection' for more information.
   * 
   * Set this flag to false to throw an exception at runtime when a field is
   * injected and a delegateExpression is used. Default is true for backwards compatibility.
   * 
   * @since 5.21
   */
  protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.COMPATIBILITY;
  
  /**
   *  Define a max length for storing String variable types in the database.
   *  Mainly used for the Oracle NVARCHAR2 limit of 2000 characters
   */
  protected int maxLengthStringVariableType = -1;
  
  protected ObjectMapper objectMapper = new ObjectMapper();
  
  // buildProcessEngine ///////////////////////////////////////////////////////
  /*
  该() 处理逻辑 如下
  1)调用init() 初始化 ProcessEngineConfigurationImpl对象的 各种属性值
  2) 实例化ProcessEngineImpl类
   */
  public ProcessEngine buildProcessEngine() {
    init();
    return new ProcessEngineImpl(this);
  }
  
  // init /////////////////////////////////////////////////////////////////////
  
  protected void init() {
  	initConfigurators(); //初始化配置器
  	configuratorsBeforeInit();//调用配置器的beforeinit()
    initProcessDiagramGenerator();//初始化流程图片生成器
    initHistoryLevel();//初始化历史记录归档级别
    initExpressionManager();//初始化表达式管理器
    initDataSource();//初始化数据源
    initVariableTypes();//初始化变量类型
    initBeans();//初始化可以管理bean
    initFormEngines();//初始化表单引擎
    initFormTypes();//初始化表单类型,
    initScriptingEngines();//初始化脚本引擎
    initClock();//初始化时间类 ,主要负责提供设置当前时间等
    initBusinessCalendarManager();//初始化日期管理器
    initCommandContextFactory();//初始化命令上下文工厂
    initTransactionContextFactory();//事务上下文工厂
    initCommandExecutors();//命令执行器
    initServices();//为各种服务类 比如respositoryService 设置命令执行器
    initIdGenerator();//初始化id生成器
    initDeployers();//部署器
    initJobHandlers();//定时处理器
    initJobExecutor();//定时任务执行器
    initAsyncExecutor();//异步执行器
    initTransactionFactory();//事务工厂
    initSqlSessionFactory();//SqlSession工厂
    initSessionFactories();//session工厂
    initJpa();//JPA
    initDelegateInterceptor();//负责处理器拦截器默认实现类(拦截监听器或者表达式
    initEventHandlers();//事件处理类
    initFailedJobCommandFactory();//失败命令工厂
    initEventDispatcher();//事件转发器
    initProcessValidator();//BPMNMOdel 校验器
    initDatabaseEventLogging();//数据库事件记录
    configuratorsAfterInit();//调用配置器的configure()
  }

  // failedJobCommandFactory ////////////////////////////////////////////////////////
    /*
    * 失败命令工厂
    * DefaultFailedJobCommandFactory 用于获取 JobRetryCmd命令类
    * */
  protected void initFailedJobCommandFactory() {
    if (failedJobCommandFactory == null) {
      failedJobCommandFactory = new DefaultFailedJobCommandFactory();
    }
  }

  // command executors ////////////////////////////////////////////////////////
  /*
  *
  * */
  protected void initCommandExecutors() {
    initDefaultCommandConfig();//默认命令配置信息
    initSchemaCommandConfig();//Schema命令配置信息
    initCommandInvoker();//命令调用者
    initCommandInterceptors();//命令拦截器
    initCommandExecutor();//命令执行器
  }

  protected void initDefaultCommandConfig() {
    if (defaultCommandConfig==null) {
      defaultCommandConfig = new CommandConfig();
    }
  }

  private void initSchemaCommandConfig() {
    if (schemaCommandConfig==null) {
      //同时调用 transactionNotSupported()  事务传播
      schemaCommandConfig = new CommandConfig().transactionNotSupported();
    }
  }
/*
 初始化命令调度者

* */
  protected void initCommandInvoker() {
    if (commandInvoker==null) {
      commandInvoker = new CommandInvoker();
    }
  }
  /*
  * 初始化命令拦截器
  * */
  protected void initCommandInterceptors() {
    if (commandInterceptors==null) {
      commandInterceptors = new ArrayList<CommandInterceptor>();
      if (customPreCommandInterceptors!=null) {
        commandInterceptors.addAll(customPreCommandInterceptors);
      }
      commandInterceptors.addAll(getDefaultCommandInterceptors());
      if (customPostCommandInterceptors!=null) {
        commandInterceptors.addAll(customPostCommandInterceptors);
      }
      //添加命令调用拦截器   commandInvoker 永远是拦截器链中的最后一个执行节点, 负责调度执行具体的命令类
      commandInterceptors.add(commandInvoker);
    }
  }
/*
* 获取默认的命令拦截器
* */
  protected Collection< ? extends CommandInterceptor> getDefaultCommandInterceptors() {
    List<CommandInterceptor> interceptors = new ArrayList<CommandInterceptor>();
    //1) 添加日志拦截器
    interceptors.add(new LogInterceptor());
    //2)获取事务拦截器,
    CommandInterceptor transactionInterceptor = createTransactionInterceptor();
    //
    if (transactionInterceptor != null) {
      interceptors.add(transactionInterceptor);
    }
    // 初始化命令上下文拦截器, 并将其添加到集合 ,
    interceptors.add(new CommandContextInterceptor(commandContextFactory, this));
    return interceptors;
  }
  /*
  * 命令拦截器初始化步骤
  * */
  protected void initCommandExecutor() {
    if (commandExecutor==null) { //
      //构造命令拦截器链
      CommandInterceptor first = initInterceptorChain(commandInterceptors);
      /*
      实例化命令拦截器
      根据命令配置对象 以及命令拦截器链中的第一个节点 实例化
      该类负责全局统筹命令拦截器的调用工作
       */
      commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
    }
  }
/*
*  将一系列的命令拦截器组装成链, 并返回链中的开始节点, 方便后续程序自上而下执行命令拦截器
* */
  protected CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
    if (chain==null || chain.isEmpty()) { //非空校验
      //因为 如果命令拦截器集合为空 则必要的命令拦截器肯定缺失 ,不完整的命令拦截器链式无法使用的
      throw new ActivitiException("invalid command interceptor chain configuration: "+chain);
    }
    for (int i = 0; i < chain.size()-1; i++) {
      //循环链中的 chain参数结构为LinkedLIst,    有序的, 所以最终构造的命令拦截器链的节点熟悉怒 和节点 在chain
//      集合中的顺序是一致的, 命令拦截器链构造完毕之后, 第14行 直接从 chain集合中取出第一个元素作为该() 返回值
      chain.get(i).setNext( chain.get(i+1) );
    }
    return chain.get(0);
  }
  
  protected abstract CommandInterceptor createTransactionInterceptor();
  
  // services /////////////////////////////////////////////////////////////////
  /*
  *  如何执行Activiti中的命令呢 ???
  *  因为平时都是基于Activiti暴露的API 对流程实例进行操作 ,
  * 所以分析一下 实例化服务类的同时 是否为其填充了 commandExecutor属性值
  * */
  protected void initServices() {
    //这一系列的服务类实例对象 都是通过 commadnExecutor 对象 作为属性值 注入进去的
    initService(repositoryService);
    initService(runtimeService);
    initService(historyService);
    initService(identityService);
    initService(taskService);
    initService(formService);
    initService(managementService);
    initService(dynamicBpmnService);
  }

  protected void initService(Object service) {
    //首先判断 service 对象是否是ServiceImpl 对象, 如果是 则执行后续操作
    if (service instanceof ServiceImpl) {
      //所以可以很方便的通过 任意一个服务类 对象获取CommandExecutor 对象
      ((ServiceImpl)service).setCommandExecutor(commandExecutor);
    }
  }
  
  // DataSource ///////////////////////////////////////////////////////////////
  /*
  * 初始化数据源
  * */
  protected void initDataSource() {
    if (dataSource==null) { //开关属性值
      if (dataSourceJndiName!=null) {//判空
        //JNDI方式配置数据源
        try {
          dataSource = (DataSource) new InitialContext().lookup(dataSourceJndiName);
        } catch (Exception e) {
          throw new ActivitiException("couldn't lookup datasource from "+dataSourceJndiName+": "+e.getMessage(), e);
        }
        
      }
      //判断URL
      else if (jdbcUrl!=null) {
        //判断 驱动类 和用户名 是否为null
        if ( (jdbcDriver==null) || (jdbcUrl==null) || (jdbcUsername==null) ) {
          throw new ActivitiException("DataSource or JDBC properties have to be specified in a process engine configuration");
        }
        
        log.debug("initializing datasource to db: {}", jdbcUrl);
        //实例化  并且填充属性
        PooledDataSource pooledDataSource = 
          new PooledDataSource(ReflectUtil.getClassLoader(), jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword );
        
        if (jdbcMaxActiveConnections > 0) {
          pooledDataSource.setPoolMaximumActiveConnections(jdbcMaxActiveConnections);
        }
        if (jdbcMaxIdleConnections > 0) {
          pooledDataSource.setPoolMaximumIdleConnections(jdbcMaxIdleConnections);
        }
        if (jdbcMaxCheckoutTime > 0) {
          pooledDataSource.setPoolMaximumCheckoutTime(jdbcMaxCheckoutTime);
        }
        if (jdbcMaxWaitTime > 0) {
          pooledDataSource.setPoolTimeToWait(jdbcMaxWaitTime);
        }
        if (jdbcPingEnabled == true) {
          pooledDataSource.setPoolPingEnabled(true);
          if (jdbcPingQuery != null) {
            pooledDataSource.setPoolPingQuery(jdbcPingQuery);
          }
          pooledDataSource.setPoolPingConnectionsNotUsedFor(jdbcPingConnectionNotUsedFor);
        }
        if (jdbcDefaultTransactionIsolationLevel > 0) {
          pooledDataSource.setDefaultTransactionIsolationLevel(jdbcDefaultTransactionIsolationLevel);
        }
        dataSource = pooledDataSource;
      }
      
      if (dataSource instanceof PooledDataSource) {
        // ACT-233: connection pool of Ibatis is not properely initialized if this is not called!
        ((PooledDataSource)dataSource).forceCloseAll();
      }
    }

    if (databaseType == null) {
      initDatabaseType();// 初始化DB类型
    }
  }
  
  protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();
  
  public static final String DATABASE_TYPE_H2 = "h2";
  public static final String DATABASE_TYPE_HSQL = "hsql";
  public static final String DATABASE_TYPE_MYSQL = "mysql";
  public static final String DATABASE_TYPE_ORACLE = "oracle";
  public static final String DATABASE_TYPE_POSTGRES = "postgres";
  public static final String DATABASE_TYPE_MSSQL = "mssql";
  public static final String DATABASE_TYPE_DB2 = "db2";
  /*
  * 内部使用 Properties类封装了 所有的DB 厂商 信息 ,从而解决了 同一个DB 存在多个版本的问题
  * 进而把同种类型但是版本不同的DB 进行统一对待,
  * 例如 DB2 提供了很多发行版本, 但是不管客户端使用哪个版本呢么 经过这个处理之后 ,
  * 统一按照DB2类型处理
  * */
  protected static Properties getDefaultDatabaseTypeMappings() {
    Properties databaseTypeMappings = new Properties();
    databaseTypeMappings.setProperty("H2", DATABASE_TYPE_H2);
    databaseTypeMappings.setProperty("HSQL Database Engine", DATABASE_TYPE_HSQL);
    databaseTypeMappings.setProperty("MySQL", DATABASE_TYPE_MYSQL);
    databaseTypeMappings.setProperty("Oracle", DATABASE_TYPE_ORACLE);
    databaseTypeMappings.setProperty("PostgreSQL", DATABASE_TYPE_POSTGRES);
    databaseTypeMappings.setProperty("Microsoft SQL Server", DATABASE_TYPE_MSSQL);
    databaseTypeMappings.setProperty(DATABASE_TYPE_DB2,DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/NT",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/NT64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDP",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUX",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUX390",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXX8664",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXZ64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXPPC64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/LINUXPPC64LE",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/400 SQL",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/6000",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDB iSeries",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/AIX64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/HPUX",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/HP64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/SUN",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/SUN64",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/PTX",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2/2",DATABASE_TYPE_DB2);
    databaseTypeMappings.setProperty("DB2 UDB AS400", DATABASE_TYPE_DB2);
    return databaseTypeMappings;
  }
/*
* 获取DB类型是为了下一步对DB的SQL语句进行差异化做铺垫的
* '因此 获取DB类型的操作 是非常重要的 ,
*
* 只有databaseType 为空 程序才会执行initDatabaseType() 进行判断
* 而此() 的处理就需要打开一次DB 链接进行DB类型的获取
* 仅仅是为了获取DB的类型就打开一次DB连接,有点得不偿失
* 所以可以直接在 databaseType开关属性值
* */
  public void initDatabaseType() {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();//打开链接
      //获取DB生产厂商信息
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      //mysql 的  name 为 MySQL
      String databaseProductName = databaseMetaData.getDatabaseProductName();
      log.debug("database product name: '{}'", databaseProductName);
      //从集合中获取DB类型
      databaseType = databaseTypeMappings.getProperty(databaseProductName);
      if (databaseType==null) {
        throw new ActivitiException("couldn't deduct database type from database product name '"+databaseProductName+"'");
      }
      log.debug("using database type: {}", databaseType);

    } catch (SQLException e) {
      log.error("Exception while initializing Database connection", e);
    } finally {
      try {
        if (connection!=null) {
          connection.close();//关闭连接
        }
      } catch (SQLException e) {
          log.error("Exception while closing the Database connection", e);
      }
    }
  }
  
  // myBatis SqlSessionFactory ////////////////////////////////////////////////
  /*
  * 初始化事务工厂
  * */
  protected void initTransactionFactory() {
    if (transactionFactory==null) {
      if (transactionsExternallyManaged) { //
        //Spring 事务
        transactionFactory = new ManagedTransactionFactory();
      } else {
        transactionFactory = new JdbcTransactionFactory();
      }
    }
  }
  /*
  * 初始化SqlSessionFactory
  * */
  protected void initSqlSessionFactory() {
    if (sqlSessionFactory==null) {
      //判null
      InputStream inputStream = null;
      try {
        /*如果打算自定义Mapper文件, 则可以自定义一个类, 然后继承当前类 (流程引擎配置类) 并且重写该() 即可 */
        inputStream = getMyBatisXmlConfigurationSteam();//获取Mapper  映射文件的数据流

        // update the jdbc parameters to the configured ones...
        Environment environment = new Environment("default", transactionFactory, dataSource);
        //实例化文件流读取器Reader类
        Reader reader = new InputStreamReader(inputStream);
        Properties properties = new Properties();
        /*
        根据DB的类型设置不同的属性值
        这些属性值和SQL分页 以及 排序有关,
        为何要这样? 意义?
          因为 不同的DB 有一定的差异 ,这个属性值就是为了解决这个问题
         */

        properties.put("prefix", databaseTablePrefix);
        String wildcardEscapeClause = "";
        if ((databaseWildcardEscapeCharacter != null) && (databaseWildcardEscapeCharacter.length() != 0)) {
          wildcardEscapeClause = " escape '" + databaseWildcardEscapeCharacter + "'";
        }
        properties.put("wildcardEscapeClause", wildcardEscapeClause);
        if(databaseType != null) {
          properties.put("limitBefore" , DbSqlSessionFactory.databaseSpecificLimitBeforeStatements.get(databaseType));
          properties.put("limitAfter" , DbSqlSessionFactory.databaseSpecificLimitAfterStatements.get(databaseType));
          properties.put("limitBetween" , DbSqlSessionFactory.databaseSpecificLimitBetweenStatements.get(databaseType));
          properties.put("limitOuterJoinBetween" , DbSqlSessionFactory.databaseOuterJoinLimitBetweenStatements.get(databaseType));
          properties.put("orderBy" , DbSqlSessionFactory.databaseSpecificOrderByStatements.get(databaseType));
          properties.put("limitBeforeNativeQuery" , ObjectUtils.toString(DbSqlSessionFactory.databaseSpecificLimitBeforeNativeQueryStatements.get(databaseType)));
        }
//     MyBatis 中所有的信息 都存储在该类中, 可以将该类理解为MyBAtis 的注册中心, 该类提供了一系列设置属性的()
        Configuration configuration = initMybatisConfiguration(environment, reader, properties);
        sqlSessionFactory = new DefaultSqlSessionFactory(configuration);

      } catch (Exception e) {
        throw new ActivitiException("Error while building ibatis SqlSessionFactory: " + e.getMessage(), e);
      } finally {
        IoUtil.closeSilently(inputStream);
      }
    }
  }
  /*
  * 负责 整个配置类 Configuration 的实例化 以及属性填充工作
  * 1) 实例化 XMLConfigBuilder 类 ,该类主要用于构造 Configuration 对象
  * 5)
  * */
	protected Configuration initMybatisConfiguration(Environment environment, Reader reader, Properties properties) {
	  XMLConfigBuilder parser = new XMLConfigBuilder(reader,"", properties);
	  Configuration configuration = parser.getConfiguration();
	  configuration.setEnvironment(environment);
	  
	  initMybatisTypeHandlers(configuration);
	  initCustomMybatisMappers(configuration);
	  // 用于注册Mapper映射配置文件  XML 配置
	  configuration = parseMybatisConfiguration(configuration, parser);
	  return configuration;
  }
/*
注册类型转换器
IbatisVariableTypeHandler  实现DB中 VARCHAR类型与Activiti中的 VariableType类型的相互转换 ,
Activiti中 可以使用的变量类型都是通过 此Handler 进行转换的
* */
	protected void initMybatisTypeHandlers(Configuration configuration) {
	  configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler());
  }
  /*
  * 自定义映射类 (注解方式)  可以直接设置 CustomMybatisMappers 开关属性
  * */
	protected void initCustomMybatisMappers(Configuration configuration) {
	  if (getCustomMybatisMappers() != null) {
	  	for (Class<?> clazz : getCustomMybatisMappers()) {
	  		configuration.addMapper(clazz);
	  	}
	  }
  }
	  /*
	  *
	  * */
	protected Configuration parseMybatisConfiguration(Configuration configuration, XMLConfigBuilder parser) {
	  return parseCustomMybatisXMLMappers(parser.parse());
  }
	
	protected Configuration parseCustomMybatisXMLMappers(Configuration configuration) {
	  if (getCustomMybatisXMLMappers() != null)   //判null
    // see XMLConfigBuilder.mapperElement()
    for(String resource: getCustomMybatisXMLMappers()){ //循环遍历
      XMLMapperBuilder mapperParser = new XMLMapperBuilder(getResourceAsStream(resource), 
          configuration, resource, configuration.getSqlFragments());
      mapperParser.parse();//开始解析
    }
    return configuration;//最终将其注册 configuration对象中
  }
  
	protected InputStream getResourceAsStream(String resource) {
    return ReflectUtil.getResourceAsStream(resource);
  }
	
  protected InputStream getMyBatisXmlConfigurationSteam() {
    return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
  }
  
  public Set<Class<?>> getCustomMybatisMappers() {
	return customMybatisMappers;
  }

  public void setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
	this.customMybatisMappers = customMybatisMappers;
  }
  // 自定义mybatis中的XML 映射文件
  public Set<String> getCustomMybatisXMLMappers() {
    return customMybatisXMLMappers;
  }
  
  public void setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
    this.customMybatisXMLMappers = customMybatisXMLMappers;
  }
  
  // session factories ////////////////////////////////////////////////////////
  
  /*
  * 初始化SqlSessionFactory   该类主要提供 DB的连接 以及DB的访问操作
  * 但是Activiti并没有直接对外部程序提供SqlSessionFactory 而是使用了DbSqlSessionFactory 包裹了 SqlSessionFactory
  *
  * */
  protected void initSessionFactories() {
    if (sessionFactories==null) {
      sessionFactories = new HashMap<Class<?>, SessionFactory>();

      if (dbSqlSessionFactory == null) {
        dbSqlSessionFactory = new DbSqlSessionFactory();
      }
      //填充属性  设置表前缀, 批量操作 \限制  是否使用权限表 是否使用历史表
      dbSqlSessionFactory.setDatabaseType(databaseType);
      dbSqlSessionFactory.setIdGenerator(idGenerator);
      dbSqlSessionFactory.setSqlSessionFactory(sqlSessionFactory);
      dbSqlSessionFactory.setDbIdentityUsed(isDbIdentityUsed);
      dbSqlSessionFactory.setDbHistoryUsed(isDbHistoryUsed);
      dbSqlSessionFactory.setDatabaseTablePrefix(databaseTablePrefix);
      dbSqlSessionFactory.setTablePrefixIsSchema(tablePrefixIsSchema);
      dbSqlSessionFactory.setDatabaseCatalog(databaseCatalog);
      dbSqlSessionFactory.setDatabaseSchema(databaseSchema);
      dbSqlSessionFactory.setBulkInsertEnabled(isBulkInsertEnabled, databaseType);
      dbSqlSessionFactory.setMaxNrOfStatementsInBulkInsert(maxNrOfStatementsInBulkInsert);
      addSessionFactory(dbSqlSessionFactory);
      //实体管理器 类的添加
      addSessionFactory(new GenericManagerFactory(AttachmentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(CommentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(DeploymentEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ModelEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ExecutionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricActivityInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricDetailEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricProcessInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricVariableInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricTaskInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(HistoricIdentityLinkEntityManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityInfoEntityManager.class));
      addSessionFactory(new GenericManagerFactory(IdentityLinkEntityManager.class));
      addSessionFactory(new GenericManagerFactory(JobEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ProcessDefinitionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ProcessDefinitionInfoEntityManager.class));
      addSessionFactory(new GenericManagerFactory(PropertyEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ResourceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(ByteArrayEntityManager.class));
      addSessionFactory(new GenericManagerFactory(TableDataManager.class));
      addSessionFactory(new GenericManagerFactory(TaskEntityManager.class));
      addSessionFactory(new GenericManagerFactory(VariableInstanceEntityManager.class));
      addSessionFactory(new GenericManagerFactory(EventSubscriptionEntityManager.class));
      addSessionFactory(new GenericManagerFactory(EventLogEntryEntityManager.class));
      
      addSessionFactory(new DefaultHistoryManagerSessionFactory());
      
      addSessionFactory(new UserEntityManagerFactory());
      addSessionFactory(new GroupEntityManagerFactory());
      addSessionFactory(new MembershipEntityManagerFactory());
    }
    
    if (customSessionFactories!=null) {
      for (SessionFactory sessionFactory: customSessionFactories) {
        addSessionFactory(sessionFactory);
      }
    }
  }
  /*
  * Map结构 Key为 getSessionType()   value是  SessionFactory
  * 可以自定义  替换内置的即可
  * */
  protected void addSessionFactory(SessionFactory sessionFactory) {
    sessionFactories.put(sessionFactory.getSessionType(), sessionFactory);
  }
  /*
  初始化配置器,
  不管是Activiti配置风格还是Spring风格 都是 XML配置
  在XML 中定义力流程引擎配置类 有如下几个缺点
  1) 不灵活, 所有的属性信息都需要在XML中进行配置
  2) 不能满足动态属性配置需求,如果开发人员打算使用编程方式构造流程引擎配置类的对象 则这种方式几乎不可能实现
  3)无法检查必要的属性是否已经被初始化,比如 期望检查数据源的信息 . 则不能满足需求

  Actvitii 5.13版本 增加了 配置器,
  可以通过编程的方式 动态修改 或者刷新 配置类中属性值
  所有的配置器 都需要实现,ProcessEngineConfigurator
   */
  protected void initConfigurators() {

  	//该集合用于存储所有的配置器实例
  	allConfigurators = new ArrayList<ProcessEngineConfigurator>();
  	//判断 开关属性 configurators 是否为空 ,如果不为空, 则循环遍历该集合 ,并将值 添加到 此集合中
    if (configurators != null) {
      for (ProcessEngineConfigurator configurator : configurators) {
        allConfigurators.add(configurator);
      }
    }
    //判断是否为true 默认为true
    if (enableConfiguratorServiceLoader) {
      //获取类加载器
    	ClassLoader classLoader = getClassLoader();
    	if (classLoader == null) {
    		classLoader = ReflectUtil.getClassLoader();
    	}
    	//利用java中的serviceclassloader  特性加载 ProcessEngineConfigurator集合
    	ServiceLoader<ProcessEngineConfigurator> configuratorServiceLoader
    			= ServiceLoader.load(ProcessEngineConfigurator.class, classLoader);
    	int nrOfServiceLoadedConfigurators = 0;
    	//循环遍历并添加到allConfigurators 集合中
    	for (ProcessEngineConfigurator configurator : configuratorServiceLoader) {
    		allConfigurators.add(configurator);
    		nrOfServiceLoadedConfigurators++;
    	}
    	
    	if (nrOfServiceLoadedConfigurators > 0) {
    		log.info("Found {} auto-discoverable Process Engine Configurator{}", nrOfServiceLoadedConfigurators++, nrOfServiceLoadedConfigurators > 1 ? "s" : "");
    	}
    	//判断allConfigurators集合是否为空 如果 不为空 , 对集合中的元素按照
    	if (!allConfigurators.isEmpty()) {
    		  //按照ProcessEngineConfigurator 中的getPriporty() 返回值进行升序排序
	    	Collections.sort(allConfigurators, new Comparator<ProcessEngineConfigurator>() {
	    		@Override
	    		public int compare(ProcessEngineConfigurator configurator1, ProcessEngineConfigurator configurator2) {
	    			int priority1 = configurator1.getPriority();
	    			int priority2 = configurator2.getPriority();
	    			
	    			if (priority1 < priority2) {
	    				return -1;
	    			} else if (priority1 > priority2) {
	    				return 1;
	    			} 
	    			return 0;
	    		}
				});
	    	
	    	// Execute the configurators
	    	log.info("Found {} Process Engine Configurators in total:", allConfigurators.size());
	    	for (ProcessEngineConfigurator configurator : allConfigurators) {
	    		log.info("{} (priority:{})", configurator.getClass(), configurator.getPriority());
	    	}
	    	
    	}
    	
    }
  }
  
  protected void configuratorsBeforeInit() {
  	for (ProcessEngineConfigurator configurator : allConfigurators) {
  		log.info("Executing beforeInit() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
  		configurator.beforeInit(this);
  	}
  }
  
  protected void configuratorsAfterInit() {
  	for (ProcessEngineConfigurator configurator : allConfigurators) {
  		log.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
  		configurator.configure(this);
  	}
  }
  
  // deployers ////////////////////////////////////////////////////////////////
  /*
  初始化 部署器
  为什么要用两个变量分别定义 前置部署器和后置部署器呢? 定义一个岂不是更好??
  因为部署器集合是 List  ,List是有序的, 也是为了后续遍历该集合时 有先后顺序
   */
  protected void initDeployers() {
    //判断 deployers 集合是否已经 初始化 ,如果 已经初始化 ,则不会进行实例化操作
    if (this.deployers==null) {
      //初始化 部署器集合
      this.deployers = new ArrayList<Deployer>();
      //添加前置部署器不为空,则向集合中添加该值
      if (customPreDeployers!=null) {
        this.deployers.addAll(customPreDeployers);
      }
      //添加内置部署器
      this.deployers.addAll(getDefaultDeployers());
      //添加后置部署器
      if (customPostDeployers!=null) {
        this.deployers.addAll(customPostDeployers);
      }
    }
    //部署管理器
    if (deploymentManager==null) {
      //实例化部署管理器
      deploymentManager = new DeploymentManager();
      //向部署管理器 添加部署器集合
      deploymentManager.setDeployers(deployers);

      /*
      关于 缓存   有4种
      bpmnModel   processDefinition  processDefinitionInfo  KnowLedge

    上述 各自 职责 不同, 但是 逻辑 大体相同
      如下:
        1) 客户端自定义缓存处理类判断
          以上4种 逻辑都是 首先判断客户端是否自定义了 缓存处理类 (开关属性)

          如果客户端定义了缓存处理类, 则直接使用
          否则使用系统内置的缓存处理类,

          在实例化系统内置的缓存处理类的时候 需要根据缓存对象的容器大小,限制值进行判断
        2) 系统内置缓存处理类策略判断
          首先获取客户端配置的缓存对象容器大小限制值(卡关属性) 以上四种 默认为 -1
          也就是说 默认堆缓存对象的容器大小不进行限制, 内部使用Map 实现
          当限制缓存容器大小时,使用LRU进行算法控制
          程序以缓存的限制值 实例化不同的缓存处理类,

          DefaultDeploymentCache 类 为默认的缓存处理类
        3) 将 4个缓存处理类设置到DeploymentManager中
       */
      //如果流程定义缓存 限制 为空
      if (processDefinitionCache == null) {
        //  processDefinitionCacheLimit 默认-1 表示不开启缓存
        if (processDefinitionCacheLimit <= 0) {
          processDefinitionCache = new DefaultDeploymentCache<ProcessDefinitionEntity>();
        } else {
          processDefinitionCache = new DefaultDeploymentCache<ProcessDefinitionEntity>(processDefinitionCacheLimit);
        }
      }
      
//    其他 的缓存处理类 ....
      if (bpmnModelCache == null) {
        if (bpmnModelCacheLimit <= 0) {
          bpmnModelCache = new DefaultDeploymentCache<BpmnModel>();
        } else {
          bpmnModelCache = new DefaultDeploymentCache<BpmnModel>(bpmnModelCacheLimit);
        }
      }
      
      if (processDefinitionInfoCache == null) {
        if (processDefinitionInfoCacheLimit <= 0) {
          processDefinitionInfoCache = new ProcessDefinitionInfoCache(commandExecutor);
        } else {
          processDefinitionInfoCache = new ProcessDefinitionInfoCache(commandExecutor, processDefinitionInfoCacheLimit);
        }
      }
      
      // Knowledge base cache (used for Drools business task)
      if (knowledgeBaseCache == null) {
        if (knowledgeBaseCacheLimit <= 0) {
          knowledgeBaseCache = new DefaultDeploymentCache<Object>();
        } else {
          knowledgeBaseCache = new DefaultDeploymentCache<Object>(knowledgeBaseCacheLimit);
        }
      }
      //设置缓存处理类
      deploymentManager.setProcessDefinitionCache(processDefinitionCache);
      deploymentManager.setBpmnModelCache(bpmnModelCache);
      deploymentManager.setProcessDefinitionInfoCache(processDefinitionInfoCache);
      deploymentManager.setKnowledgeBaseCache(knowledgeBaseCache);
    }
  }
  /*
  此() 是获取 内置部署器的入口
  涉及 部署器的实例化 ,元素解析器的实例化 对象解析器的实例化 以及相关属性 填充操作
   */
  protected Collection< ? extends Deployer> getDefaultDeployers() {
    // 初始化  defaultDeployers  用来存储所有的部署器实例对象
    List<Deployer> defaultDeployers = new ArrayList<Deployer>();
    //
    if (bpmnDeployer == null) {
      bpmnDeployer = new BpmnDeployer(); //实例化 bpmnDeployer 类
    }
      
    bpmnDeployer.setExpressionManager(expressionManager); //设置表达式管理器  解析 JUEL表达式
    bpmnDeployer.setIdGenerator(idGenerator);//id生成器  负责DB主键ID值生成
    //默认的BPMN解析工厂 该工厂负责创建BpmnParse 对象   也可以自定义实现
    if (bpmnParseFactory == null) {
      bpmnParseFactory = new DefaultBpmnParseFactory();
    }
    /*
    行为类 工厂
    判断  是否自定义了 行为类 工厂,
    最简单的方法就是 定义一个类 继承  DefaultActivityBehaviorFactory 然后  设置   activityBehaviorFactory 开关属性即可



     */
	if (activityBehaviorFactory == null) {
	  DefaultActivityBehaviorFactory defaultActivityBehaviorFactory = new DefaultActivityBehaviorFactory();
	  defaultActivityBehaviorFactory.setExpressionManager(expressionManager);
	  activityBehaviorFactory = defaultActivityBehaviorFactory;
	} else if ((activityBehaviorFactory instanceof AbstractBehaviorFactory)
			&& ((AbstractBehaviorFactory) activityBehaviorFactory).getExpressionManager() == null) {
		((AbstractBehaviorFactory) activityBehaviorFactory).setExpressionManager(expressionManager);
	}
    //监听器工厂
	if (listenerFactory == null) {
	  DefaultListenerFactory defaultListenerFactory = new DefaultListenerFactory();
	  defaultListenerFactory.setExpressionManager(expressionManager);
	  listenerFactory = defaultListenerFactory;
	} else if ((listenerFactory instanceof AbstractBehaviorFactory)
			&& ((AbstractBehaviorFactory) listenerFactory).getExpressionManager() == null) {
		((AbstractBehaviorFactory) listenerFactory).setExpressionManager(expressionManager);
	}
    //实例话BPMNParser 类    该类很重要 负责 创建 BpmnParse 对象 而 BpmnParse 对象 负责全局调度 元素解析 和对象解析工作
    if (bpmnParser == null) {
      bpmnParser = new BpmnParser();
    }
    
    bpmnParser.setExpressionManager(expressionManager);
    bpmnParser.setBpmnParseFactory(bpmnParseFactory);
    bpmnParser.setActivityBehaviorFactory(activityBehaviorFactory);
    bpmnParser.setListenerFactory(listenerFactory);
    
    List<BpmnParseHandler> parseHandlers = new ArrayList<BpmnParseHandler>();
    if(getPreBpmnParseHandlers() != null) { //前置对象解析器
      parseHandlers.addAll(getPreBpmnParseHandlers());
    }
    parseHandlers.addAll(getDefaultBpmnParseHandlers()); //内置对象解析器
    if(getPostBpmnParseHandlers() != null) {// 后置对象解析器
      parseHandlers.addAll(getPostBpmnParseHandlers());
    }
    //
    BpmnParseHandlers bpmnParseHandlers = new BpmnParseHandlers();
    bpmnParseHandlers.addHandlers(parseHandlers);
    bpmnParser.setBpmnParserHandlers(bpmnParseHandlers);
    
    bpmnDeployer.setBpmnParser(bpmnParser);
    
    defaultDeployers.add(bpmnDeployer);
    return defaultDeployers;
  }
  /*
  初始化内置对象解析器
   */
  protected List<BpmnParseHandler> getDefaultBpmnParseHandlers() {
    
    // Alpabetic list of default parse handler classes
    // 这一系列的内置对象解析器 都间接或者直接实现了    BpmnParseHandler 接口
    List<BpmnParseHandler> bpmnParserHandlers = new ArrayList<BpmnParseHandler>();
    bpmnParserHandlers.add(new BoundaryEventParseHandler());
    bpmnParserHandlers.add(new BusinessRuleParseHandler());
    bpmnParserHandlers.add(new CallActivityParseHandler());
    bpmnParserHandlers.add(new CancelEventDefinitionParseHandler());
    bpmnParserHandlers.add(new CompensateEventDefinitionParseHandler());
    bpmnParserHandlers.add(new EndEventParseHandler());
    bpmnParserHandlers.add(new ErrorEventDefinitionParseHandler());
    bpmnParserHandlers.add(new EventBasedGatewayParseHandler());
    bpmnParserHandlers.add(new ExclusiveGatewayParseHandler());
    bpmnParserHandlers.add(new InclusiveGatewayParseHandler());
    bpmnParserHandlers.add(new IntermediateCatchEventParseHandler());
    bpmnParserHandlers.add(new IntermediateThrowEventParseHandler());
    bpmnParserHandlers.add(new ManualTaskParseHandler());
    bpmnParserHandlers.add(new MessageEventDefinitionParseHandler());
    bpmnParserHandlers.add(new ParallelGatewayParseHandler());
    bpmnParserHandlers.add(new ProcessParseHandler());
    bpmnParserHandlers.add(new ReceiveTaskParseHandler());
    bpmnParserHandlers.add(new ScriptTaskParseHandler());
    bpmnParserHandlers.add(new SendTaskParseHandler());
    bpmnParserHandlers.add(new SequenceFlowParseHandler());
    bpmnParserHandlers.add(new ServiceTaskParseHandler());
    bpmnParserHandlers.add(new SignalEventDefinitionParseHandler());
    bpmnParserHandlers.add(new StartEventParseHandler());
    bpmnParserHandlers.add(new SubProcessParseHandler());
    bpmnParserHandlers.add(new EventSubProcessParseHandler());
    bpmnParserHandlers.add(new TaskParseHandler());
    bpmnParserHandlers.add(new TimerEventDefinitionParseHandler());
    bpmnParserHandlers.add(new TransactionParseHandler());
    bpmnParserHandlers.add(new UserTaskParseHandler());
    
    // Replace any default handler if the user wants to replace them
    //判断  customDefaultBpmnParseHandlers 是否为空null
    if (customDefaultBpmnParseHandlers != null) {
      /*
      循环遍历该集合并且 替换步骤1)中的内置对象解析器,
      如果开发人员   觉得 Activiti 中的内置对象解析器 不能满足 自己的业务需求,
      可以通过该属性替换 内置 对象解析器
       */
      Map<Class<?>, BpmnParseHandler> customParseHandlerMap = new HashMap<Class<?>, BpmnParseHandler>();
      for (BpmnParseHandler bpmnParseHandler : customDefaultBpmnParseHandlers) {
        for (Class<?> handledType : bpmnParseHandler.getHandledTypes()) {
          customParseHandlerMap.put(handledType, bpmnParseHandler);
        }
      }
      // 循环遍历  内置的handler集合 , 如果  customParseHandlerMap 中存在值, 则替换内置对象解析器
      for (int i=0; i<bpmnParserHandlers.size(); i++) {
        // All the default handlers support only one type
        BpmnParseHandler defaultBpmnParseHandler = bpmnParserHandlers.get(i);
        if (defaultBpmnParseHandler.getHandledTypes().size() != 1) {
          StringBuilder supportedTypes = new StringBuilder();
          for (Class<?> type : defaultBpmnParseHandler.getHandledTypes()) {
            supportedTypes.append(" ").append(type.getCanonicalName()).append(" ");
          }
          throw new ActivitiException("The default BPMN parse handlers should only support one type, but " + defaultBpmnParseHandler.getClass() 
                  + " supports " + supportedTypes.toString() + ". This is likely a programmatic error");
        } else {
          Class<?> handledType = defaultBpmnParseHandler.getHandledTypes().iterator().next();
          if (customParseHandlerMap.containsKey(handledType)) {
            BpmnParseHandler newBpmnParseHandler = customParseHandlerMap.get(handledType);
            log.info("Replacing default BpmnParseHandler " + defaultBpmnParseHandler.getClass().getName() + " with " + newBpmnParseHandler.getClass().getName());
            bpmnParserHandlers.set(i, newBpmnParseHandler);
          }
        }
      }
    }
    
    // History
    /*
    获取历史解析器, 然后 添加到  bpmnParserHandlers 集合中
    需要获取的历史解析器 有
    UserTaskHistoryParseHandler
    StartEventHistoryParseHandler
    ProcessHistoryParseHandler

    添加历史解析器的目的是 主要是 为 部分对象(这些对象均被历史解析器所管理)
    自动添加系统内置记录监听器,
    以上4种 历史解析器 被添加到  bpmnParserHandlers 集合中
    这样解析  BaseElement 对象时
    就会查找 BaseElement对象对应的 所有解析器, 然后依次执行
    通过上面的添加按顺序 可知
    历史解析器永远是最后执行的(前提是 开发人员没有定义后置对象解析器)
    可以查看这些历史解析器中 executeParse() 的 处理逻辑
     */
    for (BpmnParseHandler handler : getDefaultHistoryParseHandlers()) {
      bpmnParserHandlers.add(handler);
    }
    
    return bpmnParserHandlers;
  }
  /*
  * 默认的 历史解析器初始化的过程
  * */
  protected List<BpmnParseHandler> getDefaultHistoryParseHandlers() {
    List<BpmnParseHandler> parseHandlers = new ArrayList<BpmnParseHandler>();
    parseHandlers.add(new FlowNodeHistoryParseHandler()); //负责事件 活动, 网关 的内置记录监听器
    parseHandlers.add(new ProcessHistoryParseHandler());//负责process元素
    parseHandlers.add(new StartEventHistoryParseHandler());//负责startEvent
    parseHandlers.add(new UserTaskHistoryParseHandler());//负责userTask
    return parseHandlers;
  }

  private void initClock() {
    if (clock == null) {
      clock = new DefaultClockImpl();
    }
  }

  protected void initProcessDiagramGenerator() {
    if (processDiagramGenerator == null) {
      processDiagramGenerator = new DefaultProcessDiagramGenerator();
    }
  }
  /*
  初始化 作业处理器
  Activiti 将一系列的作业按照类型进行归类, 这样同种类型的作业处理器都为同一个  也方便程序的维护
   */
  protected void initJobHandlers() {
    //
    jobHandlers = new HashMap<String, JobHandler>();
    /*
    任务超时作业处理器
    在实际项目中 可以为任务节点绑定一个定时边界事件 ,如果任务节点 在指定的时间内没有结束,
    则流程实例会按照边界事件的流向进行流转
        cancelActivity 为true
            当作业被处理时,当前的活动节点 (attachedToRef) 会被中断,可以称之为中断事件 ,引擎默认使用的是中断事件方式
          cancelActivity 为false
            当作业被处理时,当前的活动节点不会被中断,即当前的活动节点会继续存活,
            例如 userTask1  任务节点会继续保留在Task表中
            但是引擎会创建一个新的分支,  从而可以让流程实例 沿着id为 boudarytimer1 节点对应的流向继续向下运转 ,可以称之为 非中断事件

    cancelActivity: 可以参考 BoundaryEventActivityBehavior  的 execute()

     */
    TimerExecuteNestedActivityJobHandler timerExecuteNestedActivityJobHandler = new TimerExecuteNestedActivityJobHandler();
    //
    jobHandlers.put(timerExecuteNestedActivityJobHandler.getType(), timerExecuteNestedActivityJobHandler);
    /*
    定时任务作业处理器

    在实际开发中 可以使用定时中间事件来指定流程实例到达下一节点的时间

    比如 流程实例 首先到达 task1   当任务节点完成 20s之后 ,自动到达第2个节点上

    intermediateCatchEvent 行为类是 intermediateCatchEventActivityBehavior
     */
    TimerCatchIntermediateEventJobHandler timerCatchIntermediateEvent = new TimerCatchIntermediateEventJobHandler();
    jobHandlers.put(timerCatchIntermediateEvent.getType(), timerCatchIntermediateEvent);
    /*
    定时启动流程实例作业处理器
    在实际开发中 可以在定义开始节点 中配置启动该流程实例的时间阈值, 这样当流程文档部署完毕之后
    超过该 阈值 之后 流程实例会被 作业处理器启动
    比方  部署一个流程  20s 之后自动启动
     */
    TimerStartEventJobHandler timerStartEvent = new TimerStartEventJobHandler();
    jobHandlers.put(timerStartEvent.getType(), timerStartEvent);
    /*
    其他作业
    Activiti 在5.11版本之后 增加了挂起 流程的 特性,
    挂起 和激活 流程的() 都在 RepositoryService中
     */
    //异步节点处理器
    AsyncContinuationJobHandler asyncContinuationJobHandler = new AsyncContinuationJobHandler();
    jobHandlers.put(asyncContinuationJobHandler.getType(), asyncContinuationJobHandler);
    //暂不清楚
    ProcessEventJobHandler processEventJobHandler = new ProcessEventJobHandler();
    jobHandlers.put(processEventJobHandler.getType(), processEventJobHandler);
    //挂起 流程定义处理器
    TimerSuspendProcessDefinitionHandler suspendProcessDefinitionHandler = new TimerSuspendProcessDefinitionHandler();
    jobHandlers.put(suspendProcessDefinitionHandler.getType(), suspendProcessDefinitionHandler);
    //激活流程定义处理器
    TimerActivateProcessDefinitionHandler activateProcessDefinitionHandler = new TimerActivateProcessDefinitionHandler();
    jobHandlers.put(activateProcessDefinitionHandler.getType(), activateProcessDefinitionHandler);
    //getCustomJobHandlers() 用于获取 customJobHandlers    他是开关属性,  可以通过它 替换引擎默认的作业处理器
    if (getCustomJobHandlers()!=null) {
      for (JobHandler customJobHandler : getCustomJobHandlers()) {
        jobHandlers.put(customJobHandler.getType(), customJobHandler);      
      }
    }
  }

  // job executor /////////////////////////////////////////////////////////////
  /*
   初始化作业执行器

   */
  protected void initJobExecutor() {
    //如果 异步作业执行没有开启, 初始化作业执行器
    //也即对于开发人员来说 作业执行器 和异步作业执行器 功能只能选用一个
    if (isAsyncExecutorEnabled() == false) {

      if (jobExecutor == null) {//
        //默认作业执行器
        jobExecutor = new DefaultJobExecutor();
      }
    //设置clock
      jobExecutor.setClockReader(this.clock);
    //命令执行器
      jobExecutor.setCommandExecutor(commandExecutor);
      //作业执行器是否激活   只有此属性值为true  引擎才会开启 作业执行器功能
      jobExecutor.setAutoActivate(jobExecutorActivate);
      //
      if (jobExecutor.getRejectedJobsHandler() == null) {
        if(customRejectedJobsHandler != null) {
          jobExecutor.setRejectedJobsHandler(customRejectedJobsHandler);
        } else {
          jobExecutor.setRejectedJobsHandler(new CallerRunsRejectedJobsHandler());
        }
      }
    }
  }
  
  // async executor /////////////////////////////////////////////////////////////
  
  protected void initAsyncExecutor() {
    if (isAsyncExecutorEnabled()) {
      if (asyncExecutor == null) {
        DefaultAsyncJobExecutor defaultAsyncExecutor = new DefaultAsyncJobExecutor();
        
        // Thread pool config
        defaultAsyncExecutor.setCorePoolSize(asyncExecutorCorePoolSize);
        defaultAsyncExecutor.setMaxPoolSize(asyncExecutorMaxPoolSize);
        defaultAsyncExecutor.setKeepAliveTime(asyncExecutorThreadKeepAliveTime);
        
        // Threadpool queue
        if (asyncExecutorThreadPoolQueue != null) {
        	defaultAsyncExecutor.setThreadPoolQueue(asyncExecutorThreadPoolQueue);
        }
        defaultAsyncExecutor.setQueueSize(asyncExecutorThreadPoolQueueSize);
        
        // Acquisition wait time
        defaultAsyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(asyncExecutorDefaultTimerJobAcquireWaitTime);
        defaultAsyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(asyncExecutorDefaultAsyncJobAcquireWaitTime);
        
        // Queue full wait time
        defaultAsyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(asyncExecutorDefaultQueueSizeFullWaitTime);
        
        // Job locking
        defaultAsyncExecutor.setTimerLockTimeInMillis(asyncExecutorTimerLockTimeInMillis);
        defaultAsyncExecutor.setAsyncJobLockTimeInMillis(asyncExecutorAsyncJobLockTimeInMillis);
        if (asyncExecutorLockOwner != null) {
        	defaultAsyncExecutor.setLockOwner(asyncExecutorLockOwner);
        }
        
        // Retry
        defaultAsyncExecutor.setRetryWaitTimeInMillis(asyncExecutorLockRetryWaitTimeInMillis);
        
        // Shutdown
        defaultAsyncExecutor.setSecondsToWaitOnShutdown(asyncExecutorSecondsToWaitOnShutdown);
        
        asyncExecutor = defaultAsyncExecutor;
      }
  
      asyncExecutor.setCommandExecutor(commandExecutor);
      asyncExecutor.setAutoActivate(asyncExecutorActivate);
    }
  }
  
  // history //////////////////////////////////////////////////////////////////
  
  public void initHistoryLevel() {
  	if(historyLevel == null) {
  		historyLevel = HistoryLevel.getHistoryLevelForKey(getHistory());
  	}
  }
  
  // id generator /////////////////////////////////////////////////////////////
  /*
  * 初始化id生成器
  * */
  protected void initIdGenerator() {
    if (idGenerator==null) {
      CommandExecutor idGeneratorCommandExecutor = null;
      if (idGeneratorDataSource!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(idGeneratorDataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutor();
      } else if (idGeneratorDataSourceJndiName!=null) {
        ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        processEngineConfiguration.setDataSourceJndiName(idGeneratorDataSourceJndiName);
        processEngineConfiguration.setDatabaseSchemaUpdate(DB_SCHEMA_UPDATE_FALSE);
        processEngineConfiguration.init();
        idGeneratorCommandExecutor = processEngineConfiguration.getCommandExecutor();
      } else {
        idGeneratorCommandExecutor = getCommandExecutor();
      }
      
      DbIdGenerator dbIdGenerator = new DbIdGenerator();
      dbIdGenerator.setIdBlockSize(idBlockSize);
      dbIdGenerator.setCommandExecutor(idGeneratorCommandExecutor);
      dbIdGenerator.setCommandConfig(getDefaultCommandConfig().transactionRequiresNew());
      idGenerator = dbIdGenerator;
    }
  }

  // OTHER ////////////////////////////////////////////////////////////////////
  /*
  * 初始化命令上下文工厂
  * */
  protected void initCommandContextFactory() {
    if (commandContextFactory==null) {
      commandContextFactory = new CommandContextFactory();
    }
    //this 为 本类对象
    commandContextFactory.setProcessEngineConfiguration(this);
  }
/*
* 初始化事务上下文
* */
  protected void initTransactionContextFactory() {
    if (transactionContextFactory==null) {
      transactionContextFactory = new StandaloneMybatisTransactionContextFactory();
    }
  }
  /*
  * 初始化变量管理类
  * Activiti通过此() 处理这些变量,获取变量类型,
  * 也可以自定义变量类型
  *
  *
  * */
  protected void initVariableTypes() {
    if (variableTypes==null) {
      variableTypes = new DefaultVariableTypes();
      if (customPreVariableTypes!=null) { //前置  customPreVariables 开关属性可以覆盖系统内置的变量类
        for (VariableType customVariableType: customPreVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
      //内置
      variableTypes.addType(new NullType());
      variableTypes.addType(new StringType(getMaxLengthString()));
      variableTypes.addType(new LongStringType(getMaxLengthString() + 1));
      variableTypes.addType(new BooleanType());
      variableTypes.addType(new ShortType());
      variableTypes.addType(new IntegerType());
      variableTypes.addType(new LongType());
      variableTypes.addType(new DateType());
      variableTypes.addType(new DoubleType());
      variableTypes.addType(new UUIDType());
      variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper));
      variableTypes.addType(new LongJsonType(getMaxLengthString() + 1, objectMapper));
      variableTypes.addType(new ByteArrayType());
      variableTypes.addType(new SerializableType());
      variableTypes.addType(new CustomObjectType("item", ItemInstance.class));
      variableTypes.addType(new CustomObjectType("message", MessageInstance.class));
      // 后置  可以给开发人员最后一次机会修改变量处理类
      if (customPostVariableTypes != null) {
        for (VariableType customVariableType: customPostVariableTypes) {
          variableTypes.addType(customVariableType);
        }
      }
    }
  }
  /*
  * 如果 maxLengthStringVariableType 为-1
  * 则流程引擎连接的DB类型为
  * Oracle 时长度为2000 mysql 为4000
  * */
  protected int getMaxLengthString() {
    if (maxLengthStringVariableType == -1) {
      if ("oracle".equalsIgnoreCase(databaseType) == true) {
        return DEFAULT_ORACLE_MAX_LENGTH_STRING; //2000
      } else {
        return DEFAULT_GENERIC_MAX_LENGTH_STRING; //4000
      }
    } else {
      return maxLengthStringVariableType;
    }
  }

  protected void initFormEngines() {
    if (formEngines==null) {
      formEngines = new HashMap<String, FormEngine>();
      FormEngine defaultFormEngine = new JuelFormEngine();
      formEngines.put(null, defaultFormEngine); // default form engine is looked up with null
      formEngines.put(defaultFormEngine.getName(), defaultFormEngine);
    }
    if (customFormEngines!=null) {
      for (FormEngine formEngine: customFormEngines) {
        formEngines.put(formEngine.getName(), formEngine);
      }
    }
  }

  protected void initFormTypes() {
    if (formTypes==null) {
      formTypes = new FormTypes();
      formTypes.addFormType(new StringFormType());
      formTypes.addFormType(new LongFormType());
      formTypes.addFormType(new DateFormType("dd/MM/yyyy"));
      formTypes.addFormType(new BooleanFormType());
      formTypes.addFormType(new DoubleFormType());
    }
    if (customFormTypes!=null) {
      for (AbstractFormType customFormType: customFormTypes) {
        formTypes.addFormType(customFormType);
      }
    }
  }

  protected void initScriptingEngines() {
    if (resolverFactories==null) {
      resolverFactories = new ArrayList<ResolverFactory>();
      resolverFactories.add(new VariableScopeResolverFactory());
      resolverFactories.add(new BeansResolverFactory());
    }
    if (scriptingEngines==null) {
      scriptingEngines = new ScriptingEngines(new ScriptBindingsFactory(resolverFactories));
    }
  }

  protected void initExpressionManager() {
    if (expressionManager==null) {
      expressionManager = new ExpressionManager(beans);
    }
  }

  protected void initBusinessCalendarManager() {
    if (businessCalendarManager==null) {
      MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
      mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar(this.clock));
      mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar(this.clock));
      mapBusinessCalendarManager.addBusinessCalendar(CycleBusinessCalendar.NAME, new CycleBusinessCalendar(this.clock));

      businessCalendarManager = mapBusinessCalendarManager;
    }
  }
  
  protected void initDelegateInterceptor() {
    if(delegateInterceptor == null) {
      delegateInterceptor = new DefaultDelegateInterceptor();
    }
  }
  
  protected void initEventHandlers() {
    if(eventHandlers == null) {
      eventHandlers = new HashMap<String, EventHandler>();
      
      SignalEventHandler signalEventHander = new SignalEventHandler();
      eventHandlers.put(signalEventHander.getEventHandlerType(), signalEventHander);
      
      CompensationEventHandler compensationEventHandler = new CompensationEventHandler();
      eventHandlers.put(compensationEventHandler.getEventHandlerType(), compensationEventHandler);
      
      MessageEventHandler messageEventHandler = new MessageEventHandler();
      eventHandlers.put(messageEventHandler.getEventHandlerType(), messageEventHandler);
      
    }
    if(customEventHandlers != null) {
      for (EventHandler eventHandler : customEventHandlers) {
        eventHandlers.put(eventHandler.getEventHandlerType(), eventHandler);        
      }
    }
  }
  
  // JPA //////////////////////////////////////////////////////////////////////
  
  protected void initJpa() {
    if(jpaPersistenceUnitName!=null) {
      jpaEntityManagerFactory = JpaHelper.createEntityManagerFactory(jpaPersistenceUnitName);
    }
    if(jpaEntityManagerFactory!=null) {
      sessionFactories.put(EntityManagerSession.class, new EntityManagerSessionFactory(jpaEntityManagerFactory, jpaHandleTransaction, jpaCloseEntityManager));
      VariableType jpaType = variableTypes.getVariableType(JPAEntityVariableType.TYPE_NAME);
      // Add JPA-type
      if(jpaType == null) {
        // We try adding the variable right before SerializableType, if available
        int serializableIndex = variableTypes.getTypeIndex(SerializableType.TYPE_NAME);
        if(serializableIndex > -1) {
          variableTypes.addType(new JPAEntityVariableType(), serializableIndex);
        } else {
          variableTypes.addType(new JPAEntityVariableType());
        }   
      }
        
      jpaType = variableTypes.getVariableType(JPAEntityListVariableType.TYPE_NAME);
      
      // Add JPA-list type after regular JPA type if not already present
      if(jpaType == null) {
        variableTypes.addType(new JPAEntityListVariableType(), variableTypes.getTypeIndex(JPAEntityVariableType.TYPE_NAME));
      }        
    }
  }
  
  protected void initBeans() {
    if (beans == null) {
      beans = new HashMap<Object, Object>();
    }
  }
  /*
  初始化事件转发器
   */
  protected void initEventDispatcher() {
    //默认使用的转发器为 ActivitiEventDispatcherImpl  当然可以通过设置 eventDispatcher 注入自定义事件转发器
  	if(this.eventDispatcher == null) {
  		this.eventDispatcher = new ActivitiEventDispatcherImpl();
  	}
  	//开启事件转发功能 ,如果不想用, 那么关闭即可 设置为false 从而全局禁用
  	this.eventDispatcher.setEnabled(enableEventDispatcher);
  	//判断 eventListeners 集合是否为空 ,如果该集合不为空
  	if(eventListeners != null) {
  	  //循环遍历该集合并且调用  eventDispatcher 的 addEventListener 注册全局事件监听器
  		for(ActivitiEventListener listenerToAdd : eventListeners) {
  			this.eventDispatcher.addEventListener(listenerToAdd);
  		}
  	}
  	/*
  	注册具体类型的监听器
          typedEventListeners 集合为Map数据结构
          key 为具体的事件类型, value  是具体类型的事件监听器
  	 */
  	if(typedEventListeners != null) {
  	  //遍历集合
  		for(Entry<String, List<ActivitiEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
  		  //得到key 值  并将其转换为ActivitiEventType类型的数据,  (处理过程是 key使用,进行分割 可以参考getTypesFromString()的实现)
  			ActivitiEventType[] types = ActivitiEventType.getTypesFromString(listenersToAdd.getKey());
  			// 然后 根据 listenersToAdd 对象 和type 进行具体类型的事件监听器的注册工作
  			for(ActivitiEventListener listenerToAdd : listenersToAdd.getValue()) {
  				this.eventDispatcher.addEventListener(listenerToAdd, types);
  			}
  		}
  	}
  	
  }
  /*
  初始化 BpmnModel 模型校验器
   */
  protected void initProcessValidator() {
  	if (this.processValidator == null) {
  		this.processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
  	}
  }
  /*
  初始化 日志监听器
  objectMapper  用来 初始JSON格式数据(Activiti默认使用jackson处理 JSON )
  clock  用于设置 和获取时间

  结论:
    如果想要开启日志记录功能,
    1)enableDatabaseEventLogging 需要设置为true
    2)必须开启事件转发功能
   */
  protected void initDatabaseEventLogging() {
    //判断 是否true   如果是 则 开启日志监听
  	if (enableDatabaseEventLogging) {
  		// Database event logging uses the default logging mechanism and adds
  		// a specific event listener to the list of event listeners
  		getEventDispatcher().addEventListener(new EventLogger(clock, objectMapper));
  	}
  }

  // getters and setters //////////////////////////////////////////////////////
  
  public CommandConfig getDefaultCommandConfig() {
    return defaultCommandConfig;
  }
  
  public void setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
    this.defaultCommandConfig = defaultCommandConfig;
  }
  
  public CommandConfig getSchemaCommandConfig() {
    return schemaCommandConfig;
  }
  
  public void setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
    this.schemaCommandConfig = schemaCommandConfig;
  }

  public CommandInterceptor getCommandInvoker() {
    return commandInvoker;
  }
  
  public void setCommandInvoker(CommandInterceptor commandInvoker) {
    this.commandInvoker = commandInvoker;
  }

  public List<CommandInterceptor> getCustomPreCommandInterceptors() {
    return customPreCommandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
    this.customPreCommandInterceptors = customPreCommandInterceptors;
    return this;
  }
  
  public List<CommandInterceptor> getCustomPostCommandInterceptors() {
    return customPostCommandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
    this.customPostCommandInterceptors = customPostCommandInterceptors;
    return this;
  }
  
  public List<CommandInterceptor> getCommandInterceptors() {
    return commandInterceptors;
  }
  
  public ProcessEngineConfigurationImpl setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
    this.commandInterceptors = commandInterceptors;
    return this;
  }
  
  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }
  
  public ProcessEngineConfigurationImpl setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    return this;
  }

  public RepositoryService getRepositoryService() {
    return repositoryService;
  }
  
  public ProcessEngineConfigurationImpl setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
    return this;
  }
  
  public RuntimeService getRuntimeService() {
    return runtimeService;
  }
  
  public ProcessEngineConfigurationImpl setRuntimeService(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
    return this;
  }
  
  public HistoryService getHistoryService() {
    return historyService;
  }
  
  public ProcessEngineConfigurationImpl setHistoryService(HistoryService historyService) {
    this.historyService = historyService;
    return this;
  }
  
  public IdentityService getIdentityService() {
    return identityService;
  }
  
  public ProcessEngineConfigurationImpl setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
    return this;
  }
  
  public TaskService getTaskService() {
    return taskService;
  }
  
  public ProcessEngineConfigurationImpl setTaskService(TaskService taskService) {
    this.taskService = taskService;
    return this;
  }
  
  public FormService getFormService() {
    return formService;
  }
  
  public ProcessEngineConfigurationImpl setFormService(FormService formService) {
    this.formService = formService;
    return this;
  }
  
  public ManagementService getManagementService() {
    return managementService;
  }
  
  public ProcessEngineConfigurationImpl setManagementService(ManagementService managementService) {
    this.managementService = managementService;
    return this;
  }
  
  public DynamicBpmnService getDynamicBpmnService() {
    return dynamicBpmnService;
  }

  public ProcessEngineConfigurationImpl setDynamicBpmnService(DynamicBpmnService dynamicBpmnService) {
    this.dynamicBpmnService = dynamicBpmnService;
    return this;
  }

  public ProcessEngineConfiguration getProcessEngineConfiguration() {
    return this;
  }
  
  public Map<Class< ? >, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setSessionFactories(Map<Class< ? >, SessionFactory> sessionFactories) {
    this.sessionFactories = sessionFactories;
    return this;
  }
  
  public List<ProcessEngineConfigurator> getConfigurators() {
    return configurators;
  }

  public ProcessEngineConfigurationImpl addConfigurator(ProcessEngineConfigurator configurator) {
    if(this.configurators == null) {
      this.configurators = new ArrayList<ProcessEngineConfigurator>();
    }
    this.configurators.add(configurator);
    return this;
  }
  
  public ProcessEngineConfigurationImpl setConfigurators(List<ProcessEngineConfigurator> configurators) {
    this.configurators = configurators;
    return this;
  }

  public void setEnableConfiguratorServiceLoader(boolean enableConfiguratorServiceLoader) {
  	this.enableConfiguratorServiceLoader = enableConfiguratorServiceLoader;
  }

  public List<ProcessEngineConfigurator> getAllConfigurators() {
		return allConfigurators;
  }

	public BpmnDeployer getBpmnDeployer() {
    return bpmnDeployer;
  }

  public ProcessEngineConfigurationImpl setBpmnDeployer(BpmnDeployer bpmnDeployer) {
    this.bpmnDeployer = bpmnDeployer;
    return this;
  }
  
  public BpmnParser getBpmnParser() {
    return bpmnParser;
  }
  
  public ProcessEngineConfigurationImpl setBpmnParser(BpmnParser bpmnParser) {
    this.bpmnParser = bpmnParser;
    return this;
  }

  public List<Deployer> getDeployers() {
    return deployers;
  }
  
  public ProcessEngineConfigurationImpl setDeployers(List<Deployer> deployers) {
    this.deployers = deployers;
    return this;
  }
  
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
  
  public ProcessEngineConfigurationImpl setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }
  
  public String getWsSyncFactoryClassName() {
    return wsSyncFactoryClassName;
  }
  
  public ProcessEngineConfigurationImpl setWsSyncFactoryClassName(String wsSyncFactoryClassName) {
    this.wsSyncFactoryClassName = wsSyncFactoryClassName;
    return this;
  }
  
  /**
   * Add or replace the address of the given web-service endpoint with the given value
   * @param endpointName The endpoint name for which a new address must be set
   * @param address The new address of the endpoint
   */
  public ProcessEngineConfiguration addWsEndpointAddress(QName endpointName, URL address) {
      this.wsOverridenEndpointAddresses.put(endpointName, address);
      return this;
  }
  
  /**
   * Remove the address definition of the given web-service endpoint
   * @param endpointName The endpoint name for which the address definition must be removed
   */
  public ProcessEngineConfiguration removeWsEndpointAddress(QName endpointName) {
      this.wsOverridenEndpointAddresses.remove(endpointName);
      return this;
  }
  
  public ConcurrentMap<QName, URL> getWsOverridenEndpointAddresses() {
      return this.wsOverridenEndpointAddresses;
  }
  
  public ProcessEngineConfiguration setWsOverridenEndpointAddresses(final ConcurrentMap<QName, URL> wsOverridenEndpointAdress) {
    this.wsOverridenEndpointAddresses.putAll(wsOverridenEndpointAdress);
    return this;
  }
  
  public Map<String, FormEngine> getFormEngines() {
    return formEngines;
  }
  
  public ProcessEngineConfigurationImpl setFormEngines(Map<String, FormEngine> formEngines) {
    this.formEngines = formEngines;
    return this;
  }
  
  public FormTypes getFormTypes() {
    return formTypes;
  }
  
  public ProcessEngineConfigurationImpl setFormTypes(FormTypes formTypes) {
    this.formTypes = formTypes;
    return this;
  }
  
  public ScriptingEngines getScriptingEngines() {
    return scriptingEngines;
  }
  
  public ProcessEngineConfigurationImpl setScriptingEngines(ScriptingEngines scriptingEngines) {
    this.scriptingEngines = scriptingEngines;
    return this;
  }
  
  public VariableTypes getVariableTypes() {
    return variableTypes;
  }
  
  public ProcessEngineConfigurationImpl setVariableTypes(VariableTypes variableTypes) {
    this.variableTypes = variableTypes;
    return this;
  }
  
  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }
  
  public ProcessEngineConfigurationImpl setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    return this;
  }
  
  public BusinessCalendarManager getBusinessCalendarManager() {
    return businessCalendarManager;
  }
  
  public ProcessEngineConfigurationImpl setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
    this.businessCalendarManager = businessCalendarManager;
    return this;
  }
  
  public int getExecutionQueryLimit() {
    return executionQueryLimit;
  }

  public ProcessEngineConfigurationImpl setExecutionQueryLimit(int executionQueryLimit) {
    this.executionQueryLimit = executionQueryLimit;
    return this;
  }

  public int getTaskQueryLimit() {
    return taskQueryLimit;
  }

  public ProcessEngineConfigurationImpl setTaskQueryLimit(int taskQueryLimit) {
    this.taskQueryLimit = taskQueryLimit;
    return this;
  }

  public int getHistoricTaskQueryLimit() {
    return historicTaskQueryLimit;
  }

  public ProcessEngineConfigurationImpl setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
    this.historicTaskQueryLimit = historicTaskQueryLimit;
    return this;
  }

  public int getHistoricProcessInstancesQueryLimit() {
    return historicProcessInstancesQueryLimit;
  }

  public ProcessEngineConfigurationImpl setHistoricProcessInstancesQueryLimit(int historicProcessInstancesQueryLimit) {
    this.historicProcessInstancesQueryLimit = historicProcessInstancesQueryLimit;
    return this;
  }

  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
    return this;
  }
  
  public TransactionContextFactory getTransactionContextFactory() {
    return transactionContextFactory;
  }
  
  public ProcessEngineConfigurationImpl setTransactionContextFactory(TransactionContextFactory transactionContextFactory) {
    this.transactionContextFactory = transactionContextFactory;
    return this;
  }
  
  public List<Deployer> getCustomPreDeployers() {
    return customPreDeployers;
  }
  
  public ProcessEngineConfigurationImpl setCustomPreDeployers(List<Deployer> customPreDeployers) {
    this.customPreDeployers = customPreDeployers;
    return this;
  }
  
  public List<Deployer> getCustomPostDeployers() {
    return customPostDeployers;
  }

  public ProcessEngineConfigurationImpl setCustomPostDeployers(List<Deployer> customPostDeployers) {
    this.customPostDeployers = customPostDeployers;
    return this;
  }
  
  public Map<String, JobHandler> getJobHandlers() {
    return jobHandlers;
  }
  
  public ProcessEngineConfigurationImpl setJobHandlers(Map<String, JobHandler> jobHandlers) {
    this.jobHandlers = jobHandlers;
    return this;
  }
  
  public int getAsyncExecutorCorePoolSize() {
		return asyncExecutorCorePoolSize;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorCorePoolSize(int asyncExecutorCorePoolSize) {
		this.asyncExecutorCorePoolSize = asyncExecutorCorePoolSize;
		return this;
	}

	public int getAsyncExecutorMaxPoolSize() {
		return asyncExecutorMaxPoolSize;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorMaxPoolSize(int asyncExecutorMaxPoolSize) {
		this.asyncExecutorMaxPoolSize = asyncExecutorMaxPoolSize;
		return this;
	}

	public long getAsyncExecutorThreadKeepAliveTime() {
		return asyncExecutorThreadKeepAliveTime;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorThreadKeepAliveTime(long asyncExecutorThreadKeepAliveTime) {
		this.asyncExecutorThreadKeepAliveTime = asyncExecutorThreadKeepAliveTime;
		return this;
	}

	public int getAsyncExecutorThreadPoolQueueSize() {
		return asyncExecutorThreadPoolQueueSize;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorThreadPoolQueueSize(int asyncExecutorThreadPoolQueueSize) {
		this.asyncExecutorThreadPoolQueueSize = asyncExecutorThreadPoolQueueSize;
		return this;
	}

	public BlockingQueue<Runnable> getAsyncExecutorThreadPoolQueue() {
		return asyncExecutorThreadPoolQueue;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorThreadPoolQueue(BlockingQueue<Runnable> asyncExecutorThreadPoolQueue) {
		this.asyncExecutorThreadPoolQueue = asyncExecutorThreadPoolQueue;
		return this;
	}

	public long getAsyncExecutorSecondsToWaitOnShutdown() {
		return asyncExecutorSecondsToWaitOnShutdown;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorSecondsToWaitOnShutdown(long asyncExecutorSecondsToWaitOnShutdown) {
		this.asyncExecutorSecondsToWaitOnShutdown = asyncExecutorSecondsToWaitOnShutdown;
		return this;
	}

	public int getAsyncExecutorMaxTimerJobsPerAcquisition() {
		return asyncExecutorMaxTimerJobsPerAcquisition;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorMaxTimerJobsPerAcquisition(int asyncExecutorMaxTimerJobsPerAcquisition) {
		this.asyncExecutorMaxTimerJobsPerAcquisition = asyncExecutorMaxTimerJobsPerAcquisition;
		return this;
	}

	public int getAsyncExecutorMaxAsyncJobsDuePerAcquisition() {
		return asyncExecutorMaxAsyncJobsDuePerAcquisition;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorMaxAsyncJobsDuePerAcquisition(int asyncExecutorMaxAsyncJobsDuePerAcquisition) {
		this.asyncExecutorMaxAsyncJobsDuePerAcquisition = asyncExecutorMaxAsyncJobsDuePerAcquisition;
		return this;
	}

	public int getAsyncExecutorTimerJobAcquireWaitTime() {
		return asyncExecutorDefaultTimerJobAcquireWaitTime;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorDefaultTimerJobAcquireWaitTime(int asyncExecutorDefaultTimerJobAcquireWaitTime) {
		this.asyncExecutorDefaultTimerJobAcquireWaitTime = asyncExecutorDefaultTimerJobAcquireWaitTime;
		return this;
	}

	public int getAsyncExecutorDefaultAsyncJobAcquireWaitTime() {
		return asyncExecutorDefaultAsyncJobAcquireWaitTime;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorDefaultAsyncJobAcquireWaitTime(int asyncExecutorDefaultAsyncJobAcquireWaitTime) {
		this.asyncExecutorDefaultAsyncJobAcquireWaitTime = asyncExecutorDefaultAsyncJobAcquireWaitTime;
		return this;
	}
	
	public int getAsyncExecutorDefaultQueueSizeFullWaitTime() {
    return asyncExecutorDefaultQueueSizeFullWaitTime;
  }

  public ProcessEngineConfigurationImpl setAsyncExecutorDefaultQueueSizeFullWaitTime(int asyncExecutorDefaultQueueSizeFullWaitTime) {
    this.asyncExecutorDefaultQueueSizeFullWaitTime = asyncExecutorDefaultQueueSizeFullWaitTime;
    return this;
  }

  public String getAsyncExecutorLockOwner() {
		return asyncExecutorLockOwner;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorLockOwner(String asyncExecutorLockOwner) {
		this.asyncExecutorLockOwner = asyncExecutorLockOwner;
		return this;
	}

	public int getAsyncExecutorTimerLockTimeInMillis() {
		return asyncExecutorTimerLockTimeInMillis;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorTimerLockTimeInMillis(int asyncExecutorTimerLockTimeInMillis) {
		this.asyncExecutorTimerLockTimeInMillis = asyncExecutorTimerLockTimeInMillis;
		return this;
	}

	public int getAsyncExecutorAsyncJobLockTimeInMillis() {
		return asyncExecutorAsyncJobLockTimeInMillis;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorAsyncJobLockTimeInMillis(int asyncExecutorAsyncJobLockTimeInMillis) {
		this.asyncExecutorAsyncJobLockTimeInMillis = asyncExecutorAsyncJobLockTimeInMillis;
		return this;
	}

	public int getAsyncExecutorLockRetryWaitTimeInMillis() {
		return asyncExecutorLockRetryWaitTimeInMillis;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorLockRetryWaitTimeInMillis(int asyncExecutorLockRetryWaitTimeInMillis) {
		this.asyncExecutorLockRetryWaitTimeInMillis = asyncExecutorLockRetryWaitTimeInMillis;
		return this;
	}
	
	public ExecuteAsyncRunnableFactory getAsyncExecutorExecuteAsyncRunnableFactory() {
		return asyncExecutorExecuteAsyncRunnableFactory;
	}

	public ProcessEngineConfigurationImpl setAsyncExecutorExecuteAsyncRunnableFactory(ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory) {
		this.asyncExecutorExecuteAsyncRunnableFactory = asyncExecutorExecuteAsyncRunnableFactory;
		return this;
	}

	public SqlSessionFactory getSqlSessionFactory() {
    return sqlSessionFactory;
  }
  
  public ProcessEngineConfigurationImpl setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }
  
  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

  public ProcessEngineConfigurationImpl setDbSqlSessionFactory(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    return this;
  }
  
  public TransactionFactory getTransactionFactory() {
    return transactionFactory;
  }

  public ProcessEngineConfigurationImpl setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public List<SessionFactory> getCustomSessionFactories() {
    return customSessionFactories;
  }
  
  public ProcessEngineConfigurationImpl setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
    this.customSessionFactories = customSessionFactories;
    return this;
  }
  
  public List<JobHandler> getCustomJobHandlers() {
    return customJobHandlers;
  }
  
  public ProcessEngineConfigurationImpl setCustomJobHandlers(List<JobHandler> customJobHandlers) {
    this.customJobHandlers = customJobHandlers;
    return this;
  }
  
  public List<FormEngine> getCustomFormEngines() {
    return customFormEngines;
  }
  
  public ProcessEngineConfigurationImpl setCustomFormEngines(List<FormEngine> customFormEngines) {
    this.customFormEngines = customFormEngines;
    return this;
  }

  public List<AbstractFormType> getCustomFormTypes() {
    return customFormTypes;
  }

  public ProcessEngineConfigurationImpl setCustomFormTypes(List<AbstractFormType> customFormTypes) {
    this.customFormTypes = customFormTypes;
    return this;
  }

  public List<String> getCustomScriptingEngineClasses() {
    return customScriptingEngineClasses;
  }
  
  public ProcessEngineConfigurationImpl setCustomScriptingEngineClasses(List<String> customScriptingEngineClasses) {
    this.customScriptingEngineClasses = customScriptingEngineClasses;
    return this;
  }

  public List<VariableType> getCustomPreVariableTypes() {
    return customPreVariableTypes;
  }

  public ProcessEngineConfigurationImpl setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
    this.customPreVariableTypes = customPreVariableTypes;
    return this;
  }
  
  public List<VariableType> getCustomPostVariableTypes() {
    return customPostVariableTypes;
  }

  public ProcessEngineConfigurationImpl setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
    this.customPostVariableTypes = customPostVariableTypes;
    return this;
  }

  public List<BpmnParseHandler> getPreBpmnParseHandlers() {
    return preBpmnParseHandlers;
  }
  
  public ProcessEngineConfigurationImpl setPreBpmnParseHandlers(List<BpmnParseHandler> preBpmnParseHandlers) {
    this.preBpmnParseHandlers = preBpmnParseHandlers;
    return this;
  }
  
  public List<BpmnParseHandler> getCustomDefaultBpmnParseHandlers() {
    return customDefaultBpmnParseHandlers;
  }
  
  public ProcessEngineConfigurationImpl setCustomDefaultBpmnParseHandlers(List<BpmnParseHandler> customDefaultBpmnParseHandlers) {
    this.customDefaultBpmnParseHandlers = customDefaultBpmnParseHandlers;
    return this;
  }

  public List<BpmnParseHandler> getPostBpmnParseHandlers() {
    return postBpmnParseHandlers;
  }

  public ProcessEngineConfigurationImpl setPostBpmnParseHandlers(List<BpmnParseHandler> postBpmnParseHandlers) {
    this.postBpmnParseHandlers = postBpmnParseHandlers;
    return this;
  }

  public ActivityBehaviorFactory getActivityBehaviorFactory() {
    return activityBehaviorFactory;
  }
  
  public ProcessEngineConfigurationImpl setActivityBehaviorFactory(ActivityBehaviorFactory activityBehaviorFactory) {
    this.activityBehaviorFactory = activityBehaviorFactory;
    return this;
  }
  
  public ListenerFactory getListenerFactory() {
    return listenerFactory;
  }

  public ProcessEngineConfigurationImpl setListenerFactory(ListenerFactory listenerFactory) {
    this.listenerFactory = listenerFactory;
    return this;
  }
  
  public BpmnParseFactory getBpmnParseFactory() {
    return bpmnParseFactory;
  }
  
  public ProcessEngineConfigurationImpl setBpmnParseFactory(BpmnParseFactory bpmnParseFactory) {
    this.bpmnParseFactory = bpmnParseFactory;
    return this;
  }

  public Map<Object, Object> getBeans() {
    return beans;
  }

  public ProcessEngineConfigurationImpl setBeans(Map<Object, Object> beans) {
    this.beans = beans;
    return this;
  }
  
  public List<ResolverFactory> getResolverFactories() {
    return resolverFactories;
  }
  
  public ProcessEngineConfigurationImpl setResolverFactories(List<ResolverFactory> resolverFactories) {
    this.resolverFactories = resolverFactories;
    return this;
  }

  public DeploymentManager getDeploymentManager() {
    return deploymentManager;
  }
  
  public ProcessEngineConfigurationImpl setDeploymentManager(DeploymentManager deploymentManager) {
    this.deploymentManager = deploymentManager;
    return this;
  }
    
  public ProcessEngineConfigurationImpl setDelegateInterceptor(DelegateInterceptor delegateInterceptor) {
    this.delegateInterceptor = delegateInterceptor;
    return this;
  }
    
  public DelegateInterceptor getDelegateInterceptor() {
    return delegateInterceptor;
  }
    
  public RejectedJobsHandler getCustomRejectedJobsHandler() {
    return customRejectedJobsHandler;
  }
    
  public ProcessEngineConfigurationImpl setCustomRejectedJobsHandler(RejectedJobsHandler customRejectedJobsHandler) {
    this.customRejectedJobsHandler = customRejectedJobsHandler;
    return this;
  }

  public EventHandler getEventHandler(String eventType) {
    return eventHandlers.get(eventType);
  }
  
  public ProcessEngineConfigurationImpl setEventHandlers(Map<String, EventHandler> eventHandlers) {
    this.eventHandlers = eventHandlers;
    return this;
  }
    
  public Map<String, EventHandler> getEventHandlers() {
    return eventHandlers;
  }
    
  public List<EventHandler> getCustomEventHandlers() {
    return customEventHandlers;
  }
    
  public ProcessEngineConfigurationImpl setCustomEventHandlers(List<EventHandler> customEventHandlers) {
    this.customEventHandlers = customEventHandlers;
    return this;
  }
  
  public FailedJobCommandFactory getFailedJobCommandFactory() {
    return failedJobCommandFactory;
  }
  
  public ProcessEngineConfigurationImpl setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
    this.failedJobCommandFactory = failedJobCommandFactory;
    return this;
  }

  public DataSource getIdGeneratorDataSource() {
    return idGeneratorDataSource;
  }
  
  public ProcessEngineConfigurationImpl setIdGeneratorDataSource(DataSource idGeneratorDataSource) {
    this.idGeneratorDataSource = idGeneratorDataSource;
    return this;
  }
  
  public String getIdGeneratorDataSourceJndiName() {
    return idGeneratorDataSourceJndiName;
  }

  public ProcessEngineConfigurationImpl setIdGeneratorDataSourceJndiName(String idGeneratorDataSourceJndiName) {
    this.idGeneratorDataSourceJndiName = idGeneratorDataSourceJndiName;
    return this;
  }

  public int getBatchSizeProcessInstances() {
    return batchSizeProcessInstances;
  }

  public ProcessEngineConfigurationImpl setBatchSizeProcessInstances(int batchSizeProcessInstances) {
    this.batchSizeProcessInstances = batchSizeProcessInstances;
    return this;
  }
  
  public int getBatchSizeTasks() {
    return batchSizeTasks;
  }
  
  public ProcessEngineConfigurationImpl setBatchSizeTasks(int batchSizeTasks) {
    this.batchSizeTasks = batchSizeTasks;
    return this;
  }
  
  public int getProcessDefinitionCacheLimit() {
    return processDefinitionCacheLimit;
  }

  public ProcessEngineConfigurationImpl setProcessDefinitionCacheLimit(int processDefinitionCacheLimit) {
    this.processDefinitionCacheLimit = processDefinitionCacheLimit;
    return this;
  }
  
  public DeploymentCache<ProcessDefinitionEntity> getProcessDefinitionCache() {
    return processDefinitionCache;
  }
  
  public ProcessEngineConfigurationImpl setProcessDefinitionCache(DeploymentCache<ProcessDefinitionEntity> processDefinitionCache) {
    this.processDefinitionCache = processDefinitionCache;
    return this;
  }

  public int getKnowledgeBaseCacheLimit() {
    return knowledgeBaseCacheLimit;
  }

  public ProcessEngineConfigurationImpl setKnowledgeBaseCacheLimit(int knowledgeBaseCacheLimit) {
    this.knowledgeBaseCacheLimit = knowledgeBaseCacheLimit;
    return this;
  }
  
  public DeploymentCache<Object> getKnowledgeBaseCache() {
    return knowledgeBaseCache;
  }
  
  public ProcessEngineConfigurationImpl setKnowledgeBaseCache(DeploymentCache<Object> knowledgeBaseCache) {
    this.knowledgeBaseCache = knowledgeBaseCache;
    return this;
  }

  public boolean isEnableSafeBpmnXml() {
    return enableSafeBpmnXml;
  }

  public ProcessEngineConfigurationImpl setEnableSafeBpmnXml(boolean enableSafeBpmnXml) {
    this.enableSafeBpmnXml = enableSafeBpmnXml;
    return this;
  }
  
  public ActivitiEventDispatcher getEventDispatcher() {
	  return eventDispatcher;
  }
  
  public void setEventDispatcher(ActivitiEventDispatcher eventDispatcher) {
	  this.eventDispatcher = eventDispatcher;
  }
  
  public void setEnableEventDispatcher(boolean enableEventDispatcher) {
	  this.enableEventDispatcher = enableEventDispatcher;
  }
  
  public void setTypedEventListeners(Map<String, List<ActivitiEventListener>> typedListeners) {
	  this.typedEventListeners = typedListeners;
  }
  
  public void setEventListeners(List<ActivitiEventListener> eventListeners) {
	  this.eventListeners = eventListeners;
  }

	public ProcessValidator getProcessValidator() {
		return processValidator;
	}

	public void setProcessValidator(ProcessValidator processValidator) {
		this.processValidator = processValidator;
	}

	public boolean isEnableEventDispatcher() {
		return enableEventDispatcher;
	}

	public boolean isEnableDatabaseEventLogging() {
		return enableDatabaseEventLogging;
	}

	public ProcessEngineConfigurationImpl setEnableDatabaseEventLogging(boolean enableDatabaseEventLogging) {
		this.enableDatabaseEventLogging = enableDatabaseEventLogging;
    return this;
	}

  public int getMaxLengthStringVariableType() {
    return maxLengthStringVariableType;
  }

  public ProcessEngineConfigurationImpl setMaxLengthStringVariableType(int maxLengthStringVariableType) {
    this.maxLengthStringVariableType = maxLengthStringVariableType;
    return this;
  }
  
	public ProcessEngineConfigurationImpl setBulkInsertEnabled(boolean isBulkInsertEnabled) {
		this.isBulkInsertEnabled = isBulkInsertEnabled;
		return this;
	}

	public boolean isBulkInsertEnabled() {
		return isBulkInsertEnabled;
	}

	public int getMaxNrOfStatementsInBulkInsert() {
		return maxNrOfStatementsInBulkInsert;
	}

	public ProcessEngineConfigurationImpl setMaxNrOfStatementsInBulkInsert(int maxNrOfStatementsInBulkInsert) {
		this.maxNrOfStatementsInBulkInsert = maxNrOfStatementsInBulkInsert;
		return this;
	}
	
  public DelegateExpressionFieldInjectionMode getDelegateExpressionFieldInjectionMode() {
    return delegateExpressionFieldInjectionMode;
  }

  public void setDelegateExpressionFieldInjectionMode(DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode) {
    this.delegateExpressionFieldInjectionMode = delegateExpressionFieldInjectionMode;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }
}
