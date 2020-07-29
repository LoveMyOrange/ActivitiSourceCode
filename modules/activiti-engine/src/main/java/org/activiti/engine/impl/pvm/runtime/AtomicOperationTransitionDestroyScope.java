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
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Tom Baeyens
 * 连线销毁原子类
 */
public class AtomicOperationTransitionDestroyScope implements AtomicOperation {
  
  private static Logger log = LoggerFactory.getLogger(AtomicOperationTransitionDestroyScope.class);
  
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }

  @SuppressWarnings("unchecked")
  /*
  *
  * */
  public void execute(InterpretableExecution execution) {
    InterpretableExecution propagatingExecution = null;

    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    // if this transition is crossing a scope boundary

    /*
    * isScope() 此()的 返回值 主要记录 引用流程 CallActivity ,Transaction元素 TimerEventDefinition等
    * 因为这些对象的处理 与常规的对象处理有差异,
    * 最简单的理解就是 引用流程 或者多实例节点 在流程实例的运转过程中
    * 均存在执行实例和 流程实例的概念,
    * 因此需要使用isScope() 属性表达式
    *
    * 可以使用Eclipse 打开开 ActivityImpl 类 然后定位setScope     就可以看到哪些元素支持该属性
    * */
    if (activity.isScope()) {
      
      InterpretableExecution parentScopeInstance = null;
      // if this is a concurrent execution crossing a scope boundary
      if (execution.isConcurrent() && !execution.isScope()) {
        // first remove the execution from the current root
        InterpretableExecution concurrentRoot = (InterpretableExecution) execution.getParent();
        parentScopeInstance = (InterpretableExecution) execution.getParent().getParent();

        log.debug("moving concurrent {} one scope up under {}", execution, parentScopeInstance);
        List<InterpretableExecution> parentScopeInstanceExecutions = (List<InterpretableExecution>) parentScopeInstance.getExecutions();
        List<InterpretableExecution> concurrentRootExecutions = (List<InterpretableExecution>) concurrentRoot.getExecutions();
        // if the parent scope had only one single scope child
        if (parentScopeInstanceExecutions.size()==1) {
          // it now becomes a concurrent execution
          parentScopeInstanceExecutions.get(0).setConcurrent(true);
        }
        
        concurrentRootExecutions.remove(execution);
        parentScopeInstanceExecutions.add(execution);
        execution.setParent(parentScopeInstance);
        execution.setActivity(activity);
        propagatingExecution = execution;
        
        // if there is only a single concurrent execution left
        // in the concurrent root, auto-prune it.  meaning, the 
        // last concurrent child execution data should be cloned into
        // the concurrent root.   
        if (concurrentRootExecutions.size()==1) {
          InterpretableExecution lastConcurrent = concurrentRootExecutions.get(0);
          if (lastConcurrent.isScope()) {
            lastConcurrent.setConcurrent(false);
            
          } else {
            log.debug("merging last concurrent {} into concurrent root {}", lastConcurrent, concurrentRoot);
            
            // We can't just merge the data of the lastConcurrent into the concurrentRoot.
            // This is because the concurrent root might be in a takeAll-loop.  So the 
            // concurrent execution is the one that will be receiving the take
            concurrentRoot.setActivity((ActivityImpl) lastConcurrent.getActivity());
            concurrentRoot.setActive(lastConcurrent.isActive());
            lastConcurrent.setReplacedBy(concurrentRoot);
            lastConcurrent.remove();
          }
        }

      } else if (execution.isConcurrent() && execution.isScope()) {
        log.debug("scoped concurrent {} becomes concurrent and remains under {}", execution, execution.getParent());

        // TODO!
        execution.destroy();
        propagatingExecution = execution;
        
      } else {
        propagatingExecution = (InterpretableExecution) execution.getParent();
        propagatingExecution.setActivity((ActivityImpl) execution.getActivity());
        propagatingExecution.setTransition(execution.getTransition());
        propagatingExecution.setActive(true);
        log.debug("destroy scope: scoped {} continues as parent scope {}", execution, propagatingExecution);
        execution.destroy();
        execution.remove();
      }
      
    } else {
      propagatingExecution = execution;
    }
    
    // if there is another scope element that is ended
    /*
    * 获取 activity对象的parent属性值
    * */
    ScopeImpl nextOuterScopeElement = activity.getParent();
    //获取当前节点的连线信息
    TransitionImpl transition = propagatingExecution.getTransition();
    //通过transition 获取当前节点的目标节点 ,也就是流程实例最终需要运转的节点,
    ActivityImpl destination = transition.getDestination();
    //如果 nextOuterScopeElement 中存在目标节点, 则
    if (transitionLeavesNextOuterScope(nextOuterScopeElement, destination)) {

      propagatingExecution.setActivity((ActivityImpl) nextOuterScopeElement);
      //直接设置 下一个需要处理的原子类
      propagatingExecution.performOperation(TRANSITION_NOTIFY_LISTENER_END);
    } else {
      //否则设置下一个需要处理的原子类
      propagatingExecution.performOperation(TRANSITION_NOTIFY_LISTENER_TAKE);
    }
  }

  public boolean transitionLeavesNextOuterScope(ScopeImpl nextScopeElement, ActivityImpl destination) {
    return !nextScopeElement.contains(destination);
  }
}
