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
package org.activiti.engine.impl.pvm.delegate;

import java.io.Serializable;


/**
 * @author Tom Baeyens
 *
 * 该接口定义了  execute()  该() 决定了 流程实例最终可以到达的目的地以及途经的连线
 *
 * execute() 作为 所有活动行为类的 模板() 存在 , 因此该() 处理逻辑比较复杂,
 * 需要考虑所有可能出现的情况
 *
 *
 */
public interface ActivityBehavior extends Serializable {

  void execute(ActivityExecution execution) throws Exception;
}
