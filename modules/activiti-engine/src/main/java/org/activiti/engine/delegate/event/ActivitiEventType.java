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
package org.activiti.engine.delegate.event;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumeration containing all possible types of {@link ActivitiEvent}s.
 * 
 * @author Frederik Heremans
 * @desc  该枚举定义了所有可以操作的事件类型
 *
 *
 * 引擎内部所有ENTITY_*事件都是与实体相关的。下面的列表展示了实体事件与实体的对应关系：
 *
 * ENTITY_CREATED, ENTITY_INITIALIZED, ENTITY_DELETED: Attachment, Comment, Deployment, Execution, Group, IdentityLink, Job, Model, ProcessDefinition, ProcessInstance, Task, User.
 *
 * ENTITY_UPDATED: Attachment, Deployment, Execution, Group, IdentityLink, Job, Model, ProcessDefinition, ProcessInstance, Task, User.
 *
 * ENTITY_SUSPENDED, ENTITY_ACTIVATED: ProcessDefinition, ProcessInstance/Execution, Task.
 *
 */
public enum ActivitiEventType {

  /**
   * New entity is created.
   * 创建了一个新实体,实体包含在事件中
   *
   */
  ENTITY_CREATED,
  
  /**
   * New entity has been created and all child-entities that are created as a result of the creation of this
   * particular entity are also created and initialized.
   * 创建了一个新实体，初始化也完成了。如果这个实体的创建会包含子实体的创建，这个事件会在子实体都创建/初始化完成后被触发，这是与ENTITY_CREATED的区别。
   */
  ENTITY_INITIALIZED,
  
  /**
   * Existing entity us updated.
   * 更新了已经存在的实体,实体包含在事件中
   */
  ENTITY_UPDATED,
  
  /**
   * Existing entity is deleted.
   * 删除了已经存在的实体,实体包含在事件中
   */
  ENTITY_DELETED,
  
  /**
   * Existing entity has been suspended.
   * 暂停了已经存在的实体,实体包含在事件中, 会被 ProcessDefinitions ProcessInstance 和 Tasks抛出
   */
  ENTITY_SUSPENDED,
  
  /**
   * Existing entity has been activated.
   * 激活了已经存在的实体,实体包含在事件中, 会被 ProcessDefinitions ProcessInstance 和 Tasks抛出
   */
  ENTITY_ACTIVATED,
  
  /**
   * Timer has been fired successfully.
   * 触发了定时器,job包含在事件中
   */
  TIMER_FIRED,

  /**
   * Timer has been cancelled (e.g. user task on which it was bounded has been completed earlier than expected)
   * 取消了一个作业,事件包含取消的作业,作业可以通过API调用取消,任务完成之后对应的边界定时器也会取消,在新的流程定义发布时
   * 也会取消
   */
  JOB_CANCELED,

  /**
   * A job has been successfully executed.
   * 作业执行成功,作业和异常信息包含在事件中
   */
  JOB_EXECUTION_SUCCESS,
  
  /**
   * A job has been executed, but failed. Event should be an instance of a {@link ActivitiExceptionEvent}.
   * 作业执行失败,作业和异常信息包含在事件中
   */
  JOB_EXECUTION_FAILURE,
  
  /**
   * The retry-count on a job has been decremented.
   * 因为作业执行失败,导致重试次数减少,作业和异常信息包含在事件中
   */
  JOB_RETRIES_DECREMENTED,
  
  /**
   * An event type to be used by custom events. These types of events are never thrown by the engine itself,
   * only be an external API call to dispatch an event.
   */
  CUSTOM,
  
  /**
   * The process-engine that dispatched this event has been created and is ready for use.
   * 监听器监听的流程引擎已经创建完毕,并准备好接受API调用
   */
  ENGINE_CREATED,
  
  /**
   * The process-engine that dispatched this event has been closed and cannot be used anymore.
   *  监听器监听器的流程引擎已经关闭,不再接受API 调用
   */
  ENGINE_CLOSED,
  
  /**
   * An activity is starting to execute. This event is dispatch right before an activity is executed.
   * 一个节点开始执行
   */
  ACTIVITY_STARTED,
  
  /**
   * An activity has been completed successfully.
   * 一个节点成功结束
   */
  ACTIVITY_COMPLETED,

  /**
   * An activity has been cancelled because of boundary event.
   */
  ACTIVITY_CANCELLED,

