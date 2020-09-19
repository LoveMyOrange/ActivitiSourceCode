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
package org.activiti.bpmn.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.child.BaseChildElementParser;
import org.activiti.bpmn.converter.export.ActivitiListenerExport;
import org.activiti.bpmn.converter.export.FailedJobRetryCountExport;
import org.activiti.bpmn.converter.export.MultiInstanceExport;
import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.Artifact;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.CancelEventDefinition;
import org.activiti.bpmn.model.CompensateEventDefinition;
import org.activiti.bpmn.model.DataObject;
import org.activiti.bpmn.model.ErrorEventDefinition;
import org.activiti.bpmn.model.Event;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.ExtensionAttribute;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.FormValue;
import org.activiti.bpmn.model.Gateway;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.SignalEventDefinition;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.TerminateEventDefinition;
import org.activiti.bpmn.model.ThrowEvent;
import org.activiti.bpmn.model.TimerEventDefinition;
import org.activiti.bpmn.model.UserTask;
import org.activiti.bpmn.model.ValuedDataObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @DESC   所有元素解析器的父类
 */
public abstract class BaseBpmnXMLConverter implements BpmnXMLConstants {

  protected static final Logger LOGGER = LoggerFactory.getLogger(BaseBpmnXMLConverter.class);
  
  protected static final List<ExtensionAttribute> defaultElementAttributes = Arrays.asList(
      new ExtensionAttribute(ATTRIBUTE_ID),
      new ExtensionAttribute(ATTRIBUTE_NAME)
  );
  
  protected static final List<ExtensionAttribute> defaultActivityAttributes = Arrays.asList(
      new ExtensionAttribute(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_ASYNCHRONOUS), 
      new ExtensionAttribute(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_EXCLUSIVE), 
      new ExtensionAttribute(ATTRIBUTE_DEFAULT), 
      new ExtensionAttribute(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION)
  );
  /*
  此类中的 convertXMLToElement(xtr,model) 是抽象()  该() 需要交给具体的子类去实现
  为何这样设计???
   因为流程文档中每个元素  都对应一个解析器, 用来实现自身的属性解析功能
   不同的解析器内部实现不同, 但是 都在父类的 convertXMLToElement() 统一调用
   该() 作为通用模板()存在

    由于BpmnXMLConstants接口 定义了流程文档中所有的元素以及属性字段
  为了方便统一管理, 所有的元素解析器都直接或者间接的实现该接口

   */
  public void convertToBpmnModel(XMLStreamReader xtr, BpmnModel model, Process activeProcess, 
      List<SubProcess> activeSubProcessList) throws Exception {
    
    String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID); //获取id值
    String elementName = xtr.getAttributeValue(null, ATTRIBUTE_NAME);//name值
    boolean async = parseAsync(xtr);//activiti:async 的值
    boolean notExclusive = parseNotExclusive(xtr);//activiti:exclusive的值
    String defaultFlow = xtr.getAttributeValue(null, ATTRIBUTE_DEFAULT); //获取default中的值
    boolean isForCompensation = parseForCompensation(xtr);//isForCompensation中的值
    /*
    上面都是 元素的公共属性
    调用具体的解析器中的convertXMLToElement() 进行解析
    因为 convertXMLToElement() 是当前类的一个抽象()
    所以最终会调用 具体的元素解析器 完成元素解析工作,
    该() 解析完毕之后会返回 BaseElement 对象
    换言之 :   元素以及属性解析完毕之后 会将其解析结果封装为BaseElement对象
     */
    BaseElement parsedElement = convertXMLToElement(xtr, model);
    /*

    根据元素对应的属性承载类 的类型进行赋值, 该操作主要区分 网关和活动 (引用流程, 子流程 以及所有的Task节点)
    两大要素
    所有的节点都需要添加id ,name 值
    如果元素是 网关类型 则需要填充 defaultFlow ,async ,notExclusive 属性值
    活动类型 则需要 填充 async, notExclusive formCompensation ,defaultFlow 属性值
     */
    if (parsedElement instanceof Artifact) {
      Artifact currentArtifact = (Artifact) parsedElement;
      currentArtifact.setId(elementId);

      if (!activeSubProcessList.isEmpty()) {
        activeSubProcessList.get(activeSubProcessList.size() - 1).addArtifact(currentArtifact);

      } else {
        activeProcess.addArtifact(currentArtifact);
      }
    }
    
