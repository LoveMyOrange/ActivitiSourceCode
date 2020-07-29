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

import java.util.Map;

import org.activiti.engine.impl.persistence.entity.DeploymentEntity;





/**
 * @author Tom Baeyens
 * @author Joram Barrez
 *  定义了 部署流程资源的()  所有的部署器都需要实现该接口
 *
 *  默认有2个实现类
 *  BpmnDeployer
 *  RulesDeployer (规则部署器)
 */
public interface Deployer {

  void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings);
}




















