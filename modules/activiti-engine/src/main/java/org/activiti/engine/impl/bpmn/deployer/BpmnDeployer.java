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
package org.activiti.engine.impl.bpmn.deployer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.UserTask;
import org.activiti.bpmn.model.ValuedDataObject;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.DynamicBpmnConstants;
import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.activiti.engine.impl.cfg.IdGenerator;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cmd.CancelJobsCmd;
import org.activiti.engine.impl.cmd.DeploymentSettings;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.event.MessageEventHandler;
import org.activiti.engine.impl.event.SignalEventHandler;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.activiti.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntity;
import org.activiti.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.task.IdentityLinkType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * 默认的Bpmn部署器
 */
public class BpmnDeployer implements Deployer {

  private static final Logger log = LoggerFactory.getLogger(BpmnDeployer.class);

  public static final String[] BPMN_RESOURCE_SUFFIXES = new String[] { "bpmn20.xml", "bpmn" };
  public static final String[] DIAGRAM_SUFFIXES = new String[]{"png", "jpg", "gif", "svg"};

  protected ExpressionManager expressionManager;
  protected BpmnParser bpmnParser;
  protected IdGenerator idGenerator;
  /*
  流程部署的整个过程
    此() 看起来 相当复杂,  做了很多工作   承载了太多的功能实现,
    为何不将该()  处理逻辑 分散到不同的() 中  这样看起来会更加清晰明了, 每一层逻辑更加单一, 容易理解,
    这可能是Activiti 需要优化的地方
   */
  public void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings) {
    log.debug("Processing deployment {}", deployment.getName());
    //实例化存储 流程定义实体的集合
    List<ProcessDefinitionEntity> processDefinitions = new ArrayList<ProcessDefinitionEntity>();
    //根据部署对象获取部署实体
    Map<String, ResourceEntity> resources = deployment.getResources();
    // giao集合 存储 BpmnMOdel 类型的元素
    Map<String, BpmnModel> bpmnModelMap = new HashMap<String, BpmnModel>();
    //获取流程引擎配置类
    final ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    for (String resourceName : resources.keySet()) {
      log.info("Processing resource {}", resourceName);
      //验证流程资源名称的 后缀 是不是bpmn20.xml 或者bpmn
      if (isBpmnResource(resourceName)) {
        //根据resourceName 获的resource 对象
        ResourceEntity resource = resources.get(resourceName);
        byte[] bytes = resource.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        /*
        实例化 BpmnParse 类 ,并且为 实例填充 属性值
        此类 很重要
        负责将 流程文档解析之后的 BaseElement 对象 再次解析 并且注入 PVM 中
         */
        BpmnParse bpmnParse = bpmnParser
          .createParse()
          .sourceInputStream(inputStream)
          .setSourceSystemId(resourceName)
          .deployment(deployment)
          .name(resourceName);
        /*
        验证是否为null
        如果不是null, 则 设置 不需要进行流程文档的验证工作 ,
        该判断操作很重要
        例如 流程定义缓存丢失 ,则程序 再次对流程文档解析时, 就没有必要 对其进行验证了

         */
        if (deploymentSettings != null) {  //
        	//验证Schema
        	if (deploymentSettings.containsKey(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED)) {
        		bpmnParse.setValidateSchema((Boolean) deploymentSettings.get(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED));
        	}
        	
        	if (deploymentSettings.containsKey(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED)) {
        		bpmnParse.setValidateProcess((Boolean) deploymentSettings.get(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED));
        	}
        	
        } else {
          // 设置属性
        	bpmnParse.setValidateSchema(false);
        	bpmnParse.setValidateProcess(false);
        }
        /*
        负责将流程文档中的元素解析并且封装为 Activiti 中内部表示 BaseElement 实例
        然后再次解析 BaseElement 对象 并且将其注入PVM
         */
        bpmnParse.execute();
        //循环遍历bpmnParse 对象中的processDefinitions集合
        for (ProcessDefinitionEntity processDefinition: bpmnParse.getProcessDefinitions()) {
          processDefinition.setResourceName(resourceName); //设置资源名称
          
          if (deployment.getTenantId() != null) {
          	processDefinition.setTenantId(deployment.getTenantId()); // 设置租户 id
          }
          
          String diagramResourceName = getDiagramResourceForProcess(resourceName, processDefinition.getKey(), resources); //获取流程文档的图片名称
                   
          // Only generate the resource when deployment is new to prevent modification of deployment resources 
          // after the process-definition is actually deployed. Also to prevent resource-generation failure every
          // time the process definition is added to the deployment-cache when diagram-generation has failed the first time.
          if(deployment.isNew()) { //判断流程文档 是否需要重新部署, 则需要根据流程文档信息生成图片,并将其添加到会话缓存中
            /*
            省略生成图片的代码  如果生成图片有中文乱码的情况 可以查看图片的处理逻辑

            Activiti 使用 Java 的Graphics2D  绘制图片,
            关于图片的生成图片 DefaultProcessDiagramCanvas 进行查看

             */
            if (processEngineConfiguration.isCreateDiagramOnDeploy() &&
                  diagramResourceName==null && processDefinition.isGraphicalNotationDefined()) {
              try {
                  byte[] diagramBytes = IoUtil.readInputStream(processEngineConfiguration.
                    getProcessDiagramGenerator().generateDiagram(bpmnParse.getBpmnModel(), "png", processEngineConfiguration.getActivityFontName(),
                        processEngineConfiguration.getLabelFontName(),processEngineConfiguration.getAnnotationFontName(), processEngineConfiguration.getClassLoader()), null);
                  diagramResourceName = getProcessImageResourceName(resourceName, processDefinition.getKey(), "png");
                  createResource(diagramResourceName, diagramBytes, deployment);
              } catch (Throwable t) { // if anything goes wrong, we don't store the image (the process will still be executable).
                log.warn("Error while generating process diagram, image will not be stored in repository", t);
              }
            } 
          }
          
          processDefinition.setDiagramResourceName(diagramResourceName);//设置 属性值
          processDefinitions.add(processDefinition);
          /*
                  以上 所有操作完毕 将相应的信息 添加 到 rocessDefinitions 集合 和bpmnModelMap 中
                  processDefintions 集合中是否有值 决定了 程序是否可以进行下一步的处理
           */
          bpmnModelMap.put(processDefinition.getKey(), bpmnParse.getBpmnModel());
        }
      }
    }
    
    // check if there are process definitions with the same process key to prevent database unique index violation
    List<String> keyList = new ArrayList<String>();
    /*
    遍历processDefinitions集合
    将processDefinition对象中的key 添加到keyList中
    添加是 如果发现 该key值 已经存在 keyList 集合中, 程序直接报错
     */
    for (ProcessDefinitionEntity processDefinition : processDefinitions) {
      //
      if (keyList.contains(processDefinition.getKey())) {
        throw new ActivitiException("The deployment contains process definitions with the same key '"+ processDefinition.getKey() +"' (process id atrribute), this is not allowed");
      }
      keyList.add(processDefinition.getKey());//将processDefinition对象的key值 添加到keyList集合中
    }
    
    CommandContext commandContext = Context.getCommandContext();
    //获取processDefinitionManager对象  此类负责 ACT_RE_PROCDEF 表的操作
    ProcessDefinitionEntityManager processDefinitionManager = commandContext.getProcessDefinitionEntityManager();
    //获取DBSQLSession 对象,  负责会话缓存 的CRUD 操作
    DbSqlSession dbSqlSession = commandContext.getSession(DbSqlSession.class);
    /*
    关于 计算流程定义版本值
       首先 根据 流程定义key  或者 tenantId 作为查询条件
       从ACT_RE_PROCDEF 表中查询数据
       如果 租户id 不存在 或者为空
       委托ProcessDefinitionManager 类中的 findLatestProcessDefinitionByKey 查询数据
       否则 使用  findLatestProcessDefinitionByKeyAndTenantId 查询数据

       不管程序用的是哪个() 查询数据
       都是通过MyBatis 查询ACT_RE_PROCDEF 表

       查询完成之后 开始计算 版本只
       例如 当前 key值为 java 并且 版本为2  则当前的流程定义版本 +1 操作
       如果 java key并不存在 则 默认值为1


    关于  生成 流程定义ID 的 值  对应 act_re_procdef 表的主键ID 值
    id值的策略为
         String nextId = idGenerator.getNextId(); //使用id生成器生成的id
        String processDefinitionId = processDefinition.getKey()
          + ":" + processDefinition.getVersion()
          + ":" + nextId; // ACT-505

          如果id 值 长度>64  直接使用id生成器生成的值作为id值

          为什么id 值必须  <64    因为 act_re_procdef 表中的id_列 varchar 并且最大长度 为64
          所以程序必须限制  数据插入到DB时 程序报错

          如果 不受限制 ?
          实现思路为
          1) 考虑如何修改deploy()中 id 值的生成逻辑
          2) 需要修改ACT_RE_PROCDEF 表 ID_ 列长度, 但还不够  其他外键约束列 都需要进行修改
     */
    for (ProcessDefinitionEntity processDefinition : processDefinitions) {
      List<TimerEntity> timers = new ArrayList<TimerEntity>();
      if (deployment.isNew()) {
        int processDefinitionVersion; //存储流程定义版本号

        ProcessDefinitionEntity latestProcessDefinition = null;
        //如果租户不是null
        if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
        	latestProcessDefinition = processDefinitionManager
        			.findLatestProcessDefinitionByKeyAndTenantId(processDefinition.getKey(), processDefinition.getTenantId());
        } else {
        	latestProcessDefinition = processDefinitionManager
        			.findLatestProcessDefinitionByKey(processDefinition.getKey());
        }
        		
        if (latestProcessDefinition != null) { //如果该key的 流程文档在DB中已经存在 则版本+1
          processDefinitionVersion = latestProcessDefinition.getVersion() + 1;
        } else {
          processDefinitionVersion = 1; //版本从1 开始算
        }

        processDefinition.setVersion(processDefinitionVersion); //设置版本值
        processDefinition.setDeploymentId(deployment.getId());//设置部署ID 值

        String nextId = idGenerator.getNextId(); //使用id生成器生成的id
        String processDefinitionId = processDefinition.getKey() 
          + ":" + processDefinition.getVersion()
          + ":" + nextId; // ACT-505
                   
        // ACT-115: maximum id length is 64 charcaters
        if (processDefinitionId.length() > 64) {      //如果流程定义的id 值 > 64 需要进行值的 截取
          processDefinitionId = nextId; 
        }
        processDefinition.setId(processDefinitionId);//设置流程定义ID
        //转发 ENTITY_CREATED 事件
        if(commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        	commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
        			ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED, processDefinition));
        }

        removeObsoleteTimers(processDefinition); //移除过期定时作业
        addTimerDeclarations(processDefinition, timers);//添加定时作业
        /*
        和上述定时作业的移除和添加 逻辑 大致一样,  具体 点进去 上面 ()  里面有注释

        这两个() 都是处理消息

        不管是消息 启动 节点 还是 信号 启动节点,  处理流程 都是首先移除 已经存在的 或者过期的数据
        然后重新添加新的数据 ,该操作针对的 ACT_RU_EVENT_SUBSCR 表
        信号启动 和消息启动唯一的区别就是 EVENT_TYPE_列 类型不同
        消息启动类型为 message
        信号启动类型为 signal
         */
        removeExistingMessageEventSubscriptions(processDefinition, latestProcessDefinition); //移除
        addMessageEventSubscriptions(processDefinition);//添加消息事件
        
        removeExistingSignalEventSubScription(processDefinition, latestProcessDefinition); //
        addSignalEventSubscriptions(processDefinition);//添加信号事件

        dbSqlSession.insert(processDefinition); //添加到会话缓存
