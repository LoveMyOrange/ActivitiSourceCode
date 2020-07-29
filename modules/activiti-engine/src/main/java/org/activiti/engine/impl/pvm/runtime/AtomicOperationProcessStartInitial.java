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

import java.util.List;

import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;


/**
 * @author Tom Baeyens
 * 流程启动准备原子类
 */
public class AtomicOperationProcessStartInitial extends AbstractEventAtomicOperation {

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  protected String getEventName() {
    return org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_START;
  }
  /*
  *
  * */
  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    //获取ActivityImpl 对象 信息 (开始节点的信息)
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    //通过execution
    ProcessDefinitionImpl processDefinition = execution.getProcessDefinition();
    //获取 StartingExecution 对象
    StartingExecution startingExecution = execution.getStartingExecution();
    //判断 是否 相等
    if (activity==startingExecution.getInitial()) {
      // 调用此() 将execution对象中的 startingExecution 设置为空
      execution.disposeStartingExecution();
      //然设置下一个需要处理的原子类
      execution.performOperation(ACTIVITY_EXECUTE);

    } else {
      /*
      重新获取开始节点的信息, 并并且设置下一个需要处理的原子类  也就是当前类 ,

      该操作主要是为了 确保流程实例的启动节点 与流程文档中定义的开始节点 为 同一个节点
       */
      List<ActivityImpl> initialActivityStack = processDefinition.getInitialActivityStack(startingExecution.getInitial());
      int index = initialActivityStack.indexOf(activity);
      activity = initialActivityStack.get(index+1);

      InterpretableExecution executionToUse = null;
      if (activity.isScope()) {
        executionToUse = (InterpretableExecution) execution.getExecutions().get(0);
      } else {
        executionToUse = execution;
      }
      executionToUse.setActivity(activity);
      executionToUse.performOperation(PROCESS_START_INITIAL);
    }
  }
}
