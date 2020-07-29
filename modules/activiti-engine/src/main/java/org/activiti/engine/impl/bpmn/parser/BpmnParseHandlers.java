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
package org.activiti.engine.impl.bpmn.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.DataObject;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.engine.parse.BpmnParseHandler;
import org.slf4j.Logger;


/**
 * @author Joram Barrez
 *  解析对象的入口
 *  此类定义了解析对象的() 内部有  List<BpmnParseHandler>> 集合
 *  这样 流程引擎解析对象的时候 就可以从 BpmnParseHandler 集合中查找到该对象的所有解析器 ,
 *  从而完成对象解析工作
 */
public class BpmnParseHandlers {
  
  private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BpmnParseHandlers.class);
  
  protected Map<Class<? extends BaseElement>, List<BpmnParseHandler>> parseHandlers;
  
  public BpmnParseHandlers() {
    this.parseHandlers = new HashMap<Class<? extends BaseElement>, List<BpmnParseHandler>>();
  }
  /*
  * 负责查找对象解析器集合
  * */
  public List<BpmnParseHandler> getHandlersFor(Class<? extends BaseElement> clazz) {
    return parseHandlers.get(clazz);
  }
  /*
  * 负责添加对象解析器
  *
  * 首先遍历  bpmnParseHandlers 对象 的  getHandledTypes() 的返回值 (对应流程文档解析后转化为Activiti内部表示类BaseElement)
  * 然后 在遍历 查找BaseElement 对象所有的解析器集合 hanlers ,并且最终将 bpmnParseHandler 值添加到 parseHandlers中
  * */
  public void addHandlers(List<BpmnParseHandler> bpmnParseHandlers) {
    for (BpmnParseHandler bpmnParseHandler : bpmnParseHandlers) {
      addHandler(bpmnParseHandler);
    }
  }
  /*
  * handlers 集合为Llist 数据结构 , 由此可知
  * 同一个baseElement 可以存在多个  对象解析器  对其进行解析
  * 对于需要动态添加内置记录监听器的BaseElement来说,
  * 经过上述步骤之后 ,该实例对象的所有对象的历史解析器, 已经被成功添加
  * 这样解析该 实例对象时, 就可以取出该实例的所有对象解析器,
  * 并且循环遍历  parseHandlers集合 进行对象的解析工作
  * */
  public void addHandler(BpmnParseHandler bpmnParseHandler) {
    for (Class<? extends BaseElement> type : bpmnParseHandler.getHandledTypes()) {
      List<BpmnParseHandler> handlers = parseHandlers.get(type);
      if (handlers == null) {
        handlers = new ArrayList<BpmnParseHandler>();
        parseHandlers.put(type, handlers);
      }
      handlers.add(bpmnParseHandler);
    }
  }
  /*

  概述:  负责根据对象的类型查找对象解析器并且调度解析器完成对象解析工作

  负责process对象的解析工作
      需要分析一下 bpmnParseHandlers 对象的初始化过程
      该对象的初始化工作非常重要, 因为如果该对象为空, 则无法进行对象的解析工作

      此对象 的初始化 是在  ProcessEngineConfigurationImpl 的 getDefaultDeployers() 中

          List<BpmnParseHandler> parseHandlers = new ArrayList<BpmnParseHandler>();
    if(getPreBpmnParseHandlers() != null) { //前置对象解析器
      parseHandlers.addAll(getPreBpmnParseHandlers());
    }
    parseHandlers.addAll(getDefaultBpmnParseHandlers()); //内置对象解析器
    if(getPostBpmnParseHandlers() != null) {// 后置对象解析器
      parseHandlers.addAll(getPostBpmnParseHandlers());
    }
    //
    BpmnParseHandlers bpmnParseHandlers = new BpmnParseHandlers();
    bpmnParseHandlers.addHandlers(parseHandlers);
    bpmnParser.setBpmnParserHandlers(bpmnParseHandlers);
    bpmnDeployer.setBpmnParser(bpmnParser);

    上面的代码 意义是什么???
        parseHandlers的初始化过程 可以分为
        初始化前置对象解析器,
        内置对象解析器,
        自定义内置对象解析器
        后置对象解析器

        这样的设计好处 是  前置 和后置 定义完全交给客户端
        如果客户端没有手动干预对象解析器的初始化工作,
        那么引擎就使用系统内置的 对象解析器进行处理
        parseHandlers集合使用List结构 存储对象解析器
        如果客户端分别 定义了 前置和 后置对象解析器,
        同一个对象的解析器 存在多个
        那么Actiiti 使用何种策略定位对象解析器呢????


    具体功能
    解析 elment 之前 判断 element对象的类型

   */
  public void parseElement(BpmnParse bpmnParse, BaseElement element) {
    
    if (element instanceof DataObject) { //如果是DataObject 对象 则不解析
      // ignore DataObject elements because they are processed on Process and Sub process level
      return;
    }
    /*
    如果是 FlowElement 则需要设置bpmnParse 对象中的  setCurrentFlowElement 属性值 为当前准备解析的element对象
    这样设计的意图 在于
    所有的对象解析器 最终都会调用 parseElement()
    在当前对象解析之前 ,将其保存在bpmnParse 对象中

    下一个对象解析时可以从bpmnParse 对象中获取到上一个对象的解析结果

  例如现在 对  a b c  3个对象按照顺序进行解析
  这样解析a 对象之前 将 a 设置到  setCurrentFlowElement 属性中
  b对象 解析时 就可以获取到 a 对象的解析结果,
  同理 C 对象 就可以获取到B对象


     */
    if (element instanceof FlowElement) {
      bpmnParse.setCurrentFlowElement((FlowElement) element);
    }
    
    // Execute parse handlers
    /*
    从集合中获取对应的解析器
    因为所有的对象解析器 都存储在 parseHandlers集合中
    所以根据对象的类型 可以很轻松的获取该对象的解析器 集合
    parseHandlers 为Map 结构

     */
    List<BpmnParseHandler> handlers = parseHandlers.get(element.getClass());
    
    if (handlers == null) {
      LOGGER.warn("Could not find matching parse handler for + " + element.getId() + " this is likely a bug.");
    } else {
      for (BpmnParseHandler handler : handlers) {
        handler.parse(bpmnParse, element);//循环集合 查询具体的对象解析器类 ,然后依次调用handler 对象中的parse () 进行对象解析工作
      }
    }
  }

}
