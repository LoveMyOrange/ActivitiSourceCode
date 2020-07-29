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
package org.activiti.engine.impl.bpmn.listener;

import java.util.List;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.bpmn.helper.ClassDelegate;
import org.activiti.engine.impl.bpmn.parser.FieldDeclaration;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.ExecutionListenerInvocation;
import org.activiti.engine.impl.delegate.JavaDelegateInvocation;


/**
 * @author Joram Barrez
 * 负责调度执行 delegateExpression 方式创建的执行监听器
 *
 * Activiti  对 delegateExpression 和 class 定义的执行监听器  处理逻辑几乎相同
 */
public class DelegateExpressionExecutionListener implements ExecutionListener {
  
  protected Expression expression;
  private final List<FieldDeclaration> fieldDeclarations;
  
  public DelegateExpressionExecutionListener(Expression expression, List<FieldDeclaration> fieldDeclarations) {
    this.expression = expression;
    this.fieldDeclarations = fieldDeclarations;
  }
  
  public void notify(DelegateExecution execution) throws Exception {
    // Note: we can't cache the result of the expression, because the
    // execution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
    // 获取监听器 对象
    Object delegate = expression.getValue(execution);
    //填充属性
    ClassDelegate.applyFieldDeclaration(fieldDeclarations, delegate);
    //判断是否是 ExecutionListener 或者 JavaDelegate 并根据 delegate对象的类型触发相应的监听器
    if (delegate instanceof ExecutionListener) {
      Context.getProcessEngineConfiguration()
        .getDelegateInterceptor()
        .handleInvocation(new ExecutionListenerInvocation((ExecutionListener) delegate, execution));
    } else if (delegate instanceof JavaDelegate) {
      Context.getProcessEngineConfiguration()
        .getDelegateInterceptor()
        .handleInvocation(new JavaDelegateInvocation((JavaDelegate) delegate, execution));
    } else {
      throw new ActivitiIllegalArgumentException("Delegate expression " + expression 
              + " did not resolve to an implementation of " + ExecutionListener.class 
              + " nor " + JavaDelegate.class);
    }
  }

  /**
   * returns the expression text for this execution listener. Comes in handy if you want to
   * check which listeners you already have.
   */  
  public String getExpressionText() {
    return expression.getExpressionText();
  }

}
