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

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.PvmException;
import org.activiti.engine.impl.pvm.process.ScopeImpl;


/**
 * @author Tom Baeyens
 * 为何用户配置的执行监听器 或者任务监听器 需要使用不同的类封装呢 ??
 *  需要涉及到 引擎触发监听器的原理
 *
 *   引起是如何触发监听器呢???
 *      所有的执行监听器的调用 均通过 此类 触发
 *
 *      该()  对父类进行实现
 *      execute()中  调用了3个抽象()
 *      1)getScope()  获取ScopeImpl类对象
 *      2)getEventName()  获取监听器的事件类型
 *      3)eventNotificationsCompleted() 通知当前事件完成 并且设置下一个原子类
 *
 *      在此类 以及子类中的isAsync() 返回值为fasle
 *      但是 在 AtomicOperationTransitionCreateScope 原子类中的 isAsync() 中 返回值 是从
 *      ActivityImpl对象中 动态获取的
 *
 */
public abstract class AbstractEventAtomicOperation implements AtomicOperation {
  
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }
/*
*此()的处理逻辑以 执行监听器的有无作为分水岭
* 将执行监听器和非执行监听器的处理逻辑分开
* 如果存在执行监听器则会直接调用       execution.performOperation(this); 并且this指针传递过去
*
* 如果当前当前处理类调用execute()的同时 需要处理执行监听器处理完毕才会设置下一个需要处理的原子类
*
* 如果没有执行监听器或者所有的执行监听器 执行完毕,则开始执行 else中的代码
*
*  execution.setExecutionListenerIndex(0);
      execution.setEventName(null);
      execution.setEventSource(null);
      eventNotificationsCompleted(execution); //设置下一步需要处理的原子类
* */
  public void execute(InterpretableExecution execution) {
    /*
    因为流程实例运转时 ,需要知道当前锁处理的节点,  所以直接根据execution从PVM 中获取当前节点的信息值
     */
    ScopeImpl scope = getScope(execution);//获取scope对象
    //获取同一种事件类型的监听器集合  getEventName() 获取执行监听器的事件类型 如 start 或者 end
    List<ExecutionListener> exectionListeners = scope.getExecutionListeners(getEventName());
    /*
    executionListenerIndex 此变量有何意义??
    例如在开始节点中 定义了2个事件类型为start的执行监听器
    这样 可以通过 getEventName 为start的时候  获取这两个执行监听器集合
     */
    int executionListenerIndex = execution.getExecutionListenerIndex();
    //循环遍历 监听器集合 , 因为同一个元素的同一个事件类型的执行监听器可以存在多个 ,所以需要根据事件类型循环遍历集合中的监听器并且执行
    if (exectionListeners.size()>executionListenerIndex) {
      /*
      封装execution    因为出发执行监听器时 需要将 InterpretalveExecution(ExecutionEntity) 对象作为notify()的输入参数 进行传递
      传递该参数 主要是为了方便开发人员 直接通过notify() 中的 DelegateExecution类型的参数获取需要的信息
       */
      execution.setEventName(getEventName());
      execution.setEventSource(scope);
      ExecutionListener listener = exectionListeners.get(executionListenerIndex);
      try {
        /*
        * 因为开发人员配置的执行监听器或者系统内置的监听器 都需要实现 EecutionListenr接口 中定义的notfiy()
        * 所以该 () 会触发执行监听器中的 notify()
        * */
        listener.notify(execution); //触发监听器中的notify()
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new PvmException("couldn't execute event listener : "+e.getMessage(), e);
      }
      // 原子类的 运转过程
      execution.setExecutionListenerIndex(executionListenerIndex+1);
      execution.performOperation(this);

    } else {
      execution.setExecutionListenerIndex(0);
      execution.setEventName(null);
      execution.setEventSource(null);
      
      eventNotificationsCompleted(execution);//设置下一步需要处理的原子类
    }
  }

  protected abstract ScopeImpl getScope(InterpretableExecution execution);
  protected abstract String getEventName();
  protected abstract void eventNotificationsCompleted(InterpretableExecution execution);
}
