package org.activiti.engine.impl.bpmn.helper;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;

/*
* Activiti  忽略节点功能
* */
public class SkipExpressionUtil {
  /*
  *
  * */
  public static boolean isSkipExpressionEnabled(ActivityExecution execution, Expression skipExpression) {
    //非空校验
    if (skipExpression == null) {
      return false;
    }
    //_ACTIVITI_SKIP_EXPRESSION_ENABLED 获取 变量值 , 如果该 值为 空  直接返回false
    final String skipExpressionEnabledVariable = "_ACTIVITI_SKIP_EXPRESSION_ENABLED";
    Object isSkipExpressionEnabled = execution.getVariable(skipExpressionEnabledVariable);
    //
    if (isSkipExpressionEnabled == null) {
      return false;
      
    }
    //如果isSkipExpressionEnabled 不是 Boolena 直接 报错
    else if (isSkipExpressionEnabled instanceof Boolean) {
      return ((Boolean) isSkipExpressionEnabled).booleanValue();
      
    } else {
      throw new ActivitiIllegalArgumentException(skipExpressionEnabledVariable + " variable does not resolve to a boolean. " + isSkipExpressionEnabled);
    } 
  }
  
  public static boolean shouldSkipFlowElement(ActivityExecution execution, Expression skipExpression) {
    Object value = skipExpression.getValue(execution);
    
    if (value instanceof Boolean) {
      return ((Boolean)value).booleanValue();
      
    } else {
      throw new ActivitiIllegalArgumentException("Skip expression does not resolve to a boolean: " + skipExpression.getExpressionText());
    }
  }
}
