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

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionAttribute;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.alfresco.AlfrescoStartEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tijs Rademakers
 * 重写源码: 使得开始事件自动拥有扩展属性
 *
 * 疑问:
 *  protected static  final List<ExtensionAttribute>   defaultElementAttributes=
 *           Arrays.asList(new ExtensionAttribute(ATTRIBUTE_ID),new ExtensionAttribute(ATTRIBUTE_NAME));
 *      BpmnXMLUtil.addCustomAttributes(xtr,startEvent,defaultElementAttributes);
 *      实际上就加了2行代码,
 *      为何不直接调用父类的() 呢??
 *      eg:
 *      BaseElemnt  element =super.convertXMLToElemnt(xtr,model);// 委托父类的
 *      BpmnXMLUtil.addCustomAttributes(xtr,element,defaultElementAttributes);
 *
 *    上面的代码处理逻辑看起来很合理, 也很清晰,但是这样的操作完全错误,
 *    convertXMLToElement() 主要用于解析开始节点 ,
 *
 *    由于 STAX的解析原理, 使用流模型,解析文档时读取到的数据流只能前进不能回退,
 *    如果第2行委托父类进行元素的解析工作则势必会涉及子元素的解析,
 *    如果执行完子元素的解析工作之后, 再回来解析父类的属性,
 *    这样的操作是完全错误,因为流模型解析文档的时候数据流只能前进而不能后退
 *
 *    那么如何替换引擎元素解析器呢????
 *    BpmnXMLConverter  bpmnxmlConverter = new  BpmnXMLConverter();
 *    BpmnXMLConverter.addConverter(new MyStartEventConverter());//临时添加
 *
 *    也有全局的方式 直接extedns   BpmnXMLConverter
 *
 *
 */
public class StartEventXMLConverter extends BaseBpmnXMLConverter {
  /*
  * 初始化黑名单集合 defaultElementAttributes
  * 该集合定义了 id name 两个属性,
  * 这样 引擎解析开始节点时, 除了以上定义的两个属性之外, 其他属性都会作为扩展属性处理
  * */
  protected static  final List<ExtensionAttribute>   defaultElementAttributes=
          Arrays.asList(new ExtensionAttribute(ATTRIBUTE_ID),new ExtensionAttribute(ATTRIBUTE_NAME));
  /*
  * 告诉引擎开始节点的属性信息 用StartEvent进行封装
  * */
  public Class<? extends BaseElement> getBpmnElementType() {
    return StartEvent.class;
  }
  /*
  * 告诉引擎 程序要解析 startEvent元素
  * */
  @Override
  protected String getXMLElementName() {
    return ELEMENT_EVENT_START;
  }
  /*
  * 此() 用来解析开始节点
  * */
  @Override
  protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
    //得到formKey值
    String formKey = xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_FORMKEY);
    StartEvent startEvent = null;
    /*
    * 根据formKey的有无, 实例化不同的类,
    * */
    if (StringUtils.isNotEmpty(formKey)) {
      if (model.getStartEventFormTypes() != null && model.getStartEventFormTypes().contains(formKey)) {
        startEvent = new AlfrescoStartEvent();
      }
    }
    if (startEvent == null) {
      startEvent = new StartEvent();
    }
    //设置开始节点在流程文档中的坐标信息
    BpmnXMLUtil.addXMLLocation(startEvent, xtr);
    //设置 流程发起人
    startEvent.setInitiator(xtr.getAttributeValue(ACTIVITI_EXTENSIONS_NAMESPACE, ATTRIBUTE_EVENT_START_INITIATOR));
    //设置formKey值
    startEvent.setFormKey(formKey);
    /*
    * 自己加的一行代码,  进行自定义属性的解析工作
    * */
    BpmnXMLUtil.addCustomAttributes(xtr,startEvent,defaultElementAttributes);
    /*
    * 调用 parseChildElements 进行子元素的解析工作
    * */
    parseChildElements(getXMLElementName(), startEvent, model, xtr);
    
    return startEvent;
  }
  
  @Override
  protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    StartEvent startEvent = (StartEvent) element;
    writeQualifiedAttribute(ATTRIBUTE_EVENT_START_INITIATOR, startEvent.getInitiator(), xtw);
    writeQualifiedAttribute(ATTRIBUTE_FORM_FORMKEY, startEvent.getFormKey(), xtw);
  }
  
  @Override
  protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
    StartEvent startEvent = (StartEvent) element;
    didWriteExtensionStartElement = writeFormProperties(startEvent, didWriteExtensionStartElement, xtw);
    return didWriteExtensionStartElement;
  }
  
  @Override
  protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    StartEvent startEvent = (StartEvent) element;
    writeEventDefinitions(startEvent, startEvent.getEventDefinitions(), model, xtw);
  }
}