//
        addAuthorizations(processDefinition);//将流程启动人(也就是谁能启动这个流程) 信息 添加到会话缓存
      //转发  ENTITY_INITIALIZED
        if(commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        	commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
        			ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_INITIALIZED, processDefinition));
        }

        scheduleTimers(timers); //调度定时作业

      } else {
        String deploymentId = deployment.getId();
        processDefinition.setDeploymentId(deploymentId); //设置 部署 ID
        
        ProcessDefinitionEntity persistedProcessDefinition = null; 
        if (processDefinition.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
        	persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinition.getKey());
        } else {
        	persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinition.getKey(), processDefinition.getTenantId());
        }
        
        if (persistedProcessDefinition != null) {
        	processDefinition.setId(persistedProcessDefinition.getId());
        	processDefinition.setVersion(persistedProcessDefinition.getVersion());
        	processDefinition.setSuspensionState(persistedProcessDefinition.getSuspensionState());
        }
      }

      // Add to cache
      DeploymentManager deploymentManager = processEngineConfiguration.getDeploymentManager();
      //将processDefinition 添加到 缓存中
      deploymentManager.getProcessDefinitionCache().add(processDefinition.getId(), processDefinition);
      // 添加 流程节点缓存
      addDefinitionInfoToCache(processDefinition, processEngineConfiguration, commandContext);

      deployment.addDeployedArtifact(processDefinition);
      //操作节点缓存
      createLocalizationValues(processDefinition.getId(), bpmnModelMap.get(processDefinition.getKey()).getProcessById(processDefinition.getKey()));
    }
  }

  /**
   *
   * @param processDefinition
   * @param processEngineConfiguration
   * @param commandContext
   *  初始化节点缓存数据
   */
  protected void addDefinitionInfoToCache(ProcessDefinitionEntity processDefinition, 
      ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {
    
    if (processEngineConfiguration.isEnableProcessDefinitionInfoCache() == false) {
      return; //如果没有开启流程定义节点缓存, 则不会 进行处理 直接return
    }
    
    DeploymentManager deploymentManager = processEngineConfiguration.getDeploymentManager();
    ProcessDefinitionInfoEntityManager definitionInfoEntityManager = commandContext.getProcessDefinitionInfoEntityManager();
    ObjectMapper objectMapper = commandContext.getProcessEngineConfiguration().getObjectMapper();
    //根据processDefinition 对象的id 值 ,从ACT_PROCDEF_INFO 表中查询数据
    ProcessDefinitionInfoEntity definitionInfoEntity = definitionInfoEntityManager.findProcessDefinitionInfoByProcessDefinitionId(processDefinition.getId());
    
    ObjectNode infoNode = null;
    if (definitionInfoEntity != null && definitionInfoEntity.getInfoJsonId() != null) {
      //从ACT_GE_BYTEARRAY 表中获取数据
      byte[] infoBytes = definitionInfoEntityManager.findInfoJsonById(definitionInfoEntity.getInfoJsonId());
      if (infoBytes != null) {  //如果获取到数据
        try {
          infoNode = (ObjectNode) objectMapper.readTree(infoBytes); //将infoBytes  转换为infoNode
        } catch (Exception e) {
          throw new ActivitiException("Error deserializing json info for process definition " + processDefinition.getId());
        }
      }
    }
    //实例化 此类 填充属性值
    ProcessDefinitionInfoCacheObject definitionCacheObject = new ProcessDefinitionInfoCacheObject();
    if (definitionInfoEntity == null) {
      definitionCacheObject.setRevision(0);
    } else {
      definitionCacheObject.setId(definitionInfoEntity.getId());
      definitionCacheObject.setRevision(definitionInfoEntity.getRevision());
    }
    
    if (infoNode == null) {
      infoNode = objectMapper.createObjectNode(); //创建 根节点 保证infoNode对象不为空
    }
    definitionCacheObject.setInfoNode(infoNode);

    //将数据添加到 缓存中
    deploymentManager.getProcessDefinitionInfoCache().add(processDefinition.getId(), definitionCacheObject);
    /*
    上述的操作 仅仅是根据 processDefinition对象的id值 从DB中查询数据 并将其到缓存中,
    并没有 对流程文档 中定义的节点缓存 进行解析 和入库,
    引擎为何这样设计, 处于何种考虑   和 createLocalizationValues() 有关系
     */
  }
  /*
  如果  addTimerDeclarations() timers 中有值 ,然后会进行处理

  作业 是如何被记录到DB 并且执行呢???
      此()
   */
  protected void scheduleTimers(List<TimerEntity> timers) {
    for (TimerEntity timer : timers) {
      Context
        .getCommandContext()
        .getJobEntityManager()
        .schedule(timer);
    }
  }

  @SuppressWarnings("unchecked")
  /*
  此()操作的表 是 ACT_RU_JOB
  如果 该集合 为null 则不予处理

   */
  protected void addTimerDeclarations(ProcessDefinitionEntity processDefinition, List<TimerEntity> timers) {
    List<TimerDeclarationImpl> timerDeclarations = (List<TimerDeclarationImpl>) processDefinition.getProperty(BpmnParse.PROPERTYNAME_START_TIMER);
    if (timerDeclarations!=null) {
      for (TimerDeclarationImpl timerDeclaration : timerDeclarations) {
        //狗挨揍timeer对象
        TimerEntity timer = timerDeclaration.prepareTimerEntity(null);
        if (timer!=null) {
          //属性填充
          timer.setProcessDefinitionId(processDefinition.getId());
	        
          // Inherit timer (if appliccable)
          if (processDefinition.getTenantId() != null) {
            timer.setTenantId(processDefinition.getTenantId());
          }
          //添加到timers 集合中  ,如果 timers 集合中存在值 则 scheduleTimers(timers) 会对该集合进行处理
          timers.add(timer);
        }
      }
    }
  }
  /*
  移除 过期 作业
   主要针对于 配置了 定时作业的流程

   为何 部署流程文档 需要 调用此() 呢????
         比如 :

         当开发人员 部署了一个key 为  java的 流程文档 ,并且制定了 giao流程 1天之后启动
         则该流程文档部署完毕版本为1
         如果在该流程等待启动时间内,
         开发人员再次部署该流程文档并且重新设置启动时间为2天  则 该文档部署之后的版本为2

         这里会出现一个问题
         因为流程定义key 相同 并且 多个版本共存的情况下,
         流程实例默认 是按照 最高版本启动的

         这时 第一个版本的流程实例 是否需要再指定时间启动  意义已经不大
         只需要 让 第二个版本 流程文档 在指定时间启动即可

         对于该场景来说

         第一个版本的定义 作业 需要通过   removeObsoleteTimers()  删除
         并且通过   addTimerDeclarations 重新添加第2个版本的定时作业
   */
  protected void removeObsoleteTimers(ProcessDefinitionEntity processDefinition) {
  	
  	List<Job> jobsToDelete = null;
  	  //判断  然后 根据不同的 判断条件 查询DB 中 ACT_RU_JOB表中的数据 然后使用 jobsToDelete 集合存储查询之后的数据
  	if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
  		jobsToDelete = Context.getCommandContext().getJobEntityManager().findJobsByTypeAndProcessDefinitionKeyAndTenantId(
  				TimerStartEventJobHandler.TYPE, processDefinition.getKey(), processDefinition.getTenantId());
    } else {
    	jobsToDelete = Context.getCommandContext().getJobEntityManager()
    			.findJobsByTypeAndProcessDefinitionKeyNoTenantId(TimerStartEventJobHandler.TYPE, processDefinition.getKey());
    }
  //判null
  	if (jobsToDelete != null) {
  	  //如果不为null 循环调用  CancelJobsCmd 移除 作业(删除ACT_RU_JOB 表中的查询的数据 )
	    for (Job job :jobsToDelete) {
	        new CancelJobsCmd(job.getId()).execute(Context.getCommandContext());
	    }
  	}
  }
  
  protected void removeExistingMessageEventSubscriptions(ProcessDefinitionEntity processDefinition, ProcessDefinitionEntity latestProcessDefinition) {
    if(latestProcessDefinition != null) {
      CommandContext commandContext = Context.getCommandContext();
      
      List<EventSubscriptionEntity> subscriptionsToDisable = commandContext
        .getEventSubscriptionEntityManager()
        .findEventSubscriptionsByTypeAndProcessDefinitionId(MessageEventHandler.EVENT_HANDLER_TYPE, latestProcessDefinition.getId(), latestProcessDefinition.getTenantId());
      
      for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsToDisable) {
        eventSubscriptionEntity.delete();        
      } 
      
    }
  }
  
  @SuppressWarnings("unchecked")
  /*

  * */
  protected void addMessageEventSubscriptions(ProcessDefinitionEntity processDefinition) {
    CommandContext commandContext = Context.getCommandContext();
    List<EventSubscriptionDeclaration> eventDefinitions = (List<EventSubscriptionDeclaration>) processDefinition.getProperty(BpmnParse.PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION);
    if(eventDefinitions != null) {  
      
      Set<String> messageNames = new HashSet<String>();
      for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
        if(eventDefinition.getEventType().equals("message") && eventDefinition.isStartEvent()) {
          
          if (!messageNames.contains(eventDefinition.getEventName())) {
            messageNames.add(eventDefinition.getEventName());
          } else {
            throw new ActivitiException("Cannot deploy process definition '" + processDefinition.getResourceName()
                + "': there are multiple message event subscriptions for the message with name '" + eventDefinition.getEventName() + "'.");
          }
          
          // look for subscriptions for the same name in db:
          List<EventSubscriptionEntity> subscriptionsForSameMessageName = commandContext
            .getEventSubscriptionEntityManager()
            .findEventSubscriptionsByName(MessageEventHandler.EVENT_HANDLER_TYPE, 
            		eventDefinition.getEventName(), processDefinition.getTenantId());
          
          // also look for subscriptions created in the session:
          List<MessageEventSubscriptionEntity> cachedSubscriptions = commandContext
            .getDbSqlSession()
            .findInCache(MessageEventSubscriptionEntity.class);
          for (MessageEventSubscriptionEntity cachedSubscription : cachedSubscriptions) {
            if(eventDefinition.getEventName().equals(cachedSubscription.getEventName())
                    && !subscriptionsForSameMessageName.contains(cachedSubscription)) {
              subscriptionsForSameMessageName.add(cachedSubscription);
            }
          }
          
          // remove subscriptions deleted in the same command
          subscriptionsForSameMessageName = commandContext
                  .getDbSqlSession()
                  .pruneDeletedEntities(subscriptionsForSameMessageName);
                
          for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsForSameMessageName) {
            // throw exception only if there's already a subscription as start event
            
            // no process instance-id = it's a message start event
            if(StringUtils.isEmpty(eventSubscriptionEntity.getProcessInstanceId())) {
              throw new ActivitiException("Cannot deploy process definition '" + processDefinition.getResourceName()
                      + "': there already is a message event subscription for the message with name '" + eventDefinition.getEventName() + "'.");
            }
          }
          
          MessageEventSubscriptionEntity newSubscription = new MessageEventSubscriptionEntity();
          newSubscription.setEventName(eventDefinition.getEventName());
          newSubscription.setActivityId(eventDefinition.getActivityId());
          newSubscription.setConfiguration(processDefinition.getId());
          newSubscription.setProcessDefinitionId(processDefinition.getId());

          if (processDefinition.getTenantId() != null) {
          	newSubscription.setTenantId(processDefinition.getTenantId());
          }
          
          newSubscription.insert();
        }
      }
    }      
  }
  
  protected void removeExistingSignalEventSubScription(ProcessDefinitionEntity processDefinition, ProcessDefinitionEntity latestProcessDefinition) {
    if(latestProcessDefinition != null) {
      CommandContext commandContext = Context.getCommandContext();
      
      List<EventSubscriptionEntity> subscriptionsToDisable = commandContext
        .getEventSubscriptionEntityManager()
        .findEventSubscriptionsByTypeAndProcessDefinitionId(SignalEventHandler.EVENT_HANDLER_TYPE, latestProcessDefinition.getId(), latestProcessDefinition.getTenantId());
      
      for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsToDisable) {
        eventSubscriptionEntity.delete();  
      } 
      
    }
  }
  
  @SuppressWarnings("unchecked")
  protected void addSignalEventSubscriptions(ProcessDefinitionEntity processDefinition) {
     List<EventSubscriptionDeclaration> eventDefinitions = (List<EventSubscriptionDeclaration>) processDefinition.getProperty(BpmnParse.PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION);
     if(eventDefinitions != null) {     
       for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
         if(eventDefinition.getEventType().equals("signal") && eventDefinition.isStartEvent()) {
        	 
        	 SignalEventSubscriptionEntity subscriptionEntity = new SignalEventSubscriptionEntity();
        	 subscriptionEntity.setEventName(eventDefinition.getEventName());
        	 subscriptionEntity.setActivityId(eventDefinition.getActivityId());
        	 subscriptionEntity.setProcessDefinitionId(processDefinition.getId());
        	 if (processDefinition.getTenantId() != null) {
        		 subscriptionEntity.setTenantId(processDefinition.getTenantId());
           }
        	 subscriptionEntity.insert();
        	 
         }	 
       }
     }      
  }
  /*
  更新节点缓存的()

   activiti:localization 元素 暂时支持的属性有 id ,name locale
   支持的子元素只有 documentation 只有process userTask, subProcess ,dataObject 等元素支持节点缓存功能

   最后 一行有Bug
   saveProcessDefinitionInfo() 只会将数据更新到DB 并不会更新缓存  显然是Activiti一个Bug

   */
  protected void createLocalizationValues(String processDefinitionId, Process process) {
    if (process == null) return;
    //获取命令上下文对象
    CommandContext commandContext = Context.getCommandContext();
    DynamicBpmnService dynamicBpmnService = commandContext.getProcessEngineConfiguration().getDynamicBpmnService();
    //根据流程定义ID 从 ACT_PROCDEF_INFO 表中查询数据
    ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinitionId);

    boolean localizationValuesChanged = false;//记录节点信息是否发生变化
    // 直解析localization 元素     其他的元素 引擎不会处理
    List<ExtensionElement> localizationElements = process.getExtensionElements().get("localization");
    if (localizationElements != null) {
      //如果 扩展元素中 有localization 子元素 则开始获取
      for (ExtensionElement localizationElement : localizationElements) { //循环遍历元素
        //通过下面的操作 可以看出命名空间必须是activiti 才可以
        if (BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {
          //获取locale
          String locale = localizationElement.getAttributeValue(null, "locale");
          //获取name值
          String name = localizationElement.getAttributeValue(null, "name");
          String documentation = null; //获取documentation值
          //
          List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
          if (documentationElements != null) {  //如果查找到就 break
            for (ExtensionElement documentationElement : documentationElements) {
              documentation = StringUtils.trimToNull(documentationElement.getElementText());
              break; //如果配置了 多个 documentation 元素 只会执行一次
            }
          }

          String processId = process.getId(); //流程id
          //如果查询到的数据与缓存中的数据不一致 则需要修改
          if (isEqualToCurrentLocalizationValue(locale, processId, "name", name, infoNode) == false) {
            //更新infoNode对象 (将两者数据 合并 )
            dynamicBpmnService.changeLocalizationName(locale, processId, name, infoNode);
            localizationValuesChanged = true;
          }
          
          if (documentation != null && isEqualToCurrentLocalizationValue(locale, processId, "description", documentation, infoNode) == false) {
            //需要修改描述信息 description     //更新infoNode对象 (将两者数据 合并 )
            dynamicBpmnService.changeLocalizationDescription(locale, processId, documentation, infoNode);
            localizationValuesChanged = true;
          }
          
          break; // for循环执行一次之后 break ,说明如果存在多个兄弟localization 元素 只会执行一次  也即只会解析第一个localization元素
        }
      }
    }
    //比对UserTask SubProcess 元素中的数据是否发生变化
    boolean isFlowElementLocalizationChanged = localizeFlowElements(process.getFlowElements(), infoNode);
    //比如 dataObject 元素中的数据 是否发生变化
    boolean isDataObjectLocalizationChanged = localizeDataObjectElements(process.getDataObjects(), infoNode);
    if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
      localizationValuesChanged = true;
    }
    //
    if (localizationValuesChanged) { //将infoNode更新到DB,并不会更新缓存,这是Activiti的一个bug
      dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);
    }
  }
  
  protected boolean localizeFlowElements(Collection<FlowElement> flowElements, ObjectNode infoNode) {
    boolean localizationValuesChanged = false;
    
    if (flowElements == null) return localizationValuesChanged;
    
    CommandContext commandContext = Context.getCommandContext();
    DynamicBpmnService dynamicBpmnService = commandContext.getProcessEngineConfiguration().getDynamicBpmnService();
    
    for (FlowElement flowElement : flowElements) {
      if (flowElement instanceof UserTask || flowElement instanceof SubProcess) {
        List<ExtensionElement> localizationElements = flowElement.getExtensionElements().get("localization");
        if (localizationElements != null) {
          for (ExtensionElement localizationElement : localizationElements) {
            if (BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {
              String locale = localizationElement.getAttributeValue(null, "locale");
              String name = localizationElement.getAttributeValue(null, "name");
              String documentation = null;
              List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
              if (documentationElements != null) {
                for (ExtensionElement documentationElement : documentationElements) {
                  documentation = StringUtils.trimToNull(documentationElement.getElementText());
                  break;
                }
              }

              String flowElementId = flowElement.getId();
              if (isEqualToCurrentLocalizationValue(locale, flowElementId, "name", name, infoNode) == false) {
                dynamicBpmnService.changeLocalizationName(locale, flowElementId, name, infoNode);
                localizationValuesChanged = true;
              }
              
              if (documentation != null && isEqualToCurrentLocalizationValue(locale, flowElementId, "description", documentation, infoNode) == false) {
                dynamicBpmnService.changeLocalizationDescription(locale, flowElementId, documentation, infoNode);
                localizationValuesChanged = true;
              }
              
              break;
            }
          }
        }
        
        if (flowElement instanceof SubProcess) {
          SubProcess subprocess = (SubProcess) flowElement;
          boolean isFlowElementLocalizationChanged = localizeFlowElements(subprocess.getFlowElements(), infoNode);
          boolean isDataObjectLocalizationChanged = localizeDataObjectElements(subprocess.getDataObjects(), infoNode);
          if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
            localizationValuesChanged = true;
          }
        }
      }
    }
    
    return localizationValuesChanged;
  }
  
  protected boolean isEqualToCurrentLocalizationValue(String language, String id, String propertyName, String propertyValue, ObjectNode infoNode) {
    boolean isEqual = false;
    JsonNode localizationNode = infoNode.path("localization").path(language).path(id).path(propertyName);
    if (localizationNode.isMissingNode() == false && localizationNode.isNull() == false && localizationNode.asText().equals(propertyValue)) {
      isEqual = true;
    }
    return isEqual;
  }
  
  protected boolean localizeDataObjectElements(List<ValuedDataObject> dataObjects, ObjectNode infoNode) {
    boolean localizationValuesChanged = false;
    CommandContext commandContext = Context.getCommandContext();
    DynamicBpmnService dynamicBpmnService = commandContext.getProcessEngineConfiguration().getDynamicBpmnService();

    for(ValuedDataObject dataObject : dataObjects) {
      List<ExtensionElement> localizationElements = dataObject.getExtensionElements().get("localization");
      if (localizationElements != null) {
        for (ExtensionElement localizationElement : localizationElements) {
          if (BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {
            String locale = localizationElement.getAttributeValue(null, "locale");
            String name = localizationElement.getAttributeValue(null, "name");
            String documentation = null;

            List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
            if (documentationElements != null) {
              for (ExtensionElement documentationElement : documentationElements) {
                documentation = StringUtils.trimToNull(documentationElement.getElementText());
                break;
              }
            }
            
            if (name != null && isEqualToCurrentLocalizationValue(locale, dataObject.getName(), DynamicBpmnConstants.LOCALIZATION_NAME, name, infoNode) == false) {
              dynamicBpmnService.changeLocalizationName(locale, dataObject.getName(), name, infoNode);
              localizationValuesChanged = true;
            }
            
            if (documentation != null && isEqualToCurrentLocalizationValue(locale, dataObject.getName(), 
                DynamicBpmnConstants.LOCALIZATION_DESCRIPTION, documentation, infoNode) == false) {
              
              dynamicBpmnService.changeLocalizationDescription(locale, dataObject.getName(), documentation, infoNode);
              localizationValuesChanged = true;
            }
          }
        }
      }
    }
    
    return localizationValuesChanged;
  }
  
  enum ExprType {
	  USER, GROUP
  }
  /*

   */
  private void addAuthorizationsFromIterator(Set<Expression> exprSet, ProcessDefinitionEntity processDefinition, ExprType exprType) {
    if (exprSet != null) { //判null
      Iterator<Expression> iterator = exprSet.iterator();
      while (iterator.hasNext()) { //循环集合
        Expression expr = (Expression) iterator.next();
        IdentityLinkEntity identityLink = new IdentityLinkEntity();
        identityLink.setProcessDef(processDefinition);
        if (exprType.equals(ExprType.USER)) {
           identityLink.setUserId(expr.toString()); //设置用户
        } else if (exprType.equals(ExprType.GROUP)) {
          identityLink.setGroupId(expr.toString());//设置组
        }
        identityLink.setType(IdentityLinkType.CANDIDATE); //设置用户的类型
        identityLink.insert();//添加到会话缓存   操作的表 ACT_RU_IDENTITYLINK  表
      }
    }
  }
  /*
  设置流程启动人
  负责将流程文档 中 定义的流程启动人 添加到会话缓存中
   */
  protected void addAuthorizations(ProcessDefinitionEntity processDefinition) {
    //添加用户
    addAuthorizationsFromIterator(processDefinition.getCandidateStarterUserIdExpressions(), processDefinition, ExprType.USER);
    //添加组
    addAuthorizationsFromIterator(processDefinition.getCandidateStarterGroupIdExpressions(), processDefinition, ExprType.GROUP);
  }

  /**
   * Returns the default name of the image resource for a certain process.
   * 
   * It will first look for an image resource which matches the process
   * specifically, before resorting to an image resource which matches the BPMN
   * 2.0 xml file resource.
   * 
   * Example: if the deployment contains a BPMN 2.0 xml resource called
   * 'abc.bpmn20.xml' containing only one process with key 'myProcess', then
   * this method will look for an image resources called 'abc.myProcess.png'
   * (or .jpg, or .gif, etc.) or 'abc.png' if the previous one wasn't found.
   * 
   * Example 2: if the deployment contains a BPMN 2.0 xml resource called 
   * 'abc.bpmn20.xml' containing three processes (with keys a, b and c),
   * then this method will first look for an image resource called 'abc.a.png' 
   * before looking for 'abc.png' (likewise for b and c).
   * Note that if abc.a.png, abc.b.png and abc.c.png don't exist, all
   * processes will have the same image: abc.png.
   * 
   * @return null if no matching image resource is found.
   */
  protected String getDiagramResourceForProcess(String bpmnFileResource, String processKey, Map<String, ResourceEntity> resources) {
    for (String diagramSuffix: DIAGRAM_SUFFIXES) {
      String diagramForBpmnFileResource = getBpmnFileImageResourceName(bpmnFileResource, diagramSuffix);
      String processDiagramResource = getProcessImageResourceName(bpmnFileResource, processKey, diagramSuffix);
      if (resources.containsKey(processDiagramResource)) {
        return processDiagramResource;
      } else if (resources.containsKey(diagramForBpmnFileResource)) {
        return diagramForBpmnFileResource;
      }
    }
    return null;
  }
  
  protected String getBpmnFileImageResourceName(String bpmnFileResource, String diagramSuffix) {
    String bpmnFileResourceBase = stripBpmnFileSuffix(bpmnFileResource);
    return bpmnFileResourceBase + diagramSuffix;
  }

  protected String getProcessImageResourceName(String bpmnFileResource, String processKey, String diagramSuffix) {
    String bpmnFileResourceBase = stripBpmnFileSuffix(bpmnFileResource);
    return bpmnFileResourceBase + processKey + "." + diagramSuffix;
  }

  protected String stripBpmnFileSuffix(String bpmnFileResource) {
    for (String suffix : BPMN_RESOURCE_SUFFIXES) {
      if (bpmnFileResource.endsWith(suffix)) {
        return bpmnFileResource.substring(0, bpmnFileResource.length() - suffix.length());
      }
    }
    return bpmnFileResource;
  }

  protected void createResource(String name, byte[] bytes, DeploymentEntity deploymentEntity) {
    ResourceEntity resource = new ResourceEntity();
    resource.setName(name);
    resource.setBytes(bytes);
    resource.setDeploymentId(deploymentEntity.getId());
    
    // Mark the resource as 'generated'
    resource.setGenerated(true);
    
    Context
      .getCommandContext()
      .getDbSqlSession()
      .insert(resource);
  }
  /*
  判断 后缀  是否是 规范的BPMN 资源名称
  如果该参数值的后缀是 bpmn20.xml  或者bpmn中的任何一个  则返回true
  此() 很重要
  因为 该返回值 决定了
  流程定义数据是否可以添加到 ACT_GE_PROCDEF 表中
  如果  返回值为false 那么 部署表中 有数据  但是 定义表中没有数据
   */
  protected boolean isBpmnResource(String resourceName) {
    for (String suffix : BPMN_RESOURCE_SUFFIXES) {
      if (resourceName.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }
  
  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }
  
  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }
  
  public BpmnParser getBpmnParser() {
    return bpmnParser;
  }
  
  public void setBpmnParser(BpmnParser bpmnParser) {
    this.bpmnParser = bpmnParser;
  }
  
  public IdGenerator getIdGenerator() {
    return idGenerator;
  }
  
  public void setIdGenerator(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }
  
}