  /**
   * An activity has received a signal. Dispatched after the activity has responded to the signal.
   * 一个节点收到了一个信号
   */
  ACTIVITY_SIGNALED,
  
  /**
   * An activity is about to be executed as a compensation for another activity. The event targets the
   * activity that is about to be executed for compensation.
   * 	一个节点将要被补偿。事件包含了将要执行补偿的节点id。
   */
  ACTIVITY_COMPENSATE,
  
  /**
   * An activity has received a message event. Dispatched before the actual message has been received by
   * the activity. This event will be either followed by a {@link #ACTIVITY_SIGNALLED} event or {@link #ACTIVITY_COMPLETE}
   * for the involved activity, if the message was delivered successfully.
   *
   * 	一个节点收到了一个错误事件。在节点实际处理错误之前触发。 事件的activityId对应着处理错误的节点。 这个事件后续会是ACTIVITY_SIGNALLED或ACTIVITY_COMPLETE， 如果错误发送成功的话。
   */
  ACTIVITY_MESSAGE_RECEIVED,
  
  /**
   * An activity has received an error event. Dispatched before the actual error has been received by
   * the activity. This event will be either followed by a {@link #ACTIVITY_SIGNALLED} event or {@link #ACTIVITY_COMPLETE}
   * for the involved activity, if the error was delivered successfully.
   */
  ACTIVITY_ERROR_RECEIVED,
  
  /**
   * A event dispatched when a {@link HistoricActivityInstance} is created. 
   * This is a specialized version of the {@link ActivitiEventType#ENTITY_CREATED} and {@link ActivitiEventType#ENTITY_INITIALIZED} event,
   * with the same use case as the {@link ActivitiEventType#ACTIVITY_STARTED}, but containing
   * slightly different data.
   * 
   * Note this will be an {@link ActivitiEntityEvent}, where the entity is the {@link HistoricActivityInstance}.
   *  
   * Note that history (minimum level ACTIVITY) must be enabled to receive this event.  
   */
  HISTORIC_ACTIVITY_INSTANCE_CREATED,
  
  /**
   * A event dispatched when a {@link HistoricActivityInstance} is marked as ended. 
   * his is a specialized version of the {@link ActivitiEventType#ENTITY_UPDATED} event,
   * with the same use case as the {@link ActivitiEventType#ACTIVITY_COMPLETED}, but containing
   * slightly different data (e.g. the end time, the duration, etc.). 
   *  
   * Note that history (minimum level ACTIVITY) must be enabled to receive this event.  
   */
  HISTORIC_ACTIVITY_INSTANCE_ENDED,

  /**
   * Indicates the engine has taken (ie. followed) a sequenceflow from a source activity to a target activity.
   */
  SEQUENCEFLOW_TAKEN,
  
  /**
   * When a BPMN Error was thrown, but was not caught within in the process.
   * 抛出了未捕获的BPMN错误。流程没有提供针对这个错误的处理器。 事件的activityId为空。
   */
  UNCAUGHT_BPMN_ERROR,
  
  /**
   * A new variable has been created.
   * 创建了一个变量。事件包含变量名，变量值和对应的分支或任务（如果存在）。
   */
  VARIABLE_CREATED,
  
  /**
   * An existing variable has been updated.
   * 更新了一个变量。事件包含变量名，变量值和对应的分支或任务（如果存在）。
   */
  VARIABLE_UPDATED,
  
  /**
   * An existing variable has been deleted.
   * 删除了一个变量。事件包含变量名，变量值和对应的分支或任务（如果存在）。
   */
  VARIABLE_DELETED,

  /**
   * A task has been created. This is thrown when task is fully initialized (before TaskListener.EVENTNAME_CREATE).
   * 	创建了新任务。它位于ENTITY_CREATE事件之后。当任务是由流程创建时， 这个事件会在TaskListener执行之前被执行。
   */
  TASK_CREATED,

  /**
   * A task as been assigned. This is thrown alongside with an {@link #ENTITY_UPDATED} event.
   * 任务被分配给了一个人员,事件包含任务
   */
  TASK_ASSIGNED,
  
