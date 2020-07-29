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



/**
 * @author Tom Baeyens
 * 在 ActivityBehavior 的 基础上 增加了 处理 多实例 或者流程元素的()   lastExecutionEnded()
 *
 * 例如 多实例任务流转时
 * 流程实例 永远只有1个, 但是 执行实例可以存在多个,
 * 而且流程实例做为 所有执行实例的parent存在
 * 当多实例节点结束时 需要根据 当前的执行实例 查询顶级parent信息 也就是流程实例信息
 * 并且最终决定流程实例运转的目的地
 * 所以多实例任务  需要与 非多实例任务 区别对待
 *
 */
public interface CompositeActivityBehavior extends ActivityBehavior {

  void lastExecutionEnded(ActivityExecution execution);
}
