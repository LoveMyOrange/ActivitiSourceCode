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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.ResourceEntity;
import org.activiti.engine.impl.repository.DeploymentBuilderImpl;
import org.activiti.engine.repository.Deployment;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * 部署的命令类
 */
public class DeployCmd<T> implements Command<Deployment>, Serializable {

  private static final long serialVersionUID = 1L;
  protected DeploymentBuilderImpl deploymentBuilder;

  public DeployCmd(DeploymentBuilderImpl deploymentBuilder) {
    this.deploymentBuilder = deploymentBuilder;
  }
  /*

   */
  public Deployment execute(CommandContext commandContext) {
      //获取部署实体对象
    DeploymentEntity deployment = deploymentBuilder.getDeployment();
    //设置部署时间为当前系统时间
    deployment.setDeploymentTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
    /*
    判断是否开启了过滤重复文档功能
     如果DB中已经存在 ,则不会重复部署
    repositoryService.createDeployment().enableDuplicateFiltering();
        具体如何过滤重复文档的???
     */
    if ( deploymentBuilder.isDuplicateFilterEnabled() ) {
    	
    	List<Deployment> existingDeployments = new ArrayList<Deployment>();
    	// 如果deployment对象的 tenantId 不存在
      if (deployment.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
          //根据deployment对象 的name值 查找 act_re_deployment表中是否已经存在记录
      	DeploymentEntity existingDeployment = commandContext
     			 .getDeploymentEntityManager()
     			 .findLatestDeploymentByName(deployment.getName());

      	if (existingDeployment != null) {
            /*
            如果查找到了 直接添加到  existingDeployments 集合中
            这个集合中只有1 条数据  ,因为 上面的SQL 如果查找到了 不管多少条 只会 返回一条
             */
      		existingDeployments.add(existingDeployment);
      	}
      }
      //
      else {
          //根据租户的tenantId 和name值 从 act_re_deployment中查询数据 如果存在则 添加到集合
      	 List<Deployment> deploymentList = commandContext
             .getProcessEngineConfiguration().getRepositoryService()
             .createDeploymentQuery()
             .deploymentName(deployment.getName())
             .deploymentTenantId(deployment.getTenantId())
             .orderByDeploymentId().desc().list();
      	 //
      	 if (!deploymentList.isEmpty()) {
      		 existingDeployments.addAll(deploymentList);
      	 }
      }

      DeploymentEntity existingDeployment = null; //确保只有一个最新的实体对象
      if(!existingDeployments.isEmpty()) {
        existingDeployment = (DeploymentEntity) existingDeployments.get(0);
      }
      // 比较deployment 对象 已经存在的部署实体对象是否完全一样 ,如果 两个对象属性值完全一致 ,则不需要重新部署流程
      if ( (existingDeployment!=null)
              //deploymentsDiffer用于对比 deployment对象 与 existingDeployment 对象是否相等,
           && !deploymentsDiffer(deployment, existingDeployment)) {
        return existingDeployment;
      }
    }
    /*
    如果没有开启重复文档过滤功能, 则重新部署流程 设置isNew 为true 即可   true 表示文档是否可以进行部署操作
         该值决定了  流程文档是否可以进行部署操作
         既然 此值 可以设置为 true  ,那么什么情况下 设置为false 呢??

         举个栗子:
         例如 现在 开发人员要完成一个指定的任务 , 那么 当开发人员调用Activiti 提供的API 完成任务时
         引擎内部需要根据任务ID 值 查找当前任务节点的信息 ,  以及 该任务节点的活动行为类 ,目标节点等信息
         而以上 所说的信息值 都需要存储在 流程定义缓存中 ,
         如果流程定义缓存 数据丢失 ,则需要 根据 任务 节点 id 值
         从DB 中查询该 任务节点 所归属的流程文档 (存储在ACT_GE_BYTEARRAY )
         以及流程定义信息(ACT_RE_PROCDEF)
         如果查询到了流程文档信息,则会再次调用 BpmnDeployer类中的deploy() 操作 流程文档
         并且设置isNew 为false,,该目的是为了重新生成流程定义缓存数据
         既然部署流程文档与 重新生成流程定义缓存数据 都需要调用 BpmnDeployer类中的deploy()
         Activiti 就需要对 这两个操作加以区分, 这时isNew 属性为false, 值 就派上用场了

         明确告诉BpmnDeployer 现在只进行重新生成流程定义缓存数据的操作 ,不需要 执行流程文档的入库操作
     */
    deployment.setNew(true);
    /*
    将部署实体对象添加到会话缓存中 后续插入DB
    思考??
            为何不直接将数据直接插入到DB中呢???
            因为部署器分为3种 ,
            前置部署器, 内置部署器  后置部署器,

            如果直接将数据 添加到DB中 , 就会出现一个问题

            既然数据都已经插入到DB了, 那么后置部署器就没有存在的价值了

            如果使用了会话缓存技术, 则会话缓存技术,则会话缓存中的数据在彻底刷新到DB之前
            前置部署器可以对deployment 对象的 属性值 进行操作
            后置部署器可以对 会话缓存中的数据进行任意修改

            例如 开发人员可以通过后置部署器 获取会话缓存中的deployment 对象并对其 进行修改
            也可以通过后置部署器, 将扩展的元素信息值 保存到指定的介质中 例如DB 或者内存
     */
    commandContext
      .getDeploymentEntityManager()
      .insertDeployment(deployment);
    //如果开启事件转发机制,转发  ENTITY_CREATED 事件
    if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
	    commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
	    		ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED, deployment));
    }
    /*
    设置是否开启BPMN2.0 XSD 文件验证和流程文档 格式验证  ,默认开启
    禁用功能
        repositoryService.createDeployment().disableBpmnValidation().disableSchemaValidation()
     */
    Map<String, Object> deploymentSettings = new HashMap<String, Object>();
    deploymentSettings.put(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED, deploymentBuilder.isBpmn20XsdValidationEnabled());
    deploymentSettings.put(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED, deploymentBuilder.isProcessValidationEnabled());
    /*
    真正开始部署流程文档
    此() 处理逻辑可以分为两个 步骤

    1) 获取部署器管理器 DeploymentManager
    2) 调用 DeploymentManager类的deploy() 部署流程文档
        */
    commandContext
      .getProcessEngineConfiguration()
      .getDeploymentManager()
      .deploy(deployment, deploymentSettings);
    /*
    如果设置了 流程开启 时间  ,则进行相应的处理 repositoryService.createDeployment().activateProcessDefinitionsOn(new Date());
    该操作 主要 针对 挂起  激活流程
     */
    if (deploymentBuilder.getProcessDefinitionsActivationDate() != null) {
      scheduleProcessDefinitionActivation(commandContext, deployment);
    }
    //省略 如果开启事件转发机制,则转发  ENTITY_INITIALIZED 事件
    if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
	    commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
	    		ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_INITIALIZED, deployment));
    }
    
    return deployment;
  }
    /*
  比较deployment 对象 已经存在的部署实体对象是否完全一样
     */
  protected boolean deploymentsDiffer(DeploymentEntity deployment, DeploymentEntity saved) {
    
    if(deployment.getResources() == null || saved.getResources() == null) {
      return true;
    }
    
    Map<String, ResourceEntity> resources = deployment.getResources();
    Map<String, ResourceEntity> savedResources = saved.getResources();
    
    for (String resourceName: resources.keySet()) {
      ResourceEntity savedResource = savedResources.get(resourceName);
      
      if(savedResource == null) return true;
      
      if(!savedResource.isGenerated()) {
        ResourceEntity resource = resources.get(resourceName);
        
        byte[] bytes = resource.getBytes();
        byte[] savedBytes = savedResource.getBytes();
        if (!Arrays.equals(bytes, savedBytes)) {
          return true;
        }
      }
    }
    return false;
  }
  
  protected void scheduleProcessDefinitionActivation(CommandContext commandContext, DeploymentEntity deployment) {
    for (ProcessDefinitionEntity processDefinitionEntity : deployment.getDeployedArtifacts(ProcessDefinitionEntity.class)) {
      
      // If activation date is set, we first suspend all the process definition
      SuspendProcessDefinitionCmd suspendProcessDefinitionCmd = 
              new SuspendProcessDefinitionCmd(processDefinitionEntity, false, null, deployment.getTenantId());
      suspendProcessDefinitionCmd.execute(commandContext);
      
      // And we schedule an activation at the provided date
      ActivateProcessDefinitionCmd activateProcessDefinitionCmd =new ActivateProcessDefinitionCmd(
      		processDefinitionEntity, false, deploymentBuilder.getProcessDefinitionsActivationDate(), deployment.getTenantId());
      activateProcessDefinitionCmd.execute(commandContext);
    }
  }

//  private boolean resourcesDiffer(ByteArrayEntity value, ByteArrayEntity other) {
//    if (value == null && other == null) {
//      return false;
//    }
//    String bytes = createKey(value.getBytes());
//    String savedBytes = other == null ? null : createKey(other.getBytes());
//    return !bytes.equals(savedBytes);
//  }
//
//  private String createKey(byte[] bytes) {
//    if (bytes == null) {
//      return "";
//    }
//    MessageDigest digest;
//    try {
//      digest = MessageDigest.getInstance("MD5");
//    } catch (NoSuchAlgorithmException e) {
//      throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
//    }
//    bytes = digest.digest(bytes);
//    return String.format("%032x", new BigInteger(1, bytes));
//  }
}
