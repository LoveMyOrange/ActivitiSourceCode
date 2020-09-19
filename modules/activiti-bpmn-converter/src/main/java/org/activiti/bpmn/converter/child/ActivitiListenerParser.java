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
package org.activiti.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ImplementationType;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 * 此类主要是 prcoess元素中 执行监听器的处理逻辑
 */
public abstract class ActivitiListenerParser extends BaseChildElementParser {
  
  public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
    //此对象承载了执行监听器和 任务监听器的存取工作, 这两种类型的监听器仅仅是事件以及类型不同而已, 其他的属性 大体相同
    ActivitiListener listener = new ActivitiListener();//实例化 监听器 承载类
    BpmnXMLUtil.addXMLLocation(listener, xtr); //坐标信息
    //根据监听器的创建方式执行不同的处理逻辑,
    if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS))) { //class方式创建
      listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS));  //获取属性
      listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);//填充属性
    } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EXPRESSION))) {
      listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EXPRESSION));
      listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
    } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION))) {
      listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION));
      listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
    }
    listener.setEvent(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EVENT)); //获取event事件并且填充对象属性
    //因为 监听器作为子元素存在, 所以需要将ActivitiListener 对象通过 addListenerToParent() 添加到监听器的父级元素中
    //因为 addListenerToParent 是当前类的一个抽象()
    addListenerToParent(listener, parentElement);//将其添加到父元素
    /*
    *  监听器也存在子元素吗?? 答案是肯定的 监听器中可以配置变量,变量可以是具体的值 也可以是表达式
    *  parseChildElements() 主要负责解析 监听器中的 field元素
    * field元素的配置形如:  <activiti:filed name="java"  expression="${expression}"/>
    * 通过该() 中的输入参数类型可知,filed元素的解析工作交给 FieldExtensionParser 完成
    *
    * */
    parseChildElements(xtr, listener, model, new FieldExtensionParser()); // 解析 activiti:field
  }
  
  public abstract void addListenerToParent(ActivitiListener listener, BaseElement parentElement);
}












