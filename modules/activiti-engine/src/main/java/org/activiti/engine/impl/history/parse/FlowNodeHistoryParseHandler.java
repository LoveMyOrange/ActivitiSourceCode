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
package org.activiti.engine.impl.history.parse;

import java.util.HashSet;
import java.util.Set;

import org.activiti.bpmn.model.*;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.history.handler.ActivityInstanceEndHandler;
import org.activiti.engine.impl.history.handler.ActivityInstanceStartHandler;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.parse.BpmnParseHandler;

/**
 * @author Joram Barrez
 * 负责历史节点对象的解析处理工作
 * 该类主要负责为其内部持有的  supportedElementClasses 集合中的对象自动注入不同的系统内置记录监听器
 *
 * 负责 解析流程 3大要素 并且为其添加事件类型为 start 和end 的内置记录监听器
 *
 * 通过此类代码可知
 *
 * 只要执行了此类的 parse() 就可以将
 * ActivityInstanceEndHandler
 * ActivityInstanceStartHandler
 * 两个记录监听器 自动添加到 BaseElement 对象中
 */
public class FlowNodeHistoryParseHandler implements BpmnParseHandler {
  /*
  该类 负责 监听器 流程3大要素 的结束 通知操作
  举个栗子, 就拿任务完成操作来说
  任务完成之后 ,会触发任务节点中事件类型为end的内置任务记录监听器中的业务逻辑
  也就是当前类  ActivityInstanceEndHandler 中的 notify()
  notify() 会更新当前任务节点对应的历史数据
  需要更新的属性 有 以下 3 个

   任务结束 时间 , 任务节点结束的原因,(非必须)  节点持续的时间(结束事件 -开始时间)

   为何需要更新 上面3个属性值呢/???  其他属性难道不需要修改或者更新吗??
   稍后说明

   */
  protected static final ActivityInstanceEndHandler ACTIVITI_INSTANCE_END_LISTENER = new ActivityInstanceEndHandler();
  /*
  该类负责监听 流程 3大要素的开始通知 操作
  该操作主要是将任务节点 已知的基本信息插入到历史环节表
  这就是 流程引擎 将 历史数据插入到 DB的时机
   */
  protected static final ActivityInstanceStartHandler ACTIVITY_INSTANCE_START_LISTENER = new ActivityInstanceStartHandler();
  /*
  用于存储  也可以动态添加 内置记录监听器的 BaseElement 对象
  开发人员也可以通过该集合 添加自定义的内置记录监听器 (只有存在于该集合中的BaseElement 对象 才可以被用来自动添加
  内置记录监听器

  历史解析器  是如何添加到 对象解析器集合中的呢 ???
   该过程调用了 BpmnParseHandlers类的 addHandlers()

      解析 此集合中的 BaseElement对象时,只要执行了 本类的 parse() 就可以轻松的将
      ActivityInstanceStartHandler  和 ActivityInstanceEndHandler 两个记录监听器自动添加到BaseElement对象中,
   */
  protected static Set<Class<? extends BaseElement>> supportedElementClasses = new HashSet<Class<? extends BaseElement>>();
  /*
  getHandlerTypes() 返回值正是该类静态代码块中初始化的集合 supportedElementClasses
  由于所有的流程元素对象都有可能添加内置记录监听器,
  因此没有必要单独为每一个元素添加一个历史解析器,
  统一维护即可
  如果为每一个baseElement 对象单独定义一个历史解析器, 工作量大不容易维护 扩展
  * */
  static {
    supportedElementClasses.add(EndEvent.class);
    supportedElementClasses.add(ThrowEvent.class);
    supportedElementClasses.add(BoundaryEvent.class);
    supportedElementClasses.add(IntermediateCatchEvent.class);

    supportedElementClasses.add(ExclusiveGateway.class);
    supportedElementClasses.add(InclusiveGateway.class);
    supportedElementClasses.add(ParallelGateway.class);
    supportedElementClasses.add(EventGateway.class);
    
    supportedElementClasses.add(Task.class);
    supportedElementClasses.add(ManualTask.class);
    supportedElementClasses.add(ReceiveTask.class);
    supportedElementClasses.add(ScriptTask.class);
    supportedElementClasses.add(ServiceTask.class);
    supportedElementClasses.add(BusinessRuleTask.class);
    supportedElementClasses.add(SendTask.class);
    supportedElementClasses.add(UserTask.class);
    
    supportedElementClasses.add(CallActivity.class);
    supportedElementClasses.add(SubProcess.class);
  }

  /*
  * 提供了获取集合元素的()   以方便其他模块可以获取到
  * */
  public Set<Class< ? extends BaseElement>> getHandledTypes() {
    return supportedElementClasses;
  }
/*
*
* */
  public void parse(BpmnParse bpmnParse, BaseElement element) {
    //1) 根据element对象的id 值 获取该对象的 PVM 对象 ActivityImpl
    ActivityImpl activity = bpmnParse.getCurrentScope().findActivity(element.getId());
    //根据类型 区分处理
    if(element instanceof BoundaryEvent) {// 如果是 BoundaryEvent 则为其添加结束记录监听器
    	// A boundary-event never receives an activity start-event
    	activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITY_INSTANCE_START_LISTENER, 0);
    	activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITI_INSTANCE_END_LISTENER, 1);
    } else {//否则 添加 开始和结束类型的监听器,  监听器的事件类型决定了历史归档记录操作的先后顺序
    	activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_START, ACTIVITY_INSTANCE_START_LISTENER, 0);
    	activity.addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, ACTIVITI_INSTANCE_END_LISTENER);
    }




  }

}















