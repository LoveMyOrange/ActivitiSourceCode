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
package org.activiti.engine.parse;

import java.util.Collection;

import org.activiti.bpmn.model.BaseElement;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * Allows to hook into the parsing of one or more elements during the parsing of a BPMN 2.0 process.
 * For more details, see the userguide section on bpmn parse handlers.
 * 
 * Instances of this class can be injected into the {@link ProcessEngineConfigurationImpl}.
 * The handler will then be called whenever a BPMN 2.0 element is parsed that matches
 * the types returned by the {@link #getHandledTypes()} method.
 * 
 * @see AbstractBpmnParseHandler
 * 
 * @author Joram Barrez
 *  定义了 将 BaseElment 对象转化为ActivityImpl 对象 或者 TransitionImpl 对象的()
 *  以及根据BaseElement 的具体类型查找其 对应的对象解析器()
 *    概述 : 定义了对象解析() 以及 获取指定对象解析器的()
 *  所有的对象解析器 都直接 或者 间接 实现 该接口
 *
 */
public interface BpmnParseHandler {
  
  /**
   * The types for which this handler must be calleding during process parsing.
   * 该()的返回值决定了 该类可以解析哪些对象
   * 例如 SequencFlowPparseHandler 中 该() 的返回值为 SequenceFlow.class
   * 这样 需要解析 SequenceFlow 时, 就可以直接使用  SequencFlowPparseHandler类对其进行解析
   */
  Collection<Class<? extends BaseElement>> getHandledTypes();
  
  /**
   * The actual delegation method. The parser will calls this method on a
   * match with the {@link #getHandledTypes()} return value.
   * @param bpmnParse The {@link BpmnParse} instance that acts as container
   *                  for all things produced during the parsing.
   *  解析BaseElement对象, 根据BaseElement创建不同的ActivityImpl 对象 以及行为类实例对象
   * 以及行为类实例对象,  并最终将解析结果存放到 bpmnParse 对象中
   *                  这样后续可以直接从bpmnParse 对象中获取已经解析过的对象  无需再次重复解析
   *                  bpmnParse 对象 为BpmnParse 类型
   *                  该对象存储了对象解析的公共属性值,
   *                  因此可以将 BpmnParse理解为对象解析的上下文
   */
  void parse(BpmnParse bpmnParse, BaseElement element);

}

























