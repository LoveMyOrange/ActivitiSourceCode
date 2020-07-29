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
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.Condition;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.UelExpressionCondition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Joram Barrez
 * 连线对象解析
 */
public class SequenceFlowParseHandler extends AbstractBpmnParseHandler<SequenceFlow> {
  
  public static final String PROPERTYNAME_CONDITION = "condition";
  public static final String PROPERTYNAME_CONDITION_TEXT = "conditionText";

  public Class< ? extends BaseElement> getHandledType() {
    return SequenceFlow.class;
  }

  protected void executeParse(BpmnParse bpmnParse, SequenceFlow sequenceFlow) {
    
    ScopeImpl scope = bpmnParse.getCurrentScope();
    //获取连线节点的源
    ActivityImpl sourceActivity = scope.findActivity(sequenceFlow.getSourceRef());
    //目的地
    ActivityImpl destinationActivity = scope.findActivity(sequenceFlow.getTargetRef());
    //获取连线中的配置 的 跳过表达式
    Expression skipExpression;
    if (StringUtils.isNotEmpty(sequenceFlow.getSkipExpression())) {
      ExpressionManager expressionManager = bpmnParse.getExpressionManager();
      skipExpression = expressionManager.createExpression(sequenceFlow.getSkipExpression());
    } else {
      skipExpression = null;
    }
    //实例化TransitionImpl类 ,该对象在PVM 中的最终表示
    TransitionImpl transition = sourceActivity.createOutgoingTransition(sequenceFlow.getId(), skipExpression);
    //填充 对象属性 name documentation  conditionText, condition

    bpmnParse.getSequenceFlows().put(sequenceFlow.getId(), transition);
    transition.setProperty("name", sequenceFlow.getName());
    transition.setProperty("documentation", sequenceFlow.getDocumentation());
    transition.setDestination(destinationActivity);

    if (StringUtils.isNotEmpty(sequenceFlow.getConditionExpression())) {
      Condition expressionCondition = new UelExpressionCondition(sequenceFlow.getConditionExpression());
      transition.setProperty(PROPERTYNAME_CONDITION_TEXT, sequenceFlow.getConditionExpression());
      transition.setProperty(PROPERTYNAME_CONDITION, expressionCondition);
    }
    /*
      添加连线中配置的 执行监听器
    * */
    createExecutionListenersOnTransition(bpmnParse, sequenceFlow.getExecutionListeners(), transition);
    
  }

}
