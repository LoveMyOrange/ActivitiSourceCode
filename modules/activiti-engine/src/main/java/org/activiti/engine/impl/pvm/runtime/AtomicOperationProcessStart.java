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
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 *
 * 流程启动原子类
 *  流程实例运转的时候 是如何使用职责链呢??
 */
public class AtomicOperationProcessStart extends AbstractEventAtomicOperation {

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return execution.getProcessDefinition();
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

  	if (Context.getProcessEngineConfiguration() != null && Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
  	  Map<String, Object> variablesMap = null;
  	  try {
  	    variablesMap = execution.getVariables();
  	  } catch (Throwable t) {
  	    // In some rare cases getting the execution variables can fail (JPA entity load failure for example)
  	    // We ignore the exception here, because it's only meant to include variables in the initialized event.
  	  }
        //转发Entity_INITALIZED
    	Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
    			ActivitiEventBuilder.createEntityWithVariablesEvent(ActivitiEventType.ENTITY_INITIALIZED, 
    			    execution, variablesMap, false));
      Context.getProcessEngineConfiguration().getEventDispatcher()
              .dispatchEvent(ActivitiEventBuilder.createProcessStartedEvent(execution, variablesMap, false));
    }
  	//获取ProcessDefinitionImpl类
    ProcessDefinitionImpl processDefinition = execution.getProcessDefinition();
  	// 获取StartExecution 对象  StartingExecution中 持有开始节点在PVM 中的表示类
    StartingExecution startingExecution = execution.getStartingExecution();
    //确保开始节点已经存储到 processDefinition对象中
    List<ActivityImpl> initialActivityStack = processDefinition.getInitialActivityStack(startingExecution.getInitial());
    //将开始节点存储 到 execution 对象中, 方便后续操作可获取上一个已经执行的 ActivityImpl 对象
    execution.setActivity(initialActivityStack.get(0));
    //设置 下一个需要处理的原子类
    execution.performOperation(PROCESS_START_INITIAL);
  }
}


















