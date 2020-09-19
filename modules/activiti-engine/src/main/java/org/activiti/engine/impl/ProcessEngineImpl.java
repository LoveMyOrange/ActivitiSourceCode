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
package org.activiti.engine.impl;

import java.util.Map;

import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionContextFactory;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @desc  对ProcessEngine接口中定义的() 进行实现
 *
 */
public class ProcessEngineImpl implements ProcessEngine {

  private static Logger log = LoggerFactory.getLogger(ProcessEngineImpl.class);

  protected String name;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected HistoryService historicDataService;
  protected IdentityService identityService;
  protected TaskService taskService;
  protected FormService formService;
  protected ManagementService managementService;
  protected DynamicBpmnService dynamicBpmnService;
  protected JobExecutor jobExecutor;
  protected AsyncExecutor asyncExecutor;
  protected CommandExecutor commandExecutor;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  protected ExpressionManager expressionManager;
  protected TransactionContextFactory transactionContextFactory;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
/*
  初始化流程引擎  -->  ProcessEngineConfigurationImpl类的 buildProcessEngine() 的第2行代码
  new ProcessEngineImpl(this);
  ProcessEngineImpl 对象的属性值 都从 ProcessEngineConfigurationImpl 对象中获取
  这是典型的门面设计模式, 为客户端使用提供便利, 无需关心  ProcessEngineConfiguration内部实现机制
 */
  public ProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration; //流程引擎配置 实例
    this.name = processEngineConfiguration.getProcessEngineName();//流程引擎名称
    //初始化各种服务类实例
    this.repositoryService = processEngineConfiguration.getRepositoryService();
    this.runtimeService = processEngineConfiguration.getRuntimeService();
    this.historicDataService = processEngineConfiguration.getHistoryService();
    this.identityService = processEngineConfiguration.getIdentityService();
    this.taskService = processEngineConfiguration.getTaskService();
    this.formService = processEngineConfiguration.getFormService();
    this.managementService = processEngineConfiguration.getManagementService();
    this.dynamicBpmnService = processEngineConfiguration.getDynamicBpmnService();
    //
    this.jobExecutor = processEngineConfiguration.getJobExecutor();//定时作业执行器
    this.asyncExecutor = processEngineConfiguration.getAsyncExecutor();//异步作业执行器
    this.commandExecutor = processEngineConfiguration.getCommandExecutor();//命令执行器
    this.sessionFactories = processEngineConfiguration.getSessionFactories();//sessionFactory
    this.transactionContextFactory = processEngineConfiguration.getTransactionContextFactory();//事务上下文工厂
    /*
     执行DB 表生成策略, 开发人员为流程引擎配置类 设置 databaseSchemaUpdate 属性
     false 默认值 ,流程引擎启动时, 首先从ACT_GE_PROERTY 中查询Activiti引擎版本值 NAME_指端的值 等于schema.version
     然后获取ProcessEngine接口中定义的VERSION 静态变量值
     两者进行对比, 如果DB中的表不存在或者表存在 但是版本不匹配则直接抛出异常

     true 流程引擎启动时会对所有的表进行更新操作, (upgrade目录中的DDL脚本
     如果DB中的表不存在则开始建表, (create目录中的 DDL脚本)

     create-drop  流程引擎启动时建表, 流程引擎关闭时删除表(流程引擎的关闭 形如: processEngine.close()
     drop-create 流程引擎启动时首先删除DB中的表 然后重新创建表,(该方式不需要手动关闭流程引擎,)  该操作非常危险,不建议正式环境使用
     create 流程引擎启动时 不管DB是否存在表   都创建表 (意味着如果DB中已经存在表, 再次执行创建表的DDL 肯定报错, 因此不建议使用 )
     */

    commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationsProcessEngineBuild());
    if (name == null) {
      log.info("default activiti ProcessEngine created");
    } else {
      log.info("ProcessEngine {} created", name);
    }
    /*
    注册流程引擎  此() 将 此类 注册到  ProcessEngines中
     */
    ProcessEngines.registerProcessEngine(this);
  /*
    作业执行器
   */
    if (jobExecutor != null && jobExecutor.isAutoActivate()) {
      jobExecutor.start();
    }
    /*
    * 异步作业执行器
    * */
    if (asyncExecutor != null && asyncExecutor.isAutoActivate()) {
      asyncExecutor.start();
    }
    /*
    流程引擎声明周期监听器
    如果配置了  则此行代码会触发   中的onProcessEngineBuilt ()
     */
    if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
      processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineBuilt(this);
    }
    /*
    转发事件 ENGINE_CREATED
     */
    processEngineConfiguration.getEventDispatcher().dispatchEvent(
    		ActivitiEventBuilder.createGlobalEvent(ActivitiEventType.ENGINE_CREATED));
  }
  /*
  关闭流程引擎
  如果使用 StandloneProcessEngineConfiguration 对象 ,需要手动调用流程引擎的close()
  如果使用 ProcessEngineFactoryBean 类构造流程引擎, 则无需关心 close()
  具体实现逻辑可以跟进该类的 destory()
   */
  public void close() {
    ProcessEngines.unregister(this);//注销流程引擎实例
    //关闭执行器
    if (jobExecutor != null && jobExecutor.isActive()) {
      jobExecutor.shutdown();
    }
    /*
    如果流程引擎配置类 配置了作业执行器 jobExecutorActivate () 和异步作业执行器 asyncExecutorActivate
     */
    if (asyncExecutor != null && asyncExecutor.isActive()) {
      asyncExecutor.shutdown();
    }
  //执行SchemaOperationProcessEngineClose 命令
    commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationProcessEngineClose());
    //执行 流程引擎声明周期监听器,,,  onProcessEngineClosed()
    if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
      processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineClosed(this);
    }
    //转发 ENGINE_CLOSED 事件
    processEngineConfiguration.getEventDispatcher().dispatchEvent(
    		ActivitiEventBuilder.createGlobalEvent(ActivitiEventType.ENGINE_CLOSED));
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public IdentityService getIdentityService() {
    return identityService;
  }

  public ManagementService getManagementService() {
    return managementService;
  }

  public TaskService getTaskService() {
    return taskService;
  }

  public HistoryService getHistoryService() {
    return historicDataService;
  }

  public RuntimeService getRuntimeService() {
    return runtimeService;
  }
  
  public RepositoryService getRepositoryService() {
    return repositoryService;
  }
  
  public FormService getFormService() {
    return formService;
  }
  
  public DynamicBpmnService getDynamicBpmnService() {
    return dynamicBpmnService;
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }
}
