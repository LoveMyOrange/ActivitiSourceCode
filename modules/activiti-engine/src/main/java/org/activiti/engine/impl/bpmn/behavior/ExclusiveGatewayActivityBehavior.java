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
package org.activiti.engine.impl.bpmn.behavior;

import java.util.Iterator;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.Condition;
import org.activiti.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * implementation of the Exclusive Gateway/XOR gateway/exclusive data-based gateway
 * as defined in the BPMN specification.
 * 
 * @author Joram Barrez
 * 排他 网关 行为类 原理
 */
public class ExclusiveGatewayActivityBehavior extends GatewayActivityBehavior {
  
  private static final long serialVersionUID = 1L;
  
  private static Logger log = LoggerFactory.getLogger(ExclusiveGatewayActivityBehavior.class);
  
  /**
   * The default behaviour of BPMN, taking every outgoing sequence flow
   * (where the condition evaluates to true), is not valid for an exclusive
   * gateway. 
   * 
   * Hence, this behaviour is overriden and replaced by the correct behavior:
   * selecting the first sequence flow which condition evaluates to true
   * (or which hasn't got a condition) and leaving the activity through that
   * sequence flow. 
   * 
   * If no sequence flow is selected (ie all conditions evaluate to false),
   * then the default sequence flow is taken (if defined).
   *
   *  一个排他网关 可以对应多个出口
   *  而出口可以通过  condition 属性值设置 ,
   *
   *  不管有多少个符合条件的出口
   *          execution.take(outgoingSeqFlow);  只会执行一条出口
   */
  @Override
  protected void leave(ActivityExecution execution) {
    
    if (log.isDebugEnabled()) {
      log.debug("Leaving activity '{}'", execution.getActivity().getId());
    }
    
    PvmTransition outgoingSeqFlow = null;
    //1)获取排他网关中 默认连线的id 值
    String defaultSequenceFlow = (String) execution.getActivity().getProperty("default");
    //2)得到排他网关所有的出口
    Iterator<PvmTransition> transitionIterator = execution.getActivity().getOutgoingTransitions().iterator();
    while (outgoingSeqFlow == null && transitionIterator.hasNext()) {
      PvmTransition seqFlow = transitionIterator.next();
      /*
      3) 判断是否 配置了   跳过表达式   ,如果当前被遍历的出口 配置了 该属性, 则
      只要"跳过表达式 "  返回true 就会 立即停止顺序流的遍历工作
       */
      Expression skipExpression = seqFlow.getSkipExpression();
      
      if (!SkipExpressionUtil.isSkipExpressionEnabled(execution, skipExpression)) {
        Condition condition = (Condition) seqFlow.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
        /*
        否则继续查询 并且验证   连线表达式 是否符合 条件(包括没有配置条件表达式的出口)
        如果符合条件,立即停止遍历工作
        * */
        if ( (condition == null && (defaultSequenceFlow == null || !defaultSequenceFlow.equals(seqFlow.getId())) ) 
                || (condition != null && condition.evaluate(seqFlow.getId(), execution)) ) {
          if (log.isDebugEnabled()) {
            log.debug("Sequence flow '{}'selected as outgoing sequence flow.", seqFlow.getId());
          }
          outgoingSeqFlow = seqFlow;
        }
      }
      else if (SkipExpressionUtil.shouldSkipFlowElement(execution, skipExpression)){
        outgoingSeqFlow = seqFlow;
      }
    }

    /*
    * 如果查询到了符合条件的出口  ,  执行代码
    * */
    if (outgoingSeqFlow != null) {
      execution.take(outgoingSeqFlow);
    }
    //判断  默认连线 是否为空
    else {
      
      if (defaultSequenceFlow != null) {
        //以该值作为查询条件 ,检查该值 是否是排他网关的出口
        PvmTransition defaultTransition = execution.getActivity().findOutgoingTransition(defaultSequenceFlow);
        //如果是 则 执行 take()
        if (defaultTransition != null) {
          execution.take(defaultTransition);
        }
        //直接报错
        else {
          throw new ActivitiException("Default sequence flow '" + defaultSequenceFlow + "' not found");
        }
      } else {
        //No sequence flow could be found, not even a default one
        throw new ActivitiException("No outgoing sequence flow of the exclusive gateway '"
              + execution.getActivity().getId() + "' could be selected for continuing the process");
      }
    }
  }

}
