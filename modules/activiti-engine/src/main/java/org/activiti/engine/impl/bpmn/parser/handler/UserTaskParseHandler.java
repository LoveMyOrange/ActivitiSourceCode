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
package org.activiti.engine.impl.bpmn.parser.handler;

import java.util.HashSet;
import java.util.Set;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.calendar.DueDateBusinessCalendar;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.form.DefaultTaskFormHandler;
import org.activiti.engine.impl.form.TaskFormHandler;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.apache.commons.lang3.StringUtils;


/**
 * @author Joram Barrez
 * 任务节点对象解析器 类
 *
 * 由于Activiti流程文档中的元素很多,所以对应的对象解析器 也有很多, 并且一个对象可能有多个解析器
 *
 * 两者 为 1对多 的关系,  因为对象解析逻辑大体相同,
 *
 */
public class UserTaskParseHandler extends AbstractActivityBpmnParseHandler<UserTask> {
  
  public static final String PROPERTY_TASK_DEFINITION = "taskDefinition";
  
  public Class< ? extends BaseElement> getHandledType() {
    return UserTask.class;
  }
  
  protected void executeParse(BpmnParse bpmnParse, UserTask userTask) {
    /*
    1)创建ActivityImpl对象
    该类为所有元素怒对象(连线元素对象除外) 在PVM 中的内部表示,
    ActivityImpl类是PVM 中非常重要的一个类, 主要存储节点的名称,类型,描述,监听器等信息
    该对象由 createActivityOnCurrentScope() 负责创建 ,为何不直接在该类中进行对象的创建工作呢???
      因为大部分对象解析时都需要创建该对象  所以
      该对象的创建工作 在 此类的父类, AbstractBpmnParseHandler 中提供了默认实现
      代码复用原则 ,
      唯一需要区别的地方就是 对象的类型 以及属性 而已

     */
    ActivityImpl activity = createActivityOnCurrentScope(bpmnParse, userTask, BpmnXMLConstants.ELEMENT_TASK_USER);
    /*
    * 对于userTask对象在PVM中的内部表示 ActivityImpl 来说, 该对象需要添加 async ,notExclusive 以及taskDefinition等属性
    * */
    activity.setAsync(userTask.isAsynchronous()); //设置任务节点的 async属性
    activity.setExclusive(!userTask.isNotExclusive()); //notExculsive属性
    /*
    解析配置信息
    此() 是为了 获取对象中(在这里指 任务节点) 已经存在的属性信息 并将其 设置到ActivityImpl 对象 的properties
    属性中, 该() 位于 UserTaskParseHandler中
    因为 该()主要解析 userTask 节点 ,所以完全没有必要 将 此() 放在 父类中
     */

    TaskDefinition taskDefinition = parseTaskDefinition(bpmnParse, userTask, userTask.getId(), (ProcessDefinitionEntity) bpmnParse.getCurrentScope().getProcessDefinition());
    /*
    设置taskDefinition 属性值
    setProperty() 内部使用Map存储
    而不需要在AcitivityImpl 类中单独定义一个个属性进行存储 ,
    由于Activiti 中元素的类型非常多, 因此使用Map 集合存储 差异化的属性是最灵活的方式
     */
    activity.setProperty(PROPERTY_TASK_DEFINITION, taskDefinition);//
    /*
    添加任务节点的行为类
    行为类 决定了 任务节点完成之后 可以到达的目的地,途径的连线信息等
     */
    activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createUserTaskActivityBehavior(userTask, taskDefinition));
  }
  /*
  * 因为所有的流程元素 都可以添加不同类型的执行监听器,
  * 所以createExecutionListenersOnScope() 全局调度执行监听器的添加工作
  *
  * 但是 由于 任务监听器 只可以在任务节点中进行配置使用,
  * 所有没有必要将任务监听器的解析添加工作放到公共() 中 ,只需要解析任务节点的时候处理即可
  *
  * parseTaskDefinition() 是将任务节点对象解析之后的结果 封装到TaskDefinition对象中
  *
  * 任务监听器中的all事件 在TaskDefinition类的 addTaskListener() 中进行判断处理
  *
  * createTaskListenr () 的知性逻辑和 创建执行监听器 逻辑类似
  * 唯一区别是 这里需要创建 TaskListener 对象 而后者需要创建ExecutionListener
  * */
  public TaskDefinition parseTaskDefinition(BpmnParse bpmnParse, UserTask userTask, String taskDefinitionKey, ProcessDefinitionEntity processDefinition) {
    TaskFormHandler taskFormHandler = new DefaultTaskFormHandler();
    taskFormHandler.parseConfiguration(userTask.getFormProperties(), userTask.getFormKey(), bpmnParse.getDeployment(), processDefinition);

    TaskDefinition taskDefinition = new TaskDefinition(taskFormHandler);

    taskDefinition.setKey(taskDefinitionKey);
    processDefinition.getTaskDefinitions().put(taskDefinitionKey, taskDefinition);
    ExpressionManager expressionManager = bpmnParse.getExpressionManager();

    if (StringUtils.isNotEmpty(userTask.getName())) {
      taskDefinition.setNameExpression(expressionManager.createExpression(userTask.getName()));
    }

    if (StringUtils.isNotEmpty(userTask.getDocumentation())) {
      taskDefinition.setDescriptionExpression(expressionManager.createExpression(userTask.getDocumentation()));
    }

    if (StringUtils.isNotEmpty(userTask.getAssignee())) {
      taskDefinition.setAssigneeExpression(expressionManager.createExpression(userTask.getAssignee()));
    }
    if (StringUtils.isNotEmpty(userTask.getOwner())) {
      taskDefinition.setOwnerExpression(expressionManager.createExpression(userTask.getOwner()));
    }
    for (String candidateUser : userTask.getCandidateUsers()) {
      taskDefinition.addCandidateUserIdExpression(expressionManager.createExpression(candidateUser));
    }
    for (String candidateGroup : userTask.getCandidateGroups()) {
      taskDefinition.addCandidateGroupIdExpression(expressionManager.createExpression(candidateGroup));
    }
    
    // Activiti custom extension
    
    // Task listeners
    for (ActivitiListener taskListener : userTask.getTaskListeners()) {
      taskDefinition.addTaskListener(taskListener.getEvent(), createTaskListener(bpmnParse, taskListener, userTask.getId()));
    }

    // Due date
    if (StringUtils.isNotEmpty(userTask.getDueDate())) {
      taskDefinition.setDueDateExpression(expressionManager.createExpression(userTask.getDueDate()));
    }

    // Business calendar name
    if (StringUtils.isNotEmpty(userTask.getBusinessCalendarName())) {
      taskDefinition.setBusinessCalendarNameExpression(expressionManager.createExpression(userTask.getBusinessCalendarName()));
    } else {
      taskDefinition.setBusinessCalendarNameExpression(expressionManager.createExpression(DueDateBusinessCalendar.NAME));
    }

    // Category
    if (StringUtils.isNotEmpty(userTask.getCategory())) {
    	taskDefinition.setCategoryExpression(expressionManager.createExpression(userTask.getCategory()));
    }
    
    // Priority
    if (StringUtils.isNotEmpty(userTask.getPriority())) {
      taskDefinition.setPriorityExpression(expressionManager.createExpression(userTask.getPriority()));
    }
    
    if (StringUtils.isNotEmpty(userTask.getFormKey())) {
    	taskDefinition.setFormKeyExpression(expressionManager.createExpression(userTask.getFormKey()));
    }

    // CustomUserIdentityLinks
    for (String customUserIdentityLinkType : userTask.getCustomUserIdentityLinks().keySet()) {
    	Set<Expression> userIdentityLinkExpression = new HashSet<Expression>();
    	for (String userIdentityLink : userTask.getCustomUserIdentityLinks().get(customUserIdentityLinkType)) {
    		userIdentityLinkExpression.add(expressionManager.createExpression(userIdentityLink));
    	}
    	taskDefinition.addCustomUserIdentityLinkExpression(customUserIdentityLinkType, userIdentityLinkExpression);
      }
    
    // CustomGroupIdentityLinks
    for (String customGroupIdentityLinkType : userTask.getCustomGroupIdentityLinks().keySet()) {
    	Set<Expression> groupIdentityLinkExpression = new HashSet<Expression>();
    	for (String groupIdentityLink : userTask.getCustomGroupIdentityLinks().get(customGroupIdentityLinkType)) {
    		groupIdentityLinkExpression.add(expressionManager.createExpression(groupIdentityLink));
    	}
    	taskDefinition.addCustomGroupIdentityLinkExpression(customGroupIdentityLinkType, groupIdentityLinkExpression);
      }

    if (StringUtils.isNotEmpty(userTask.getSkipExpression())) {
      taskDefinition.setSkipExpression(expressionManager.createExpression(userTask.getSkipExpression()));
    }
    
    return taskDefinition;
  }
  
  protected TaskListener createTaskListener(BpmnParse bpmnParse, ActivitiListener activitiListener, String taskId) {
    TaskListener taskListener = null;

    if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(activitiListener.getImplementationType())) {
      taskListener = bpmnParse.getListenerFactory().createClassDelegateTaskListener(activitiListener); 
    } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(activitiListener.getImplementationType())) {
      taskListener = bpmnParse.getListenerFactory().createExpressionTaskListener(activitiListener);
    } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(activitiListener.getImplementationType())) {
      taskListener = bpmnParse.getListenerFactory().createDelegateExpressionTaskListener(activitiListener);
    }
    return taskListener;
  }
}
