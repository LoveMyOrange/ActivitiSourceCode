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
package org.activiti.engine.repository;

import java.io.InputStream;
import java.util.Date;
import java.util.zip.ZipInputStream;

import org.activiti.bpmn.model.BpmnModel;

/**
 * Builder for creating new deployments.
 * 
 * A builder instance can be obtained through {@link org.activiti.engine.RepositoryService#createDeployment()}.
 * 
 * Multiple resources can be added to one deployment before calling the {@link #deploy()}
 * operation.
 * 
 * After deploying, no more changes can be made to the returned deployment
 * and the builder instance can be disposed.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @desc
 *   不管开发人员使用何种方式部署流程文档 最终都是通过流程引擎提供的API 进行操作 ,
 *   流程文档的部署步骤 首先是 通过
 *   ProcessEngine 接口的实现类 获取到  repositoryService 对象
 *   然后接触对象的 createDeployment() 创建 DeploymentBuilder 对象
 *   DeploymentBuilder 的默认实现类 为DeploymentBuilderImpl
 *
 *   1)inputstream  对应数据库表 ACT_GE_BYTEARRAY 表中的BYTES_列
 *   2)resourceName参数  资源名称  对应ACT_GE_BYTEARRAY 表中的NAME_列
 *   如果客户端没有干预部署器的初始化, 那么 资源名称必须以bpmn20.xml 或者bpmn作为后缀
 *   最后的结果是   ACT_RE_PROCDEF表中不会产生数据
 *
 *   如果使用zip方式 部署  ,
 *   则需要确保, 流程文档的后缀为.bpmn20.xml .bpmn
 *
 *   因为 该方式 部署资源时不能显式指定资源名称 ,
 *   所以使用该方式部署资源时,
 *   资源名称 默认为压缩包中的流程文档名称\
 *
 *   addInputstream 使用数据流的方式部署资源 (流程文档和流程定义图片等等)
 *
 *   addClasspathResource  使用classpath 方式直接读取项目中 classpath目录下指定的流程资源进行部署
 *
 *   addZipInputStream() 将流程资源文件 (流程文档和流程定义图片 ) 打包进行部署
 *   例如 将XML PNG   打包为zip 或者 bar  该方式支持多个流程资源文件一次性打包部署(批量部署)
 *      内部使用迭代器方式  循环遍历 压缩包中的文件并读取 相应的文件流
 *
 *   addString  通过字符串方式部署   例如 定义一个流程文档之后 将文档的内容读取出来 然后直接使用该方式进行部署
 *
 *   addBPMNModel    通过构造BpmnModel 对象进行部署 比较适合  自定义流程设计器,
 *
 *   deploy()  部署的核心方法 负责流程资源的部署
 *
 *   不管以何种方式 部署流程资源 最终都会调用 此对象中的 deploy()
 *
 *
 * 关于过滤重复部署
 * 进行了第一次部署之后,资源如果没有发生变化而再次进行部署,同样会将部署数据
 * 写入DB中, 想避免这种情况 可以调用 enableDuplicateFiltering
 * 该方法 在执行deploy()
 *
 * 如果发现该值为true ,会根据部署对象的名称查找最后一条部署记录
 * 如果发现最后一条部署记录与当前需要部署的记录一直,则不会重复部署
 *
 * 这里所说的资源项目 包括 资源名称与资源内容
 *
 * 也就是说   如果 XML信息一样 但是 name属性不一样, 仍然会重新部署
 *
 *
 */
public interface DeploymentBuilder {
  //通过inputstream
  DeploymentBuilder addInputStream(String resourceName, InputStream inputStream);
  //通过资源文件所在的classpath
  DeploymentBuilder addClasspathResource(String resource);
  //通过字符串的方式部署
  DeploymentBuilder addString(String resourceName, String text);
  //通过zip方式部署
  DeploymentBuilder addZipInputStream(ZipInputStream zipInputStream);
  //通过bpmnModel方式进行部署
  DeploymentBuilder addBpmnModel(String resourceName, BpmnModel bpmnModel);
  
  /**
   * If called, no XML schema validation against the BPMN 2.0 XSD.
   * 
   * Not recommended in general.
   */
  DeploymentBuilder disableSchemaValidation();
  
  /**
   * If called, no validation that the process definition is executable on the engine
   * will be done against the process definition.
   * 
   * Not recommended in general.
   */
  DeploymentBuilder disableBpmnValidation();
  
  /**
   * Gives the deployment the given name.
   */
  DeploymentBuilder name(String name);
  
  /**
   * Gives the deployment the given category.
   */
  DeploymentBuilder category(String category);
  
  /**
   * Gives the deployment the given tenant id.
   */
  DeploymentBuilder tenantId(String tenantId);
  
  /**
   * If set, this deployment will be compared to any previous deployment.
   * This means that every (non-generated) resource will be compared with the
   * provided resources of this deployment.
   */
  DeploymentBuilder enableDuplicateFiltering();
  
  /**
   * Sets the date on which the process definitions contained in this deployment
   * will be activated. This means that all process definitions will be deployed
   * as usual, but they will be suspended from the start until the given activation date.
   */
  DeploymentBuilder activateProcessDefinitionsOn(Date date);

  /**
   * Deploys all provided sources to the Activiti engine.
   * 根据提供的部署方式进行资源的部署
   */
  Deployment deploy();
  
}
