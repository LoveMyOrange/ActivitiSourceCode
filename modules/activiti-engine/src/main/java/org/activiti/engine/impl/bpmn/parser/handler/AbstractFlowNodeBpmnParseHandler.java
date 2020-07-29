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

import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;


/**
 * @author Joram Barrez
 * 调用父类解析对象的同时, 负责执行监听器的添加工作
 *
 *  监听器 是如何添加到BaseElement中的 也和此类有关
 *
 *  该类 是  对象解析器的模板类存在,
 *  pase() 主要完成 2大功能
 *  1) 调用父类() 对BaseElement对象进行解析
 *  2) BaseElement对象的解析结果(ActivityImpl对象 ) 添加执行监听器,
 */
public abstract class AbstractFlowNodeBpmnParseHandler<T extends FlowNode> extends AbstractBpmnParseHandler<T> {
  
  @Override
  public void parse(BpmnParse bpmnParse, BaseElement element) {
    super.parse(bpmnParse, element);//调用父类进行element的解析 然后最终 将element转化为ActivityImpl对象
    /*
    调用父类为element对象添加执行监听器
    因为所有的流程元素都可以配置执行监听器,
    调用createExecutionListenersOnScope () 全局调度执行监听器的添加工作,
    该操作只针对于 监听器的添加, , 任务监听器并不在这里进行操作

    第1个参数 是 bpmnParse 对象
    第2个参数是 元素中配置的所有监听器集合 (元素配置的监听器解析之后转化为引擎的 内部表示类 ActivitListener
    第3个参数是 element 解析之后 对应的 ActivityImpl 对象
     */
    createExecutionListenersOnScope(bpmnParse, ((FlowNode) element).getExecutionListeners(), findActivity(bpmnParse, element.getId()));
  }
  
  
}
