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

package org.activiti.engine.impl.pvm.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.PvmProcessInstance;
import org.activiti.engine.impl.pvm.runtime.ExecutionImpl;
import org.activiti.engine.impl.pvm.runtime.InterpretableExecution;



/**
 * @author Tom Baeyens
 * 对  接口中定义的() 进行了实现
 */
public class ProcessDefinitionImpl extends ScopeImpl implements PvmProcessDefinition {
  
  private static final long serialVersionUID = 1L;
  
  protected String name;
  protected String key;
  protected String description;
  /*
  * 封装了开始节点 的 ActivityImpl
  * */
  protected ActivityImpl initial;
  protected Map<ActivityImpl, List<ActivityImpl>> initialActivityStacks = new HashMap<ActivityImpl, List<ActivityImpl>>();
  protected List<LaneSet> laneSets;
  protected ParticipantProcess participantProcess;

  public ProcessDefinitionImpl(String id) {
    super(id, null);
    processDefinition = this;
  }
  /*
  * 创建ExecutionEntity对象
  * ExecutionEntity对象的创建工作委托给 ProcessDefinitionEntity的父类 ProcessDefinitioniMPL
  * */
  public PvmProcessInstance createProcessInstance() {
    /*
    * initial 属性为 ActivityImpl 类型
    * 具体存储哪一个节点的信息呢???
    * 思考流程实例启动的时候 最先执行的是文档中的哪一个节点呢???
    * 当然是开始节点, 流程文档 如果没有定义开始节点好比是无水之源,,
    * 所以initial 参数值就是开始节点的信息
    *
    * 关于该参数的赋值操作, 可以参考
    * StartEventParseHandler 类的 selectInitial() 中的实现
    * */
    if(initial == null) {
      throw new ActivitiException("Process '"+name+"' has no default start activity (e.g. none start event), hence you cannot use 'startProcessInstanceBy...' but have to start it using one of the modeled start events (e.g. message start events).");
    }
    return createProcessInstanceForInitial(initial);
  }
  
  /** creates a process instance using the provided activity as initial */
  /*
  * 委托 此() 创建 EexecutionEntity 对象
  * ExecutionEntity 类实现了 PvmProcessInstance
  * */
  public PvmProcessInstance createProcessInstanceForInitial(ActivityImpl initial) {
      /*
      判断 initial 参数值
      因为该() 有可能被 StartProcessInstanceByMessageCmd (消息开始节点) 等其他类() 直接调用
      所以在这里进行参数值的校验工作是非常有必要的
       */
    if(initial == null) {
      throw new ActivitiException("Cannot start process instance, initial activity where the process instance should start is null.");
    }
    /*
    * 该类的实例化工作 委托 newProcessInstance() 处理
    *
    * 尽管 newProcessInstance () 的调用 位于 本类中
    * 但是也应该牢记 程序当前的this指针指向的是ProcessDefinitionEntity类的具体事例对象
    * 而并非ProcessDefinitionImpl 对象
    *
    * 由于ProcessDefinitionEntity 重写了 父类的 newProcessInstance
    * 所以程序最终调用的是 ProcessDefinitionenEntity类的 newProcessInstance
    * */
    InterpretableExecution processInstance = newProcessInstance(initial);
    /*
    *  主要是为 ExecutionEntity 填充属性
    * 填充 processDefinition ,processDefinitionId ,processDefinitionKey
    * */
    processInstance.setProcessDefinition(this);
    processInstance.setProcessInstance(processInstance); //设置流程实例ID
    /*
    * 确保当前是否存在流程实例, 如果存在 ,则需要初始化流程实例, 执行实例 已经执行定时作业
    * */
    processInstance.initialize();

    InterpretableExecution scopeInstance = processInstance;
    /*
    * 主要用于将开始节点的表示类 ActivityImpl 加入到ProcessDefinitionImpl 类中的 initialActivityStacks集合
    * 方便后续取值操作 ,然后将 initial 对象设置到 InterpretableExecution对象中
    * 方便后续流程实例启动时获取开始节点信息
    * */
    List<ActivityImpl> initialActivityStack = getInitialActivityStack(initial);
    
    for (ActivityImpl initialActivity: initialActivityStack) {
      if (initialActivity.isScope()) {
        scopeInstance = (InterpretableExecution) scopeInstance.createExecution();
        scopeInstance.setActivity(initialActivity);
        if (initialActivity.isScope()) {
          scopeInstance.initialize();
        }
      }
    }
    
    scopeInstance.setActivity(initial);

    return processInstance;
  }

  public List<ActivityImpl> getInitialActivityStack() {
    return getInitialActivityStack(initial);    
  }
  
  public synchronized List<ActivityImpl> getInitialActivityStack(ActivityImpl startActivity) {
    List<ActivityImpl> initialActivityStack = initialActivityStacks.get(startActivity);
    if(initialActivityStack == null) {
      initialActivityStack = new ArrayList<ActivityImpl>();
      ActivityImpl activity = startActivity;
      while (activity!=null) {
        initialActivityStack.add(0, activity);
        activity = activity.getParentActivity();
      }
      initialActivityStacks.put(startActivity, initialActivityStack);
    }
    return initialActivityStack;
  }

  protected InterpretableExecution newProcessInstance(ActivityImpl startActivity) {
    return new ExecutionImpl(startActivity);
  }

  public String getDiagramResourceName() {
    return null;
  }

  public String getDeploymentId() {
    return null;
  }
  
  public void addLaneSet(LaneSet newLaneSet) {
    getLaneSets().add(newLaneSet);
  }
  
  public Lane getLaneForId(String id) {
    if(laneSets != null && !laneSets.isEmpty()) {
      Lane lane;
      for(LaneSet set : laneSets) {
        lane = set.getLaneForId(id);
        if(lane != null) {
          return lane;
        }
      }
    }
    return null;
  }
  
  // getters and setters //////////////////////////////////////////////////////
  
  public ActivityImpl getInitial() {
    return initial;
  }

  public void setInitial(ActivityImpl initial) {
    this.initial = initial;
  }
  
  public String toString() {
    return "ProcessDefinition("+id+")";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return (String) getProperty("documentation");
  }
  
  /**
   * @return all lane-sets defined on this process-instance. Returns an empty list if none are defined. 
   */
  public List<LaneSet> getLaneSets() {
    if(laneSets == null) {
      laneSets = new ArrayList<LaneSet>();
    }
    return laneSets;
  }
  
  
  public void setParticipantProcess(ParticipantProcess participantProcess) {
    this.participantProcess = participantProcess;
  }
  
  public ParticipantProcess getParticipantProcess() {
    return participantProcess;
  }
}
