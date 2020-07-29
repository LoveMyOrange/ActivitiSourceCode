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
package org.activiti.engine.impl.persistence.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.delegate.event.impl.ActivitiEventSupport;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.form.StartFormHandler;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.runtime.InterpretableExecution;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.IdentityLinkType;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 * 流程定义的映射实体, 该类很重要, 间接继承了ScopeImpl 类
 * 因为所有对象解析之后的ActivityIMpl 对象都存储在ScopeImpl 类中
 * 所以可以通过ProcessDefinitionEntity 对象 获取所有对象解析之后 的结果
 */
public class ProcessDefinitionEntity extends ProcessDefinitionImpl implements ProcessDefinition, PersistentObject, HasRevision {

  private static final long serialVersionUID = 1L;

  protected String key;
  protected int revision = 1;
  protected int version;
  protected String category;
  protected String deploymentId;
  protected String resourceName;
  protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
  protected Integer historyLevel;
  protected StartFormHandler startFormHandler;
  protected String diagramResourceName;
  protected boolean isGraphicalNotationDefined;
  protected Map<String, TaskDefinition> taskDefinitions;
  protected Map<String, Object> variables;
  protected boolean hasStartFormKey;
  protected int suspensionState = SuspensionState.ACTIVE.getStateCode();
  protected boolean isIdentityLinksInitialized = false;
  protected List<IdentityLinkEntity> definitionIdentityLinkEntities = new ArrayList<IdentityLinkEntity>();
  protected Set<Expression> candidateStarterUserIdExpressions = new HashSet<Expression>();
  protected Set<Expression> candidateStarterGroupIdExpressions = new HashSet<Expression>();
  protected transient ActivitiEventSupport eventSupport;
  
