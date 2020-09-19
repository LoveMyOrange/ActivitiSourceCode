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

import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.child.ActivitiEventListenerParser;
import org.activiti.bpmn.converter.child.ExecutionListenerParser;
import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SubProcess;

/**
 * @author Tijs Rademakers
 * @desc
 *  此类解析 监听器的元素
 *
 *   流程文档的三大要素都可以定义 执行监听器 和 任务监听器(仅限于在任务节点中进行定义 )
 *   监听器通常作为扩展元素  extensionElements的子元素进行定义
 *   因为监听器可以很方便的让客户使用和扩展,
 *   执行监听器 以及 任务监听器  的解析器 都 继承了 BaseChildElementParser类 ,并且在父类中统一调度
 *   既然 监听器通常作为 extensionElements的子元素存在, 所以首先找到 extensionElements元素解析器
 *   也就是本类
 *
 *   看到此类 会发现,
 *   虽然人物监听器同样也是作为扩展元素, 但是 这里并没有发现对任务监听器的 处理总计
 *   其实不难理解,
 *   因为流程三大要素中的节点都可以使用执行监听器, 而任务监听器只可以在任务节点定义和使用
 *   因此任务监听器的解析工作,只需要在任务节点解析的时候处理即可
 *   UserTaskXMLConverter
 *
 *
 */
public class ExtensionElementsParser implements BpmnXMLConstants {
  
  public void parse(XMLStreamReader xtr, List<SubProcess> activeSubProcessList, Process activeProcess, BpmnModel model) throws Exception {
    BaseElement parentElement = null;
    // 解析 extensionElements  之前 先 确定  extensionElements 的父级元素类型
    if (!activeSubProcessList.isEmpty()) {  //
      parentElement = activeSubProcessList.get(activeSubProcessList.size() - 1);
      
    } else {
      parentElement = activeProcess;
    }
    
    boolean readyWithChildElements = false;
    while (readyWithChildElements == false && xtr.hasNext()) {
      xtr.next();
      if (xtr.isStartElement()) {
        //如果是executionListner  执行监听器
        if (ELEMENT_EXECUTION_LISTENER.equals(xtr.getLocalName())) {
          new ExecutionListenerParser().parseChildElement(xtr, parentElement, model);
          //eventListner   事件监听器
        } else if (ELEMENT_EVENT_LISTENER.equals(xtr.getLocalName())){
        	new ActivitiEventListenerParser().parseChildElement(xtr, parentElement, model);
        	//  流程启动人 监听
        } else if (ELEMENT_POTENTIAL_STARTER.equals(xtr.getLocalName())){
          new PotentialStarterParser().parse(xtr, activeProcess);
          //如果子元素不是以上3种类型 中的任何一个  则开始解析用户自定义元素
        } else {
          ExtensionElement extensionElement = BpmnXMLUtil.parseExtensionElement(xtr);
          parentElement.addExtensionElement(extensionElement);
        }

      } else if (xtr.isEndElement()) {
        if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
          readyWithChildElements = true;
        }
      }
    }
  }
}



















