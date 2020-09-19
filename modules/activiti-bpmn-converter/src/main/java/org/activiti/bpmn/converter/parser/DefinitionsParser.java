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
package org.activiti.bpmn.converter.parser;

import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionAttribute;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 * @desc
 * 根元素  definitions 对应的解析器为 DefinitionsParser
 * 根据元素中可以定义多个流程元素   process (建议每个流程文档只定义一个process元素 这样可以减少开发过程中的维护成本)
 *
 *  <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
 *  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
 *  xmlns:activiti="http://activiti.org/bpmn"
 *  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
 *  xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
 *  xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
 *  typeLanguage="http://www.w3.org/2001/XMLSchema"
 *  expressionLanguage="http://www.w3.org/1999/XPath"
 *  targetNamespace="http://www.activiti.org/processdef"
 *  id="javaee"
 *  name="javaeee"
 *  exporter="javaee"
 *  exporterVersion="1"
 *  >
 *
 *
 * definitions 元素 至少需要包含xmlns 和targetNameSpace  两个属性声明
 * targetNameSpace 可以是任意值,, 该属性可以用来对流程定义模板进行分类
 *
 *
 * 疑问:
 *         目前 Actiivit中 哪些元素 默认支持扩展属性呢??
 *         解答  3个 根元素 definitions  任务节点 userTask   流程元素 process
 */
public class DefinitionsParser implements BpmnXMLConstants {
  /*
  黑名单列表
  有3个
  typeLanguage, expressionLanguage,targetNameSpace

  处理流程如下:
    1)遍历definitions元素中的所有属性 ,对于definitions元素来说 ,除了定义黑名单中的属性
    还分别定义了exporterVersion, id ,name exporter 4个属性
    因此上述7个属性都会进行遍历

    2)封装属性信息
    ExtensionAttribute 承载了定义的属性名称 name  属性值value  命名空间 namespace  命名空间前缀namespaceprefix
    3)definitions元素的扩展属性判断


   */
  protected static final List<ExtensionAttribute> defaultAttributes = Arrays.asList(
      new ExtensionAttribute(TYPE_LANGUAGE_ATTRIBUTE), 
      new ExtensionAttribute(EXPRESSION_LANGUAGE_ATTRIBUTE), 
      new ExtensionAttribute(TARGET_NAMESPACE_ATTRIBUTE)
  );
  
  @SuppressWarnings("unchecked")
  public void parse(XMLStreamReader xtr, BpmnModel model) throws Exception {
    //获取targetNameSpace 命名空间, 对应值为 http://www.activiti.org/processdef
    model.setTargetNamespace(xtr.getAttributeValue(null, TARGET_NAMESPACE_ATTRIBUTE));
    //遍历所有命名空间的URI ,包括获取URI的前缀以及值 然后添加到model对象中
    for (int i = 0; i < xtr.getNamespaceCount(); i++) { //获取命名空间形如 xmlns:activiti
      String prefix = xtr.getNamespacePrefix(i);//形如activiti
      if (StringUtils.isNotEmpty(prefix)) {
        //xtr.getNameSpaceURI(i) 值对应 http://activiti.org/bpmn
        model.addNamespace(prefix, xtr.getNamespaceURI(i)); //获取到 根元素的targetNameSpace 并设置到model对象中
      }
    }
    //进行属性的解析
    for (int i = 0; i < xtr.getAttributeCount(); i++) { //获取属性值  比如 id name等
      ExtensionAttribute extensionAttribute = new ExtensionAttribute(); //扩展属性承载类
      extensionAttribute.setName(xtr.getAttributeLocalName(i));
      extensionAttribute.setValue(xtr.getAttributeValue(i));
      if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
        extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));  ///存在命名空间则添加
      }
      if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) { //不为空添加
        extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
      }
      /*
      验证黑名单列表 如果解析的属性不在黑名单列表中 ,则将其作为扩展属性进行处理  然后添加到model对象中

      经过此() 处理之后 , 如果存在扩展属性 ,则通过
       model.addDefinitionsAttribute(extensionAttribute); 将其 添加到model 对象中
       方便后续获取
       addDefinitionsAttribute() 内部使用Map 存储

       疑问:
        目前 Actiivit中 哪些元素 默认支持扩展属性呢??
          3个 根元素 definitions  任务节点 userTask   流程元素 process
       */
      if (!BpmnXMLUtil.isBlacklisted(extensionAttribute, defaultAttributes)) {
        model.addDefinitionsAttribute(extensionAttribute);
      }
    }
  }
}














