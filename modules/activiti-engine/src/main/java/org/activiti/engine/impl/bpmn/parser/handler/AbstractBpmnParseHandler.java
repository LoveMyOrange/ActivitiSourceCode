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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.Artifact;
import org.activiti.bpmn.model.Association;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.DataSpec;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.EventGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Gateway;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.ValuedDataObject;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.bpmn.data.Data;
import org.activiti.engine.impl.bpmn.data.DataRef;
import org.activiti.engine.impl.bpmn.data.IOSpecification;
import org.activiti.engine.impl.bpmn.data.ItemDefinition;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.EventSubscriptionDeclaration;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.parse.BpmnParseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Joram Barrez
 * 该类是所有对象解析器的父类  定义了解析对象的 模板()
 * 对象的具体解析工作则移植到不同的子类进行实现,
 *
 */
public abstract class AbstractBpmnParseHandler<T extends BaseElement> implements BpmnParseHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractBpmnParseHandler.class);
  
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBpmnParseHandler.class);
  
  public static final String PROPERTYNAME_IS_FOR_COMPENSATION = "isForCompensation";
  
  public static final String PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION = "eventDefinitions";
  
  public static final String PROPERTYNAME_ERROR_EVENT_DEFINITIONS = "errorEventDefinitions";
  
  public static final String PROPERTYNAME_TIMER_DECLARATION = "timerDeclarations";
  /*
   获取对象集合
  * */
  public Set<Class< ? extends BaseElement>> getHandledTypes() {
    Set<Class< ? extends BaseElement>> types = new HashSet<Class<? extends BaseElement>>();
    types.add(getHandledType());
    return types;
  }
  /*抽象() 子类需要对其进行 实现 */
  protected abstract Class<? extends BaseElement> getHandledType();
  
  @SuppressWarnings("unchecked")
  /*
  *  该() 作为对象解析的模板() 存在, 全局调度对象解析工作
  * 重点是
  * 对象解析器中的executeParse ()
  *   告诉引擎如何解析对象
  *
  * getHandlerType()  这两个()
  *     告诉引擎(具体的对象解析器) 需要解析的具体对象
  *
  * */
  public void parse(BpmnParse bpmnParse, BaseElement element) {
    //首先需要获取解析对象的类型
    T baseElement = (T) element;
    //然后 调用 此() 进行对象解析工作, 具体实现则移交给不同的子类完成
    executeParse(bpmnParse, baseElement);
  }
  /*抽象()  负责具体对象的解析工作 */
  protected abstract void executeParse(BpmnParse bpmnParse, T element);
  /*
  * 根据流程文档中元素定义的id 值 从PVM 中获取 其对应的 ActivityImpl 对象
  * */
  protected ActivityImpl findActivity(BpmnParse bpmnParse, String id) {
    return bpmnParse.getCurrentScope().findActivity(id);
  }
  /*
  * bpmnParse.getCurrentScope() 操作就是 获取 bpmnParse 对象中的 currentScopeStack集合
  * 该集合存储的是ProcessDefinitionEntity对象
  * */
  public ActivityImpl createActivityOnCurrentScope(BpmnParse bpmnParse, FlowElement flowElement, String xmlLocalName) {
    return createActivityOnScope(bpmnParse, flowElement, xmlLocalName, bpmnParse.getCurrentScope());
  }
  /*
  * 创建ActivityImpl对象
  * */
  public ActivityImpl createActivityOnScope(BpmnParse bpmnParse, FlowElement flowElement, String xmlLocalName, ScopeImpl scopeElement) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Parsing activity {}", flowElement.getId());
    }
    //这里scopeElement在ScopeImpl类中    对应ProcessDefinitionEntity 类
    ActivityImpl activity = scopeElement.createActivity(flowElement.getId());
    bpmnParse.setCurrentActivity(activity);//设置当前解析的元素 添加到bpmnParse 对象中
    /*
    填充属性 , 常用的属性有  name docuemntation, type
    如果元素类型为 Activity  活动, 则需要设置 default以及 isForCompensation
    如果是 Gateway (网关)  则需要设置 default属性值
     */
    activity.setProperty("name", flowElement.getName()); //设置name值
    activity.setProperty("documentation", flowElement.getDocumentation());  //设置描述信息
    if (flowElement instanceof Activity) {//判断元素的类型 UserTask 是Activity 子类
      Activity modelActivity = (Activity) flowElement;
      activity.setProperty("default", modelActivity.getDefaultFlow());//设置任务节点默认的出线
      if(modelActivity.isForCompensation()) {//设置 isForCompensation 属性值
        activity.setProperty(PROPERTYNAME_IS_FOR_COMPENSATION, true);        
      }
    } else if (flowElement instanceof Gateway) {//如果元素是 网关类型 则需要设置默认值
      activity.setProperty("default", ((Gateway) flowElement).getDefaultFlow());
    }
    activity.setProperty("type", xmlLocalName);//向属性集合中添加元素的类型, 也就是元素的名称
    
    return activity;
  }
  /*
  * 为解析的对象添加执行监听器支持
  * */
  protected void createExecutionListenersOnScope(BpmnParse bpmnParse, List<ActivitiListener> activitiListenerList, ScopeImpl scope) {
    for (ActivitiListener activitiListener : activitiListenerList) { //遍历 监听器集合
      scope.addExecutionListener(activitiListener.getEvent(), createExecutionListener(bpmnParse, activitiListener));
    }
  }
  
  protected void createExecutionListenersOnTransition(BpmnParse bpmnParse, List<ActivitiListener> activitiListenerList, TransitionImpl transition) {
    for (ActivitiListener activitiListener : activitiListenerList) {

      transition.addExecutionListener(createExecutionListener(bpmnParse, activitiListener));
    }
  }
  /*
  * 创建执行监听器对象
  * 不管是执行监听器 还是 任务监听器,  元素解析完成之后都会使用ActivitiListener 对象进行表示
  *  第一个参数是 监听事件类型
  * 第2个参数是 ExecutionListener
  *
  * 这样后续的流程实例 运转的时候 ,如果探测到ActivityImpl对象中有执行监听器 ,则直接触发执行监听器中的()
  *
  * 为何需要将 ActivitiListenr 转化为 ExecutionListener 对象呢??
  * 因为所有的执行监听器都需要实现 ExecutionListener 接口,
  * 在这里进行 转化的目的
  *  是为了方便对所有的执行监听器进行全局统一调度 ,而无需关心其内部的具体实现细节
  * 这是典型的面向接口编程思想
  *
  *
  * 任务监听器 肯定也是这么做的
  *
  *
  * 此()主要用于创建执行监听器,
  * 以为 执行\任务监听器 都可以通过class,expression,delegateExpression 方式进行定义
  * 所以在执行监听器的创建过程中会根据 其创建方式 委托 给 DefaultListenerFactory类中不同的() 进行处理
  * */
  protected ExecutionListener createExecutionListener(BpmnParse bpmnParse, ActivitiListener activitiListener) {
    ExecutionListener executionListener = null;
    //根据执行监听器的类型实例化不同的ExecutionListener子类
    if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(activitiListener.getImplementationType())) {
      executionListener = bpmnParse.getListenerFactory().createClassDelegateExecutionListener(activitiListener);  
    } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(activitiListener.getImplementationType())) {
      executionListener = bpmnParse.getListenerFactory().createExpressionExecutionListener(activitiListener);
    } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(activitiListener.getImplementationType())) {
      executionListener = bpmnParse.getListenerFactory().createDelegateExpressionExecutionListener(activitiListener);
    }
    return executionListener;
  }
  
  @SuppressWarnings("unchecked")
  protected void addEventSubscriptionDeclaration(BpmnParse bpmnParse, EventSubscriptionDeclaration subscription, EventDefinition parsedEventDefinition, ScopeImpl scope) {
    List<EventSubscriptionDeclaration> eventDefinitions = (List<EventSubscriptionDeclaration>) scope.getProperty(PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION);
    if(eventDefinitions == null) {
      eventDefinitions = new ArrayList<EventSubscriptionDeclaration>();
      scope.setProperty(PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION, eventDefinitions);
    } else {
      // if this is a message event, validate that it is the only one with the provided name for this scope
      if(subscription.getEventType().equals("message")) {
        for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
          if(eventDefinition.getEventType().equals("message")
            && eventDefinition.getEventName().equals(subscription.getEventName()) 
            && eventDefinition.isStartEvent() == subscription.isStartEvent()) {
            
            logger.warn("Cannot have more than one message event subscription with name '" + subscription.getEventName() +
                "' for scope '"+scope.getId()+"'");
          }
        }
      }
    }  
    eventDefinitions.add(subscription);
  }
  
  protected String getPrecedingEventBasedGateway(BpmnParse bpmnParse, IntermediateCatchEvent event) {
    String eventBasedGatewayId = null;
    for (SequenceFlow sequenceFlow : event.getIncomingFlows()) {
      FlowElement sourceElement = bpmnParse.getBpmnModel().getFlowElement(sequenceFlow.getSourceRef());
      if (sourceElement instanceof EventGateway) {
        eventBasedGatewayId = sourceElement.getId();
        break;
      }
    }
    return eventBasedGatewayId;
  }
  /*
  * 创建IOSpecification对象
  * */
  protected IOSpecification createIOSpecification(BpmnParse bpmnParse, org.activiti.bpmn.model.IOSpecification specificationModel) {
    IOSpecification ioSpecification = new IOSpecification();

    for (DataSpec dataInputElement : specificationModel.getDataInputs()) {
      ItemDefinition itemDefinition = bpmnParse.getItemDefinitions().get(dataInputElement.getItemSubjectRef());
      Data dataInput = new Data(bpmnParse.getTargetNamespace() + ":" + dataInputElement.getId(), dataInputElement.getId(), itemDefinition);
      ioSpecification.addInput(dataInput);
    }

    for (DataSpec dataOutputElement : specificationModel.getDataOutputs()) {
      ItemDefinition itemDefinition = bpmnParse.getItemDefinitions().get(dataOutputElement.getItemSubjectRef());
      Data dataOutput = new Data(bpmnParse.getTargetNamespace() + ":" + dataOutputElement.getId(), dataOutputElement.getId(), itemDefinition);
      ioSpecification.addOutput(dataOutput);
    }

    for (String dataInputRef : specificationModel.getDataInputRefs()) {
      DataRef dataRef = new DataRef(dataInputRef);
      ioSpecification.addInputRef(dataRef);
    }

    for (String dataOutputRef : specificationModel.getDataOutputRefs()) {
      DataRef dataRef = new DataRef(dataOutputRef);
      ioSpecification.addOutputRef(dataRef);
    }

    return ioSpecification;
  }
  
  protected void processArtifacts(BpmnParse bpmnParse, Collection<Artifact> artifacts, ScopeImpl scope) {
    // associations  
    for (Artifact artifact : artifacts) {
      if (artifact instanceof Association) {
        createAssociation(bpmnParse, (Association) artifact, scope);
      }
    }
  }
  
  protected void createAssociation(BpmnParse bpmnParse, Association association, ScopeImpl parentScope) {
    BpmnModel bpmnModel = bpmnParse.getBpmnModel();
    if (bpmnModel.getArtifact(association.getSourceRef()) != null ||
        bpmnModel.getArtifact(association.getTargetRef()) != null) {
      
      // connected to a text annotation so skipping it
      return;
    }
    
    ActivityImpl sourceActivity = parentScope.findActivity(association.getSourceRef());
    ActivityImpl targetActivity = parentScope.findActivity(association.getTargetRef());
    
    // an association may reference elements that are not parsed as activities (like for instance 
    // text annotations so do not throw an exception if sourceActivity or targetActivity are null)
    // However, we make sure they reference 'something':
    if (sourceActivity == null) {
      //bpmnModel.addProblem("Invalid reference sourceRef '" + association.getSourceRef() + "' of association element ", association.getId());
    } else if (targetActivity == null) {
      //bpmnModel.addProblem("Invalid reference targetRef '" + association.getTargetRef() + "' of association element ", association.getId());
    } else {      
      if (sourceActivity.getProperty("type").equals("compensationBoundaryCatch")) {
        Object isForCompensation = targetActivity.getProperty(PROPERTYNAME_IS_FOR_COMPENSATION);          
        if (isForCompensation == null || !(Boolean) isForCompensation) {
          logger.warn("compensation boundary catch must be connected to element with isForCompensation=true");
        } else {            
          ActivityImpl compensatedActivity = sourceActivity.getParentActivity();
          compensatedActivity.setProperty(BpmnParse.PROPERTYNAME_COMPENSATION_HANDLER_ID, targetActivity.getId());            
        }
      }
    }
  }
  
  protected Map<String, Object> processDataObjects(BpmnParse bpmnParse, Collection<ValuedDataObject> dataObjects, ScopeImpl scope) {
    Map<String, Object> variablesMap = new HashMap<String, Object>();
    // convert data objects to process variables  
    if (dataObjects != null) {
      for (ValuedDataObject dataObject : dataObjects) {
        variablesMap.put(dataObject.getName(), dataObject.getValue());
      }
    }
    return variablesMap;
  }
}
