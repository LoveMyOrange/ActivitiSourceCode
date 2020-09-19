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
package org.activiti.engine.impl.bpmn.parser.factory;

import java.util.ArrayList;
import java.util.List;

import org.activiti.bpmn.model.FieldExtension;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.parser.FieldDeclaration;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.FixedValue;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Joram Barrez
 *  此类内部有 表达式管理器
 *  主要负责将 FeeldExtension 类型的集合 转化为 FeldDeclaration (activiti:field 元素 )
 */
public abstract class AbstractBehaviorFactory {
  /*
  * 内部
  * */
  protected ExpressionManager expressionManager;
  /*
  * createFieldDeclarations() 主要工作就是将监听器中的 FieldExtension 转化为 FieldDeclaration类型对象
  *
  * 因为监听器中的表达式属性解析之后 被封装为FieldExtension
  * 而PVM 运转的过程中需要的属性类型为 FieldDeclaration
  * 所以需要统一转化
  * */
  public List<FieldDeclaration> createFieldDeclarations(List<FieldExtension> fieldList) {
    List<FieldDeclaration> fieldDeclarations = new ArrayList<FieldDeclaration>();

    for (FieldExtension fieldExtension : fieldList) {//遍历filedlist集合
      FieldDeclaration fieldDeclaration = null;
      if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
        fieldDeclaration = new FieldDeclaration(fieldExtension.getFieldName(), Expression.class.getName(), 
            expressionManager.createExpression(fieldExtension.getExpression()));
      } else {
        fieldDeclaration = new FieldDeclaration(fieldExtension.getFieldName(), Expression.class.getName(), 
            new FixedValue(fieldExtension.getStringValue()));
      }
      
      fieldDeclarations.add(fieldDeclaration);
    }
    return fieldDeclarations;
  }

  
  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

}
