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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.bpmn.data.IOSpecification;
import org.activiti.engine.impl.pvm.PvmException;
import org.activiti.engine.impl.pvm.PvmScope;


/**
 * @author Tom Baeyens
 * 该类负责创建执行监听器,等工作
 * 内部使用Map集合缓存, ActivityImpl 对象 更重要的是 该类作为 抽象类 存在
 * 子类都可以复用该类的()
 */
public abstract class ScopeImpl extends ProcessElementImpl implements PvmScope {

  private static final long serialVersionUID = 1L;
  
  protected List<ActivityImpl> activities = new ArrayList<ActivityImpl>();
  protected Map<String, ActivityImpl> namedActivities = new HashMap<String, ActivityImpl>();
  protected Map<String, List<ExecutionListener>> executionListeners = new HashMap<String, List<ExecutionListener>>();
  protected IOSpecification ioSpecification;
  
  public ScopeImpl(String id, ProcessDefinitionImpl processDefinition) {
    super(id, processDefinition);
  }
  
  public ActivityImpl findActivity(String activityId) {
    ActivityImpl localActivity = namedActivities.get(activityId);
    if (localActivity!=null) {
      return localActivity;
    }
    for (ActivityImpl activity: activities) {
      ActivityImpl nestedActivity = activity.findActivity(activityId);
      if (nestedActivity!=null) {
        return nestedActivity;
      }
    }
    return null;
  }

  public ActivityImpl createActivity() {
    return createActivity(null);
  }
  /*
  * 根据activityId 和processDefinition 参数 实例化ActivityImpl类
  * 此类的构造会调用其父类 ScopeImpl的构造
  * 而ScopeImpl 会继续调用父类ProcessElementImpl的构造,
  * 因此以上所说的两个参数值 最终会存储到ProcessElementImpl类中
  *
  *
  * 以下操作完毕之后 , 就可以很轻松的通过ProcessDefinitionEntity 对象获取 namedActivities 或者
  * activities 集合 ,换言之, 任务节点的对象已经被成功注入到 PVM中了
  * */
  public ActivityImpl createActivity(String activityId) {
    //id 可以理解为流程文档中对应的元素 id processDefinition,参数对应 ProcessDefinitionEntity
    ActivityImpl activity = new ActivityImpl(activityId, processDefinition);
    if (activityId!=null) {
      /*
      根据activityId 调用processDefinition对象的  findActivity() 进行下一步处理
      如果查询到activityId 则程序直接报错,
      该操作是为了确保已经存在于namedActivities 集合 或者activities集合中的节点不会被覆盖掉
       */
      if (processDefinition.findActivity(activityId) != null) {
        throw new PvmException("duplicate activity id '" + activityId + "'");
      }
      /*
      该过程非常简单,首先尝试从nameActivities 集合中进行查找 ,如果查询到则直接返回,
      否则开始对 activities集合进行遍历查找,
      如果以上上述两个集合均不存在该值
      该()直接返回null


       */
      namedActivities.put(activityId, activity); //将activity对象添加到namedActivities集合中
    }
    /*
    this 表示 processDefinitionEntity对象

    设置ActivityImpl对象的parent属性值为当前执行对象, 也就是ProcessDefinitionEntity对象
    通过该操作之后 ,所有的ActivityImpl对象与ProcessDefinitionEntity 双向关联
    可以相互获取,
    流程在流程文档部署时 可以通过引擎提供的API 获取ProcessDefinitionEntity对象, 然后通过该对象获取ActivityImpl 对象
     */
    activity.setParent(this);
    /*
    添加到activities集合
    ActivityImpl 对象的创建工作是非常消耗系统资源的 ,该对象创建完成之后
    需要将已经创建的实例对象 添加到namedActivities 和activities 集合中进行缓存, 方便后续查找
    从 findActivity() 也可以看出 缓存该对象的意义

     */

    activities.add(activity);
    return  activity;
  }

  public boolean contains(ActivityImpl activity) {
    if (namedActivities.containsKey(activity.getId())) {
      return true;
    }
    for (ActivityImpl nestedActivity : activities) {
      if (nestedActivity.contains(activity)) {
        return true;
      }
    }
    return false;
  }
  
  // event listeners //////////////////////////////////////////////////////////
  
  @SuppressWarnings("unchecked")
  public List<ExecutionListener> getExecutionListeners(String eventName) {
    List<ExecutionListener> executionListenerList = getExecutionListeners().get(eventName);
    if (executionListenerList!=null) {
      return executionListenerList;
    }
    return Collections.EMPTY_LIST;
  }
  
  public void addExecutionListener(String eventName, ExecutionListener executionListener) {
    addExecutionListener(eventName, executionListener, -1);
  }
  
  public void addExecutionListener(String eventName, ExecutionListener executionListener, int index) {
    List<ExecutionListener> listeners = executionListeners.get(eventName);
    if (listeners==null) {
      listeners = new ArrayList<ExecutionListener>();
      executionListeners.put(eventName, listeners);
    }
    if (index<0) {
      listeners.add(executionListener);
    } else {
      listeners.add(index, executionListener);
    }
  }
  
  public Map<String, List<ExecutionListener>> getExecutionListeners() {
    return executionListeners;
  }
  
  // getters and setters //////////////////////////////////////////////////////
  
  public List<ActivityImpl> getActivities() {
    return activities;
  }

  public IOSpecification getIoSpecification() {
    return ioSpecification;
  }
  
  public void setIoSpecification(IOSpecification ioSpecification) {
    this.ioSpecification = ioSpecification;
  }
}
