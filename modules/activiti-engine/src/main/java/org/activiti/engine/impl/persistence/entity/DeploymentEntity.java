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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.repository.Deployment;


/**
 * @author Tom Baeyens
 */
public class DeploymentEntity implements Serializable, Deployment, PersistentObject {

  private static final long serialVersionUID = 1L;
  
  protected String id;
  protected String name;
  protected String category;
  protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
  protected Map<String, ResourceEntity> resources; //resources是  是Map结构 key : 资源名称 value 是ResourceEntity 对象
  protected Date deploymentTime;
  protected boolean isNew;
  
  /**
   * Will only be used during actual deployment to pass deployed artifacts (eg process definitions).
   * Will be null otherwise.
   */
  protected Map<Class<?>, List<Object>> deployedArtifacts;
  
  public ResourceEntity getResource(String resourceName) {
    return getResources().get(resourceName);
  }
  /*
  *
  * */
  public void addResource(ResourceEntity resource) {
    //判断集合是否为空 如果为空将reources 对象添加到resources集合中
    if (resources==null) {
      //此为MAP 结构 key是资源名称 value 是  ResourceEntity 对象
      resources = new HashMap<String, ResourceEntity>();
    }
    resources.put(resource.getName(), resource);
  }

  // lazy loading /////////////////////////////////////////////////////////////
  /*
  主要用于获取 resources集合
   */
  public Map<String, ResourceEntity> getResources() {
    //判null
    if (resources==null && id!=null) {
      /*
      从DB中获取
      以id 值 (流程部署ID ) 为查询条件 从DB(ACT_GE_bytearray) 表中查找数据,

      客户端部署流程文档的时候可以指定 resourceNmae值
      形如 repositoryService.createDeployment().addInputStream(resourceName,inputStream);
       */
      List<ResourceEntity> resourcesList = Context
        .getCommandContext()
        .getResourceEntityManager()
        .findResourcesByDeploymentId(id);
      //初始化resources集合  resources集合中的key的值 对应ACT_GE_BYTEARRAY 中的NAME_字段值
      resources = new HashMap<String, ResourceEntity>();
      //循环遍历 list 并将 该集合的值 添加到resources集合中
      for (ResourceEntity resource: resourcesList) {
        //客户端 部署流程文档的时候 可以指定 resourceName ,repositoryService.createDeployment().addInputStream("你好.bpmn",null);
        resources.put(resource.getName(), resource);
      }
    }
    return resources;
  }

  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("category", this.category);
    persistentState.put("tenantId", tenantId);
    return persistentState;
  }
  
  // Deployed artifacts manipulation //////////////////////////////////////////
  public void addDeployedArtifact(Object deployedArtifact) {
    if (deployedArtifacts == null) {
      deployedArtifacts = new HashMap<Class<?>, List<Object>>();
    }
    
    Class<?> clazz = deployedArtifact.getClass();
    List<Object> artifacts = deployedArtifacts.get(clazz);
    if (artifacts == null) {
      artifacts = new ArrayList<Object>();
      deployedArtifacts.put(clazz, artifacts);
    }
    
    artifacts.add(deployedArtifact);
  }
  
  @SuppressWarnings("unchecked")
  public <T> List<T> getDeployedArtifacts(Class<T> clazz) {
    return (List<T>) deployedArtifacts.get(clazz);
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
  
  public String getTenantId() {
  	return tenantId;
  }

  public void setTenantId(String tenantId) {
  	this.tenantId = tenantId;
  }

  public void setResources(Map<String, ResourceEntity> resources) {
    this.resources = resources;
  }
  
  public Date getDeploymentTime() {
    return deploymentTime;
  }
  
  public void setDeploymentTime(Date deploymentTime) {
    this.deploymentTime = deploymentTime;
  }

  public boolean isNew() {
    return isNew;
  }
  
  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  
  // common methods  //////////////////////////////////////////////////////////

  @Override
  public String toString() {
    return "DeploymentEntity[id=" + id + ", name=" + name + "]";
  }
  
}