  public ProcessDefinitionEntity() {
    super(null);
    eventSupport = new ActivitiEventSupport();
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
	in.defaultReadObject();
    eventSupport = new ActivitiEventSupport();

  }
  /*
  * 调用此()创建 ExecutionEntity 对象
  * 那么giao对象 如何被创建呢??? 内部实现机制 是什么????
  *
  * 如果流程不存在分支 或者多实例节点 则ExecutionEntity对象 与流程实例对象时等价的
  *
  *
  * */
  public ExecutionEntity createProcessInstance(String businessKey, ActivityImpl initial) {
    ExecutionEntity processInstance = null;
    /*
    委托当前类的父类 ProcessDefinitionImpl 创建实例对象
    唯一的区别在于 俩调用的() 不一样
    如果 initial 不为空   super.createProcessInstanceForInitial(initial);
    如果为空 super.createProcessInstance();

    * */
    if(initial == null) {
      processInstance = (ExecutionEntity) super.createProcessInstance();
    }else {
      processInstance = (ExecutionEntity) super.createProcessInstanceForInitial(initial);
    }
    /*
    * 设置ExecutionEntity对象属性
    * EntityEntity 为 ACT_RU_EXECUTION 表的映射实体类
    * 所以 EntityEntity 类中 定义的属性 与 ACT_RU_EXECUTION 表中的字段一一对应
    * */
    processInstance.setExecutions(new ArrayList<ExecutionEntity>());
    processInstance.setProcessDefinition(processDefinition);
    // Do not initialize variable map (let it happen lazily)

    // Set business key (if any)
    if (businessKey != null) {
    	processInstance.setBusinessKey(businessKey);
    }
    
    // Inherit tenant id (if any)
    if (getTenantId() != null) {
    	processInstance.setTenantId(getTenantId());
    }
    
    // Reset the process instance in order to have the db-generated process instance id available
    processInstance.setProcessInstance(processInstance);
    
    // initialize the template-defined data objects as variables first

    //获取dataObject元素信息,   dataObject是BPMN20规范, Activiti 对其进行实现
    Map<String, Object> dataObjectVars = getVariables();
    if (dataObjectVars != null) {
      //dataObject 元素信息作为流程实例的变量进行处理
      processInstance.setVariables(dataObjectVars);
    }
    /*
    获取流程实例的启动人
    内部使用 ThreadLocal
    流程实例启动人的设置操作 必须在 流程实例启动之前进行

     */

    String authenticatedUserId = Authentication.getAuthenticatedUserId();
    /*
    添加  initiatorVariableName 变量
    该变量 是 <startEvent id="startEvent1"  activiti:initator="张胜男">
    关于此属性的解析 可以参考 StartEventXMLConverter 和 StartEventParseHandler

    将 属性值作为流程实例中变量的名称  变量的值则对应   authenticatedUserId
     */
    String initiatorVariableName = (String) getProperty(BpmnParse.PROPERTYNAME_INITIATOR_VARIABLE_NAME);
    if (initiatorVariableName!=null) {
      processInstance.setVariable(initiatorVariableName, authenticatedUserId);
    }
    //将流程启动人 添加到 ACT_RU_IDENTITYLINK 表     只支持具体的人, 不支持组
    if (authenticatedUserId != null) {
      processInstance.addIdentityLink(authenticatedUserId, null, IdentityLinkType.STARTER);
    }
    //将已知属性值添加到会话缓存中 ,引擎最终会将 会话缓存中的数据刷新到 ACT_HI_PROCINST 表中
    Context.getCommandContext().getHistoryManager()
      .recordProcessInstanceStart(processInstance);
    //转发事件
    if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED, processInstance));
    }
    
    return processInstance;
  }
  public ExecutionEntity createProcessInstance(String businessKey) {
    return createProcessInstance(businessKey, null);
  }

  public ExecutionEntity createProcessInstance() {
    return createProcessInstance(null);
  }
  
  /*
  * 疑问???
  *   实例化ExecutionEntity类时 并没有进行属性的填充操作, 怎么能首先insert()
  *
  * 此处的insert()只是通知引擎 ExecutionEntity的实例化操作已经执行完毕
  * 并且已经做好了 插入会话缓存的准备
  * */
  @Override
  protected InterpretableExecution newProcessInstance(ActivityImpl activityImpl) {
    ExecutionEntity processInstance = new ExecutionEntity(activityImpl);
    processInstance.insert();//为插入DB做准备
    return processInstance;
  }
  
  public IdentityLinkEntity addIdentityLink(String userId, String groupId) {
    IdentityLinkEntity identityLinkEntity = new IdentityLinkEntity();
    getIdentityLinks().add(identityLinkEntity);
    identityLinkEntity.setProcessDef(this);
    identityLinkEntity.setUserId(userId);
    identityLinkEntity.setGroupId(groupId);
    identityLinkEntity.setType(IdentityLinkType.CANDIDATE);
    identityLinkEntity.insert();
    return identityLinkEntity;
  }
  
  public void deleteIdentityLink(String userId, String groupId) {
    List<IdentityLinkEntity> identityLinks = Context
      .getCommandContext()
      .getIdentityLinkEntityManager()
      .findIdentityLinkByProcessDefinitionUserAndGroup(id, userId, groupId);
    
    for (IdentityLinkEntity identityLink: identityLinks) {
      Context
        .getCommandContext()
        .getIdentityLinkEntityManager()
        .deleteIdentityLink(identityLink, false);
    }
  }
  
  public List<IdentityLinkEntity> getIdentityLinks() {
    if (!isIdentityLinksInitialized) {
      definitionIdentityLinkEntities = Context
        .getCommandContext()
        .getIdentityLinkEntityManager()
        .findIdentityLinksByProcessDefinitionId(id);
      isIdentityLinksInitialized = true;
    }
    
    return definitionIdentityLinkEntities;
  }

  public String toString() {
    return "ProcessDefinitionEntity["+id+"]";
  }


  // getters and setters //////////////////////////////////////////////////////
  
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();  
    persistentState.put("suspensionState", this.suspensionState);
    persistentState.put("category", this.category);
    return persistentState;
  }
  
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }
  
  public int getVersion() {
    return version;
  }
  
  public void setVersion(int version) {
    this.version = version;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }
  
  public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getHistoryLevel() {
    return historyLevel;
  }

  public void setHistoryLevel(Integer historyLevel) {
    this.historyLevel = historyLevel;
  }

  public StartFormHandler getStartFormHandler() {
    return startFormHandler;
  }

  public void setStartFormHandler(StartFormHandler startFormHandler) {
    this.startFormHandler = startFormHandler;
  }

  public Map<String, TaskDefinition> getTaskDefinitions() {
    return taskDefinitions;
  }

  public void setTaskDefinitions(Map<String, TaskDefinition> taskDefinitions) {
    this.taskDefinitions = taskDefinitions;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
  
  public String getDiagramResourceName() {
    return diagramResourceName;
  }

  public void setDiagramResourceName(String diagramResourceName) {
    this.diagramResourceName = diagramResourceName;
  }

  public boolean hasStartFormKey() {
    return hasStartFormKey;
  }
  
  public boolean getHasStartFormKey() {
    return hasStartFormKey;
  }
  
  public void setStartFormKey(boolean hasStartFormKey) {
    this.hasStartFormKey = hasStartFormKey;
  }

  public void setHasStartFormKey(boolean hasStartFormKey) {
    this.hasStartFormKey = hasStartFormKey;
  }
  
  public boolean isGraphicalNotationDefined() {
    return isGraphicalNotationDefined;
  }
  
  public boolean hasGraphicalNotation() {
  	return isGraphicalNotationDefined;
  }
  
  public void setGraphicalNotationDefined(boolean isGraphicalNotationDefined) {
    this.isGraphicalNotationDefined = isGraphicalNotationDefined;
  }
  
  public int getRevision() {
    return revision;
  }
  public void setRevision(int revision) {
    this.revision = revision;
  }
  
  public int getRevisionNext() {
    return revision+1;
  }
  
  public int getSuspensionState() {
    return suspensionState;
  }
  
  public void setSuspensionState(int suspensionState) {
    this.suspensionState = suspensionState;
  }

  public boolean isSuspended() {
    return suspensionState == SuspensionState.SUSPENDED.getStateCode();
  }
  
  public Set<Expression> getCandidateStarterUserIdExpressions() {
    return candidateStarterUserIdExpressions;
  }

  public void addCandidateStarterUserIdExpression(Expression userId) {
    candidateStarterUserIdExpressions.add(userId);
  }

  public Set<Expression> getCandidateStarterGroupIdExpressions() {
    return candidateStarterGroupIdExpressions;
  }

  public void addCandidateStarterGroupIdExpression(Expression groupId) {
    candidateStarterGroupIdExpressions.add(groupId);
  }
  
  public ActivitiEventSupport getEventSupport() {
	  return eventSupport;
  }
}