  /**
   * A task has been completed. Dispatched before the task entity is deleted ({@link #ENTITY_DELETED}).
   * If the task is part of a process, this event is dispatched before the process moves on, as a result of
   * the task completion. In that case, a {@link #ACTIVITY_COMPLETED} will be dispatched after an event of this type
   * for the activity corresponding to the task.
   * 任务被完成了。它会在ENTITY_DELETE事件之前触发。当任务是流程一部分时，事件会在流程继续运行之前， 后续事件将是ACTIVITY_COMPLETE，对应着完成任务的节点。
   */
  TASK_COMPLETED,

    /**
     * A process instance has been started. Dispatched when starting a process instance previously created. The event
     * PROCESS_STARTED is dispatched after the associated event ENTITY_INITIALIZED.
     */
  PROCESS_STARTED,

  /**
   * A process has been completed. Dispatched after the last activity is ACTIVITY_COMPLETED. Process is completed
   * when it reaches state in which process instance does not have any transition to take.
   * 	流程已结束。在最后一个节点的ACTIVITY_COMPLETED事件之后触发。 当流程到达的状态，没有任何后续连线时， 流程就会结束。
   */
  PROCESS_COMPLETED,
  
  /**
   * A process has been completed with an error end event.
   */
  PROCESS_COMPLETED_WITH_ERROR_END_EVENT,

  /**
   * A process has been cancelled. Dispatched when process instance is deleted by
   * @see org.activiti.engine.impl.RuntimeServiceImpl#deleteProcessInstance(java.lang.String, java.lang.String), before
   * DB delete.
   */
  PROCESS_CANCELLED,
  
  /**
   * A event dispatched when a {@link HistoricProcessInstance} is created. 
   * This is a specialized version of the {@link ActivitiEventType#ENTITY_CREATED} and {@link ActivitiEventType#ENTITY_INITIALIZED} event,
   * with the same use case as the {@link ActivitiEventType#PROCESS_STARTED}, but containing
   * slightly different data (e.g. the start time, the start user id, etc.). 
   * 
   * Note this will be an {@link ActivitiEntityEvent}, where the entity is the {@link HistoricProcessInstance}.
   *  
   * Note that history (minimum level ACTIVITY) must be enabled to receive this event.  
   */
  HISTORIC_PROCESS_INSTANCE_CREATED,
  
  /**
   * A event dispatched when a {@link HistoricProcessInstance} is marked as ended. 
   * his is a specialized version of the {@link ActivitiEventType#ENTITY_UPDATED} event,
   * with the same use case as the {@link ActivitiEventType#PROCESS_COMPLETED}, but containing
   * slightly different data (e.g. the end time, the duration, etc.). 
   *  
   * Note that history (minimum level ACTIVITY) must be enabled to receive this event.  
   */
  HISTORIC_PROCESS_INSTANCE_ENDED,

  /**
   * A new membership has been created.
   * 用户被添加到一个组里。事件包含了用户和组的id。
   */
  MEMBERSHIP_CREATED,
  
  /**
   * A single membership has been deleted.
   * 用户被从一个组中删除。事件包含了用户和组的id。
   */
  MEMBERSHIP_DELETED,
  
  /**
   * All memberships in the related group have been deleted. No individual {@link #MEMBERSHIP_DELETED} events will
   * be dispatched due to possible performance reasons. The event is dispatched before the memberships are deleted,
   * so they can still be accessed in the dispatch method of the listener.
   *
   * 	所有成员被从一个组中删除。在成员删除之前触发这个事件，所以他们都是可以访问的。 因为性能方面的考虑，不会为每个成员触发单独的MEMBERSHIP_DELETED事件。
   */
  MEMBERSHIPS_DELETED;

  public static final ActivitiEventType[] EMPTY_ARRAY =  new ActivitiEventType[] {};
  
  /**
   * @param string the string containing a comma-separated list of event-type names
   * @return a list of {@link ActivitiEventType} based on the given list.
   * @throws ActivitiIllegalArgumentException when one of the given string is not a valid type name
   */
  public static ActivitiEventType[] getTypesFromString(String string) {
    List<ActivitiEventType> result = new ArrayList<ActivitiEventType>();
    if(string != null && !string.isEmpty()) {
      String[] split = StringUtils.split(string, ",");
      for(String typeName : split) {
        boolean found = false;
        for(ActivitiEventType type : values()) {
          if(typeName.equals(type.name())) {
            result.add(type);
            found = true;
            break;
          }
        }
        if(!found) {
          throw new ActivitiIllegalArgumentException("Invalid event-type: " + typeName);
        }
      }
    }
    
    return result.toArray(EMPTY_ARRAY);
  }
}
