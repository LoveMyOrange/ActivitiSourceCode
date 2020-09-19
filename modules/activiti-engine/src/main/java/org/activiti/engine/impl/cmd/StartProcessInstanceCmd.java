/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.engine.impl.cmd;

import java.io.Serializable;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * 启动流程实例的命令类    PVM的运转由此开始
 */
public class StartProcessInstanceCmd<T> implements Command<ProcessInstance>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String processDefinitionKey;
  protected String processDefinitionId;
  protected Map<String, Object> variables;
  protected String businessKey;
  protected String tenantId;
  protected String processInstanceName;
  
  public StartProcessInstanceCmd(String processDefinitionKey, String processDefinitionId, String businessKey, Map<String, Object> variables) {
    this.processDefinitionKey = processDefinitionKey;//流程定义key
    this.processDefinitionId = processDefinitionId;//流程定义id
    this.businessKey = businessKey;//业务key
    this.variables = variables;//变量
  }
  
  public StartProcessInstanceCmd(String processDefinitionKey, String processDefinitionId, 
  		String businessKey, Map<String, Object> variables, String tenantId) {
  	this(processDefinitionKey, processDefinitionId, businessKey, variables);
  	this.tenantId = tenantId;
  }
  
  public StartProcessInstanceCmd(ProcessInstanceBuilderImpl processInstanceBuilder) {
    this(processInstanceBuilder.getProcessDefinitionKey(), processInstanceBuilder.getProcessDefinitionId(),
        processInstanceBuilder.getBusinessKey(), processInstanceBuilder.getVariables(), processInstanceBuilder.getTenantId());
    this.processInstanceName = processInstanceBuilder.getProcessInstanceName();
  }
  /*
  *
  * */
  public ProcessInstance execute(CommandContext commandContext) {
    //获取 DeploymentManager 负责管理所有的缓存处理类
    DeploymentManager deploymentManager = commandContext
      .getProcessEngineConfiguration()
      .getDeploymentManager();
    
    // Find the process definition
    /*
    获取 ProcessDefinitionEntity 对象
    意义何在???
        因为 所有的节点和连线的定义信息分别对应PVM 的 ActivityImpl对象 和 TransitionImpl对象 并且最终存储在ProcessDefinitionEntity对象中
        因为后续PVM 运转时需要获取   ActivityImpl对象 和 TransitionImpl对象 的信息
        所以 获取 ProcessDefinitionEntity 很有必要
        该环节尝试 首先从 缓存中获取  ,如果缓存中不存在,再次执行流程文档的部署工作
     */
    ProcessDefinitionEntity processDefinition = null;
    /*
    * 获取ProcessDefinitionEntity对象 有很多 种方式 以  findDeployedProcessDefinitionById 详细 研究一下
    * */
    if (processDefinitionId != null) {
      processDefinition = deploymentManager.findDeployedProcessDefinitionById(processDefinitionId);
      if (processDefinition == null) {
        throw new ActivitiObjectNotFoundException("No process definition found for id = '" + processDefinitionId + "'", ProcessDefinition.class);
      }
    } else if (processDefinitionKey != null && (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId))){
      processDefinition = deploymentManager.findDeployedLatestProcessDefinitionByKey(processDefinitionKey);
      if (processDefinition == null) {
        throw new ActivitiObjectNotFoundException("No process definition found for key '" + processDefinitionKey +"'", ProcessDefinition.class);
      }
    } else if (processDefinitionKey != null && tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
    	 processDefinition = deploymentManager.findDeployedLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
       if (processDefinition == null) {
         throw new ActivitiObjectNotFoundException("No process definition found for key '" + processDefinitionKey +"' for tenant identifier " + tenantId, ProcessDefinition.class);
       }
    } else {
      throw new ActivitiIllegalArgumentException("processDefinitionKey and processDefinitionId are null");
    }
    
    // Do not start process a process instance if the process definition is suspended
    //挂起流程验证, 已经挂起的流程是不能执行启动的 , 如果强行启动 已经挂起的流程  报错
    if (deploymentManager.isProcessDefinitionSuspended(processDefinition.getId())) {
      throw new ActivitiException("Cannot start process instance. Process definition " 
              + processDefinition.getName() + " (id = " + processDefinition.getId() + ") is suspended");
    }

    // Start the process instance
    /*
    创建 ExecutionEntity 对象 ,该对象时 流程实例运转的核心对象 ,负责流程实例的创建,启动结束等操作
    并持有 ProcessDefinitionEntity 对象 ,
    所以  程序可以很方便的通过 E ExecutionEntity 对象获取ProcessDefinitionEntity对象
    然后通过 ProcessDefinitionEntity 对象 获取PVM中的
    ActivityImpl对象和 TransitionImpl对象


    创建流程实例
     */
    ExecutionEntity processInstance = processDefinition.createProcessInstance(businessKey);

    // now set the variables passed into the start command
    /*
    * 设置流程变量
    * */
    initializeVariables(processInstance);

    // now set processInstance name
    /*
    * 设置 流程实例name ,
    * */
    if (processInstanceName != null) {
      processInstance.setName(processInstanceName);
      commandContext.getHistoryManager().recordProcessInstanceNameChange(processInstance.getId(), processInstanceName);
    }
    //启动
    processInstance.start();
    
    return processInstance;
  }

  protected void initializeVariables(ExecutionEntity processInstance) {
    if (variables != null) {
      processInstance.setVariables(variables);
    }
  }
}
