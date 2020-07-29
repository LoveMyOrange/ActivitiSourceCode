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
package org.activiti.engine.impl.pvm.runtime;

import org.activiti.engine.impl.pvm.process.ScopeImpl;


/**
 * @author Tom Baeyens
 * 通知连线完成原子类
 * 负责触发当前节点中配置的 事件类型为end 的执行监听器 以及 指定下一个需要处理的原子类
 *
 * 该类 做了 2个操作
 *
 * 1) 指定下一步需要处理的原子类,
 * 2) 触发连线中事件类型为end的执行监听器
 */
public class AtomicOperationTransitionNotifyListenerEnd extends AbstractEventAtomicOperation {

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  protected String getEventName() {
    return org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    execution.performOperation(TRANSITION_DESTROY_SCOPE);
  }
}