    if (parsedElement instanceof FlowElement) { //流程3大要素中的元素基本都是该类型
      
      FlowElement currentFlowElement = (FlowElement) parsedElement;
      currentFlowElement.setId(elementId); //设置id
      currentFlowElement.setName(elementName);//name
      
      if (currentFlowElement instanceof FlowNode) {
        FlowNode flowNode = (FlowNode) currentFlowElement;
        flowNode.setAsynchronous(async);//是否异步
        flowNode.setNotExclusive(notExclusive);//是否排他
        
        if (currentFlowElement instanceof Activity) {//是否是活动节点
          
          Activity activity = (Activity) currentFlowElement;
          activity.setForCompensation(isForCompensation);
          if (StringUtils.isNotEmpty(defaultFlow)) { //默认连线
            activity.setDefaultFlow(defaultFlow);
          }
        }
        
        if (currentFlowElement instanceof Gateway) {//是否是网关
          Gateway gateway = (Gateway) currentFlowElement;
          if (StringUtils.isNotEmpty(defaultFlow)) {
            gateway.setDefaultFlow(defaultFlow);
          }
        }
      }
      /*
      判断类型是否为DtaObject, 则需要判断 activeSubProcessList 参数值是否为空
      如果不为空 ,则将其作为 子流程中的元素进行添加
      否则添加到 activeProcess 对象中
       */
      if (currentFlowElement instanceof DataObject) { //dataObject
        if (!activeSubProcessList.isEmpty()) {
          activeSubProcessList.get(activeSubProcessList.size() - 1).getDataObjects().add((ValuedDataObject)parsedElement);
        } else {
          activeProcess.getDataObjects().add((ValuedDataObject)parsedElement);
        }
      }
    /*
    判断当前正在解析元素的父级元素,
    如果activeSubProcessList 不为空
    则将其 添加到 activeSubProcessList对象中
    否则添加到 activeProcess 中
     */
      if (!activeSubProcessList.isEmpty()) { //解析元素属于子流程
        activeSubProcessList.get(activeSubProcessList.size() - 1).addFlowElement(currentFlowElement);
      } else {
        activeProcess.addFlowElement(currentFlowElement);
      }
    }
  }
  
  public void convertToXML(XMLStreamWriter xtw, BaseElement baseElement, BpmnModel model) throws Exception {
    xtw.writeStartElement(getXMLElementName());
    boolean didWriteExtensionStartElement = false;
    writeDefaultAttribute(ATTRIBUTE_ID, baseElement.getId(), xtw);
    if (baseElement instanceof FlowElement) {
      writeDefaultAttribute(ATTRIBUTE_NAME, ((FlowElement) baseElement).getName(), xtw);
    }
    
    if (baseElement instanceof FlowNode) {
      final FlowNode flowNode = (FlowNode) baseElement;
      if (flowNode.isAsynchronous()) {
        writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, ATTRIBUTE_VALUE_TRUE, xtw);
        if (flowNode.isNotExclusive()) {
          writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_EXCLUSIVE, ATTRIBUTE_VALUE_FALSE, xtw);
        }
      }
      
      if (baseElement instanceof Activity) {
        final Activity activity = (Activity) baseElement;
        if (activity.isForCompensation()) {
          writeDefaultAttribute(ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION, ATTRIBUTE_VALUE_TRUE, xtw);
        }
        if (StringUtils.isNotEmpty(activity.getDefaultFlow())) {
          FlowElement defaultFlowElement = model.getFlowElement(activity.getDefaultFlow());
          if (defaultFlowElement instanceof SequenceFlow) {
            writeDefaultAttribute(ATTRIBUTE_DEFAULT, activity.getDefaultFlow(), xtw);
          }
        }
      }
      
      if (baseElement instanceof Gateway) {
        final Gateway gateway = (Gateway) baseElement;
        if (StringUtils.isNotEmpty(gateway.getDefaultFlow())) {
          FlowElement defaultFlowElement = model.getFlowElement(gateway.getDefaultFlow());
          if (defaultFlowElement instanceof SequenceFlow) {
            writeDefaultAttribute(ATTRIBUTE_DEFAULT, gateway.getDefaultFlow(), xtw);
          }
        }
      }
    }
    
    writeAdditionalAttributes(baseElement, model, xtw);
    
    if (baseElement instanceof FlowElement) {
      final FlowElement flowElement = (FlowElement) baseElement;
      if (StringUtils.isNotEmpty(flowElement.getDocumentation())) {
  
        xtw.writeStartElement(ELEMENT_DOCUMENTATION);
        xtw.writeCharacters(flowElement.getDocumentation());
        xtw.writeEndElement();
      }
    }
    
    didWriteExtensionStartElement = writeExtensionChildElements(baseElement, didWriteExtensionStartElement, xtw);
    didWriteExtensionStartElement = writeListeners(baseElement, didWriteExtensionStartElement, xtw);
    didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(baseElement, didWriteExtensionStartElement, model.getNamespaces(), xtw);
    if (baseElement instanceof Activity) {
    	final Activity activity = (Activity) baseElement;
        FailedJobRetryCountExport.writeFailedJobRetryCount(activity, xtw);
        
     }
    
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    
    if (baseElement instanceof Activity) {
      final Activity activity = (Activity) baseElement;
      MultiInstanceExport.writeMultiInstance(activity, xtw);
      
    }
    
    writeAdditionalChildElements(baseElement, model, xtw);
    
    xtw.writeEndElement();
  }
  
  protected abstract Class<? extends BaseElement> getBpmnElementType();
  
  protected abstract BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception;
  
  protected abstract String getXMLElementName();
  
  protected abstract void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception;
  
  protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
    return didWriteExtensionStartElement;
  }
  
  protected abstract void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception;
  
  // To BpmnModel converter convenience methods
  /*
   设置 additionalParsers 参数值为null
   该参数值 为客户端自定义的子元素解析器集合

   可以看出 程序默认 不加载用户自定义的子元素解析器,
   如果客户端需要添加 自定义子元素解析器
   设置  additionalParsers 属性值即可

   */
  protected void parseChildElements(String elementName, BaseElement parentElement, BpmnModel model, XMLStreamReader xtr) throws Exception {
    parseChildElements(elementName, parentElement, null, model, xtr);
  }
  /*

   */
  protected void parseChildElements(String elementName, BaseElement parentElement, Map<String, BaseChildElementParser> additionalParsers, 
      BpmnModel model, XMLStreamReader xtr) throws Exception {
    
    Map<String, BaseChildElementParser> childParsers = new HashMap<String, BaseChildElementParser>();
    if (additionalParsers != null) { //判断是否为空??
      //如果不为空  将用户自定义的子元素 解析器集合 添加到  childParsers 中
      childParsers.putAll(additionalParsers);
    }
    //委托 BpmnXMLUtil.parseChildElements解析子元素
    BpmnXMLUtil.parseChildElements(elementName, parentElement, xtr, childParsers, model);
  }
  
  @SuppressWarnings("unchecked")
  protected ExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
    ExtensionElement extensionElement = new ExtensionElement();
    extensionElement.setName(xtr.getLocalName());
    if (StringUtils.isNotEmpty(xtr.getNamespaceURI())) {
      extensionElement.setNamespace(xtr.getNamespaceURI());
    }
    if (StringUtils.isNotEmpty(xtr.getPrefix())) {
      extensionElement.setNamespacePrefix(xtr.getPrefix());
    }

    BpmnXMLUtil.addCustomAttributes(xtr, extensionElement, defaultElementAttributes);

    boolean readyWithExtensionElement = false;
    while (readyWithExtensionElement == false && xtr.hasNext()) {
      xtr.next();
      if (xtr.isCharacters() || XMLStreamReader.CDATA == xtr.getEventType()) {
        if (StringUtils.isNotEmpty(xtr.getText().trim())) {
          extensionElement.setElementText(xtr.getText().trim());
        }
      } else if (xtr.isStartElement()) {
        ExtensionElement childExtensionElement = parseExtensionElement(xtr);
        extensionElement.addChildElement(childExtensionElement);
      } else if (xtr.isEndElement() && extensionElement.getName().equalsIgnoreCase(xtr.getLocalName())) {
        readyWithExtensionElement = true;
      }
    }
    return extensionElement;
  }

  protected boolean parseAsync(XMLStreamReader xtr) {
    boolean async = false;
    String asyncString = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_ASYNCHRONOUS);
    if (ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(asyncString)) {
      async = true;
    }
    return async;
  }
  
  protected boolean parseNotExclusive(XMLStreamReader xtr) {
    boolean notExclusive = false;
    String exclusiveString = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_ACTIVITY_EXCLUSIVE);
    if (ATTRIBUTE_VALUE_FALSE.equalsIgnoreCase(exclusiveString)) {
      notExclusive = true;
    }
    return notExclusive;
  }
  
  protected boolean parseForCompensation(XMLStreamReader xtr) {
    boolean isForCompensation = false;
    String compensationString = xtr.getAttributeValue(null, ATTRIBUTE_ACTIVITY_ISFORCOMPENSATION);
    if (ATTRIBUTE_VALUE_TRUE.equalsIgnoreCase(compensationString)) {
      isForCompensation = true;
    }
    return isForCompensation;
  }
  
  protected List<String> parseDelimitedList(String expression) {
    return BpmnXMLUtil.parseDelimitedList(expression);
  }
  
  // To XML converter convenience methods
  
  protected String convertToDelimitedString(List<String> stringList) {
    return BpmnXMLUtil.convertToDelimitedString(stringList);
  }
  
  protected boolean writeFormProperties(FlowElement flowElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
    
    List<FormProperty> propertyList = null;
    if (flowElement instanceof UserTask) {
      propertyList = ((UserTask) flowElement).getFormProperties();
    } else if (flowElement instanceof StartEvent) {
      propertyList = ((StartEvent) flowElement).getFormProperties();
    }
    
    if (propertyList != null) {
    
      for (FormProperty property : propertyList) {
        
        if (StringUtils.isNotEmpty(property.getId())) {
          
          if (didWriteExtensionStartElement == false) { 
            xtw.writeStartElement(ELEMENT_EXTENSIONS);
            didWriteExtensionStartElement = true;
          }
          
          xtw.writeStartElement(ACTIVITI_EXTENSIONS_PREFIX, ELEMENT_FORMPROPERTY, ACTIVITI_EXTENSIONS_NAMESPACE);
          writeDefaultAttribute(ATTRIBUTE_FORM_ID, property.getId(), xtw);
          
          writeDefaultAttribute(ATTRIBUTE_FORM_NAME, property.getName(), xtw);
          writeDefaultAttribute(ATTRIBUTE_FORM_TYPE, property.getType(), xtw);
          writeDefaultAttribute(ATTRIBUTE_FORM_EXPRESSION, property.getExpression(), xtw);
          writeDefaultAttribute(ATTRIBUTE_FORM_VARIABLE, property.getVariable(), xtw);
          writeDefaultAttribute(ATTRIBUTE_FORM_DEFAULT, property.getDefaultExpression(), xtw);
          writeDefaultAttribute(ATTRIBUTE_FORM_DATEPATTERN, property.getDatePattern(), xtw);
          if (property.isReadable() == false) {
            writeDefaultAttribute(ATTRIBUTE_FORM_READABLE, ATTRIBUTE_VALUE_FALSE, xtw);
          }
          if (property.isWriteable() == false) {
            writeDefaultAttribute(ATTRIBUTE_FORM_WRITABLE, ATTRIBUTE_VALUE_FALSE, xtw);
          }
          if (property.isRequired()) {
            writeDefaultAttribute(ATTRIBUTE_FORM_REQUIRED, ATTRIBUTE_VALUE_TRUE, xtw);
          }
          
          for (FormValue formValue : property.getFormValues()) {
            if (StringUtils.isNotEmpty(formValue.getId())) {
              xtw.writeStartElement(ACTIVITI_EXTENSIONS_PREFIX, ELEMENT_VALUE, ACTIVITI_EXTENSIONS_NAMESPACE);
              xtw.writeAttribute(ATTRIBUTE_ID, formValue.getId());
              xtw.writeAttribute(ATTRIBUTE_NAME, formValue.getName());
              xtw.writeEndElement();
            }
          }
          
          xtw.writeEndElement();
        }
      }
    }
    
    return didWriteExtensionStartElement;
  }
  
  protected boolean writeListeners(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
    return ActivitiListenerExport.writeListeners(element, didWriteExtensionStartElement, xtw);
  }
  
  protected void writeEventDefinitions(Event parentEvent, List<EventDefinition> eventDefinitions, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    for (EventDefinition eventDefinition : eventDefinitions) {
      if (eventDefinition instanceof TimerEventDefinition) {
        writeTimerDefinition(parentEvent, (TimerEventDefinition) eventDefinition, xtw);
      } else if (eventDefinition instanceof SignalEventDefinition) {
        writeSignalDefinition(parentEvent, (SignalEventDefinition) eventDefinition, xtw);
      } else if (eventDefinition instanceof MessageEventDefinition) {
        writeMessageDefinition(parentEvent, (MessageEventDefinition) eventDefinition, model, xtw);
      } else if (eventDefinition instanceof ErrorEventDefinition) {
        writeErrorDefinition(parentEvent, (ErrorEventDefinition) eventDefinition, xtw);
      } else if (eventDefinition instanceof TerminateEventDefinition) {
        writeTerminateDefinition(parentEvent, (TerminateEventDefinition) eventDefinition, xtw);
      } else if (eventDefinition instanceof CancelEventDefinition) {
        writeCancelDefinition(parentEvent, (CancelEventDefinition) eventDefinition, xtw);
      } else if (eventDefinition instanceof CompensateEventDefinition) {
        writeCompensateDefinition(parentEvent, (CompensateEventDefinition) eventDefinition, xtw);
      }
    }
  }
  
  protected void writeTimerDefinition(Event parentEvent, TimerEventDefinition timerDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_TIMERDEFINITION);
    if (StringUtils.isNotEmpty(timerDefinition.getCalendarName())) {
      writeQualifiedAttribute(ATTRIBUTE_CALENDAR_NAME, timerDefinition.getCalendarName(), xtw);
    }
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(timerDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    if (StringUtils.isNotEmpty(timerDefinition.getTimeDate())) {
      xtw.writeStartElement(ATTRIBUTE_TIMER_DATE);
      xtw.writeCharacters(timerDefinition.getTimeDate());
      xtw.writeEndElement();
      
    } else if (StringUtils.isNotEmpty(timerDefinition.getTimeCycle())) {
      xtw.writeStartElement(ATTRIBUTE_TIMER_CYCLE);

      if (StringUtils.isNotEmpty(timerDefinition.getEndDate())) {
        xtw.writeAttribute(ACTIVITI_EXTENSIONS_PREFIX, ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_END_DATE,timerDefinition.getEndDate());
      }

      xtw.writeCharacters(timerDefinition.getTimeCycle());
      xtw.writeEndElement();
      
    } else if (StringUtils.isNotEmpty(timerDefinition.getTimeDuration())) {
      xtw.writeStartElement(ATTRIBUTE_TIMER_DURATION);
      xtw.writeCharacters(timerDefinition.getTimeDuration());
      xtw.writeEndElement();
    }
    
    xtw.writeEndElement();
  }
  
  protected void writeSignalDefinition(Event parentEvent, SignalEventDefinition signalDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_SIGNALDEFINITION);
    writeDefaultAttribute(ATTRIBUTE_SIGNAL_REF, signalDefinition.getSignalRef(), xtw);
    if (parentEvent instanceof ThrowEvent && signalDefinition.isAsync()) {
      BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, "true", xtw);
    }
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(signalDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeCancelDefinition(Event parentEvent, CancelEventDefinition cancelEventDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_CANCELDEFINITION);
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(cancelEventDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeCompensateDefinition(Event parentEvent, CompensateEventDefinition compensateEventDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_COMPENSATEDEFINITION);
    writeDefaultAttribute(ATTRIBUTE_COMPENSATE_ACTIVITYREF, compensateEventDefinition.getActivityRef(), xtw);
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(compensateEventDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeMessageDefinition(Event parentEvent, MessageEventDefinition messageDefinition, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_MESSAGEDEFINITION);
    
    String messageRef = messageDefinition.getMessageRef();
    if (StringUtils.isNotEmpty(messageRef)) {
      // remove the namespace from the message id if set
      if (messageRef.startsWith(model.getTargetNamespace())) {
        messageRef = messageRef.replace(model.getTargetNamespace(), "");
        messageRef = messageRef.replaceFirst(":", "");
      } else {
        for (String prefix : model.getNamespaces().keySet()) {
          String namespace = model.getNamespace(prefix);
          if (messageRef.startsWith(namespace)) {
            messageRef = messageRef.replace(model.getTargetNamespace(), "");
            messageRef = prefix + messageRef;
          }
        }
      }
    }
    writeDefaultAttribute(ATTRIBUTE_MESSAGE_REF, messageRef, xtw);
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(messageDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeErrorDefinition(Event parentEvent, ErrorEventDefinition errorDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_ERRORDEFINITION);
    writeDefaultAttribute(ATTRIBUTE_ERROR_REF, errorDefinition.getErrorCode(), xtw);
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(errorDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeTerminateDefinition(Event parentEvent, TerminateEventDefinition terminateDefinition, XMLStreamWriter xtw) throws Exception {
    xtw.writeStartElement(ELEMENT_EVENT_TERMINATEDEFINITION);
    
    if (terminateDefinition.isTerminateAll()) {
    	writeQualifiedAttribute(ATTRIBUTE_TERMINATE_ALL, "true", xtw);
    }
    
    boolean didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(terminateDefinition, false, xtw);
    if (didWriteExtensionStartElement) {
      xtw.writeEndElement();
    }
    xtw.writeEndElement();
  }
  
  protected void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
    BpmnXMLUtil.writeDefaultAttribute(attributeName, value, xtw);
  }
  
  protected void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
    BpmnXMLUtil.writeQualifiedAttribute(attributeName, value, xtw);
  }
}
