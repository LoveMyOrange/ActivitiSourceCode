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

package org.activiti.engine.impl.persistence.deploy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.ProcessDefinitionQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.*;
import org.activiti.engine.impl.util.io.BytesStreamSource;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;


/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Joram Barrez
 * 部署管理器类
 */
public class DeploymentManager {

  protected DeploymentCache<ProcessDefinitionEntity> processDefinitionCache;
  protected DeploymentCache<BpmnModel> bpmnModelCache;
  protected ProcessDefinitionInfoCache processDefinitionInfoCache;
  protected DeploymentCache<Object> knowledgeBaseCache; // Needs to be object to avoid an import to Drools in this core class
  protected List<Deployer> deployers;
  
  public void deploy(DeploymentEntity deployment) {
    deploy(deployment, null);
  }
  /*
  循环 所有的部署器
  因为部署管理中 持有部署器 集合 ,
  deploy() 首先循环 遍历 deployers 集合
  然后调用 deploy() 部署流程文档

  应该可以明白 为何 部署器 集合使用List 存储

  因为这里是按照 部署器集合的 先后顺序进行遍历 调用
  并且 deploy() 两个输入参数 都是 引用类型
  这样 开发人员就可以通过 前置部署器 或者 后置部署器 获取以上 所说的两个输入参数
  然后进行相应的 操作
   */
  public void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings) {
    for (Deployer deployer: deployers) {
      deployer.deploy(deployment, deploymentSettings);
    }
  }
  /*
  *
  * */
  public ProcessDefinitionEntity findDeployedProcessDefinitionById(String processDefinitionId) {
    if (processDefinitionId == null) {
      throw new ActivitiIllegalArgumentException("Invalid process definition id : null");
    }
    
    // first try the cache
    //尝试从缓存中获取
    ProcessDefinitionEntity processDefinition = processDefinitionCache.get(processDefinitionId);
    
    if (processDefinition == null) {
      /*
      如果缓存不存在的话,  委托 ProcessDefinitionEntityManager 获取 ProcessDefinitionEntity 对象
      底层通过MyBatis 根据 processDefinitionId 作为查询条件 从DB   ACT_re_procdef 表中查询数据
       */
      processDefinition = Context.getCommandContext()
        .getProcessDefinitionEntityManager()
        .findProcessDefinitionById(processDefinitionId);
      if (processDefinition == null) {
        throw new ActivitiObjectNotFoundException("no deployed process definition found with id '" + processDefinitionId + "'", ProcessDefinition.class);
      }
      //如果从DB表中 获取到数据, 则 委托 此() 进行流程文档的部署操作
      processDefinition = resolveProcessDefinition(processDefinition);
    }
    return processDefinition;
  }
  
  public ProcessDefinitionEntity findProcessDefinitionByIdFromDatabase(String processDefinitionId) {
    if (processDefinitionId == null) {
      throw new ActivitiIllegalArgumentException("Invalid process definition id : null");
    }
    
    ProcessDefinitionEntity processDefinition = Context.getCommandContext()
        .getProcessDefinitionEntityManager()
        .findProcessDefinitionById(processDefinitionId);
    
    if (processDefinition == null) {
      throw new ActivitiObjectNotFoundException("no deployed process definition found with id '" + processDefinitionId + "'", ProcessDefinition.class);
    }
    
    return processDefinition;
  }
  
  public boolean isProcessDefinitionSuspended(String processDefinitionId) {
    return findProcessDefinitionByIdFromDatabase(processDefinitionId).isSuspended();
  }
  
  public BpmnModel getBpmnModelById(String processDefinitionId) {
    if (processDefinitionId == null) {
      throw new ActivitiIllegalArgumentException("Invalid process definition id : null");
    }
    
    // first try the cache
    BpmnModel bpmnModel = bpmnModelCache.get(processDefinitionId);
    
    if (bpmnModel == null) {
      ProcessDefinitionEntity processDefinition = findDeployedProcessDefinitionById(processDefinitionId);
      if (processDefinition == null) {
        throw new ActivitiObjectNotFoundException("no deployed process definition found with id '" + processDefinitionId + "'", ProcessDefinition.class);
      }
      
      // Fetch the resource
      String resourceName = processDefinition.getResourceName();
      ResourceEntity resource = Context.getCommandContext().getResourceEntityManager()
              .findResourceByDeploymentIdAndResourceName(processDefinition.getDeploymentId(), resourceName);
      if (resource == null) {
        if (Context.getCommandContext().getDeploymentEntityManager().findDeploymentById(processDefinition.getDeploymentId()) == null) {
          throw new ActivitiObjectNotFoundException("deployment for process definition does not exist: " 
              + processDefinition.getDeploymentId(), Deployment.class);
        } else {
          throw new ActivitiObjectNotFoundException("no resource found with name '" + resourceName 
                  + "' in deployment '" + processDefinition.getDeploymentId() + "'", InputStream.class);
        }
      }
      
      // Convert the bpmn 2.0 xml to a bpmn model
      BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
      bpmnModel = bpmnXMLConverter.convertToBpmnModel(new BytesStreamSource(resource.getBytes()), false, false);
      bpmnModelCache.add(processDefinition.getId(), bpmnModel);
    }
    return bpmnModel;
  }
  
  public ProcessDefinitionEntity findDeployedLatestProcessDefinitionByKey(String processDefinitionKey) {
    ProcessDefinitionEntity processDefinition = Context
      .getCommandContext()
      .getProcessDefinitionEntityManager()
      .findLatestProcessDefinitionByKey(processDefinitionKey);
    
    if (processDefinition==null) {
      throw new ActivitiObjectNotFoundException("no processes deployed with key '"+processDefinitionKey+"'", ProcessDefinition.class);
    }
    processDefinition = resolveProcessDefinition(processDefinition);
    return processDefinition;
  }

  public ProcessDefinitionEntity findDeployedLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
    ProcessDefinitionEntity processDefinition = Context
      .getCommandContext()
      .getProcessDefinitionEntityManager()
      .findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
    if (processDefinition==null) {
      throw new ActivitiObjectNotFoundException("no processes deployed with key '"+processDefinitionKey+"' for tenant identifier '" + tenantId + "'", ProcessDefinition.class);
    }
    processDefinition = resolveProcessDefinition(processDefinition);
    return processDefinition;
  }

  public ProcessDefinitionEntity findDeployedProcessDefinitionByKeyAndVersion(String processDefinitionKey, Integer processDefinitionVersion) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) Context
      .getCommandContext()
      .getProcessDefinitionEntityManager()
      .findProcessDefinitionByKeyAndVersion(processDefinitionKey, processDefinitionVersion);
    if (processDefinition==null) {
      throw new ActivitiObjectNotFoundException("no processes deployed with key = '" + processDefinitionKey + "' and version = '" + processDefinitionVersion + "'", ProcessDefinition.class);
    }
    processDefinition = resolveProcessDefinition(processDefinition);
    return processDefinition;
  }
  /*
  * 主要用于部署流程文档,
  * 为何委托此() 进行文档的部署操作
  *
  * 试想一下 通过 ProcessDefinitionEntityManager 对象 从 DB 中直接获取的 ProcessDefinitionEntity对象
  * 属性值  仅仅是 与ACT_GE_PROCDEF 表中的字段值 一一对应
  *
  * 而PVM 最终需要通过 ProcessDefinitionEntity 实例对象  获取
  * ActivityImpl对象 以及 TransitionImpl 对象
  * 如果文档没有经过部署环节
  * 则ActvityImpl  和 transitionImpl 对象
  * 是没有 获取 和赋值的
  * 进而导致ProcessDefinitionEntity 对象 中 部分属性值 缺失
  *
  * 不完整的 对象 是无法 供 PVM 使用的
  *
  * 该操作用来保证 PVM 中 的 各种实例对象可以正常的初始化
  * */
  public ProcessDefinitionEntity resolveProcessDefinition(ProcessDefinitionEntity processDefinition) {
    //获取id 值
    String processDefinitionId = processDefinition.getId();
    String deploymentId = processDefinition.getDeploymentId();
    //根据id 从 缓存中获取值
    processDefinition = processDefinitionCache.get(processDefinitionId);
    if (processDefinition==null) {
      //首先获取 CommandContext对象 然后通过该对象获取 DeploymentEntityManager对象
//      最终通过DeploymentEntityManager 对象 获取 DeploymentEntity对象
//
      DeploymentEntity deployment = Context
        .getCommandContext()
        .getDeploymentEntityManager()
        .findDeploymentById(deploymentId);
      //设置 isNew 为fasle
      deployment.setNew(false);
      //部署流程文档
      deploy(deployment, null);
      /*
      为了确保 ProcessDefinitionEntity 对象已经被成功添加到缓存中
      因为流程文档部署涉及了 元素解析 以及对象解析
      该过程 非常消耗性能  所以该操作非常有必要
       */
      processDefinition = processDefinitionCache.get(processDefinitionId);
      
      if (processDefinition==null) {
        throw new ActivitiException("deployment '"+deploymentId+"' didn't put process definition '"+processDefinitionId+"' in the cache");
      }
    }
    return processDefinition;
  }
  
  public void removeDeployment(String deploymentId, boolean cascade) {
	  DeploymentEntityManager deploymentEntityManager = Context
			  .getCommandContext()
			  .getDeploymentEntityManager();
	  
	  DeploymentEntity deployment = deploymentEntityManager.findDeploymentById(deploymentId); 
	  if(deployment == null)
		  throw new ActivitiObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", DeploymentEntity.class);

    // Remove any process definition from the cache
    List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl(Context.getCommandContext())
            .deploymentId(deploymentId)
            .list();
    ActivitiEventDispatcher eventDispatcher = Context.getProcessEngineConfiguration().getEventDispatcher();
    
    for (ProcessDefinition processDefinition : processDefinitions) {
      
      // Since all process definitions are deleted by a single query, we should dispatch the events in this loop
      if (eventDispatcher.isEnabled()) {
      	eventDispatcher.dispatchEvent(ActivitiEventBuilder.createEntityEvent(
      			ActivitiEventType.ENTITY_DELETED, processDefinition));
      }
    }
    
    // Delete data
    deploymentEntityManager.deleteDeployment(deploymentId, cascade);
    
    // Since we use a delete by query, delete-events are not automatically dispatched
    if(eventDispatcher.isEnabled()) {
    	eventDispatcher.dispatchEvent(
    			ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_DELETED, deployment));
    }
    
    for (ProcessDefinition processDefinition : processDefinitions) {
      processDefinitionCache.remove(processDefinition.getId());
    }
  }
  
  // getters and setters //////////////////////////////////////////////////////
  
  public List<Deployer> getDeployers() {
    return deployers;
  }
  
  public void setDeployers(List<Deployer> deployers) {
    this.deployers = deployers;
  }

  public DeploymentCache<ProcessDefinitionEntity> getProcessDefinitionCache() {
    return processDefinitionCache;
  }
  
  public void setProcessDefinitionCache(DeploymentCache<ProcessDefinitionEntity> processDefinitionCache) {
    this.processDefinitionCache = processDefinitionCache;
  }
  
  public DeploymentCache<BpmnModel> getBpmnModelCache() {
    return bpmnModelCache;
  }

  public void setBpmnModelCache(DeploymentCache<BpmnModel> bpmnModelCache) {
    this.bpmnModelCache = bpmnModelCache;
  }

  public ProcessDefinitionInfoCache getProcessDefinitionInfoCache() {
    return processDefinitionInfoCache;
  }

  public void setProcessDefinitionInfoCache(ProcessDefinitionInfoCache processDefinitionInfoCache) {
    this.processDefinitionInfoCache = processDefinitionInfoCache;
  }

  public DeploymentCache<Object> getKnowledgeBaseCache() {
    return knowledgeBaseCache;
  }

  public void setKnowledgeBaseCache(DeploymentCache<Object> knowledgeBaseCache) {
    this.knowledgeBaseCache = knowledgeBaseCache;
  }
  
}
