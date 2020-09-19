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
import org.activiti.bpmn.model.SequenceFlow;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 * 连线的  元素解析器
 */
public class SequenceFlowXMLConverter extends BaseBpmnXMLConverter {
  
  public Class<? extends BaseElement> getBpmnElementType() {
    return SequenceFlow.class;
  }
  
  @Override
  protected String getXMLElementName() {
    return ELEMENT_SEQUENCE_FLOW;
  }
  /*

   */
  @Override
  protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
    /*
    实例化 sequenceFlow 元素对应的属性承载类
    该类 承载了连线元素所有属性值的 获取和存储工作
    连线对应解析完毕,
    所有的属性值都会封装到SequenceFlow 对象中
     */
    SequenceFlow sequenceFlow = new SequenceFlow();
    /*
    //获取元素再XML中的坐标信息
    获取元素在流程文档中的行号 和列号 是非常必要的
    这样做的好处就是 当元素解析 失败时  ,会将该元素在 流程文档中的行号 和 列号 暴露给客户端
    方便客户端 定位和排查问题
     */
    BpmnXMLUtil.addXMLLocation(sequenceFlow, xtr);
    /*
    解析属性信息
    SequenceFlow常用的属性 有  sourceRef targetRef   name skipExpression
    将这些属性信息 获取并且 填充到 sequenceFlow对象中
     */
    sequenceFlow.setSourceRef(xtr.getAttributeValue(null, ATTRIBUTE_FLOW_SOURCE_REF));
    sequenceFlow.setTargetRef(xtr.getAttributeValue(null, ATTRIBUTE_FLOW_TARGET_REF));
    sequenceFlow.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
    sequenceFlow.setSkipExpression(xtr.getAttributeValue(null, ATTRIBUTE_FLOW_SKIP_EXPRESSION));
    /*
    开始解析 当前元素的子元素
    但是 此类中并没有该() 的实现,既然该类中没有提供实现,那么其父类肯定提供了默认实现,
    所以找到当前类的父类 BaseBpmnXMLConverter

    调用parseChildElements() 解析 sequenceFlow元素中的子元素,
    对于 sequenceFlow 元素来说 常用的子元素有文档元素 documentation
    扩展 元素 extensionElements ( 包括 执行监听器 以及 用户自定义元素 )


     */
    parseChildElements(getXMLElementName(), sequenceFlow, model, xtr);
    
    return sequenceFlow;
  }

  @Override
  protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    SequenceFlow sequenceFlow = (SequenceFlow) element;
    writeDefaultAttribute(ATTRIBUTE_FLOW_SOURCE_REF, sequenceFlow.getSourceRef(), xtw);
    writeDefaultAttribute(ATTRIBUTE_FLOW_TARGET_REF, sequenceFlow.getTargetRef(), xtw);
    if (StringUtils.isNotEmpty(sequenceFlow.getSkipExpression())) {
      writeDefaultAttribute(ATTRIBUTE_FLOW_SKIP_EXPRESSION, sequenceFlow.getSkipExpression(), xtw);
    }
  }
  
  @Override
  protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    SequenceFlow sequenceFlow = (SequenceFlow) element;
    
    if (StringUtils.isNotEmpty(sequenceFlow.getConditionExpression())) {
      xtw.writeStartElement(ELEMENT_FLOW_CONDITION);
      xtw.writeAttribute(XSI_PREFIX, XSI_NAMESPACE, "type", "tFormalExpression");
      xtw.writeCData(sequenceFlow.getConditionExpression());
      xtw.writeEndElement();
    }
  }
}
