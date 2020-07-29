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

import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.MultiInstanceLoopCharacteristics;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.activiti.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.apache.commons.lang3.StringUtils;


/**
 * @author Joram Barrez
 * 调用父类解析对象的同时, 添加了多实例对象的解析工作,
 * 所有的多实例对象都在该类中进行相应的解析 实现
 *
 *  多实例任务节点和非多实例任务节点最终都用ActivityImpl 类型的对象进行表示,
 *  唯一不同的地方就是对象的属性值 和 行为类
 *
 *
 */
public abstract class AbstractActivityBpmnParseHandler<T extends FlowNode> extends AbstractFlowNodeBpmnParseHandler<T> {
  /*
  *  如何解析多实例对象的
  * */
  @Override
  public void parse(BpmnParse bpmnParse, BaseElement element) {
    /*
    * 调用父类进行解析工作
    * 因为多实例任务节点本质还是任务节点, 所以首先需要对任务进行解析,
    * 解析完成之后 如果该任务时多实例任务 则进行 如下操作
    * */
    super.parse(bpmnParse, element); //
    //如果元素是多实例,开始解析多实例
    if (element instanceof Activity
            && ((Activity) element).getLoopCharacteristics() != null) {
      //解析多实例,并且 填充属性
      createMultiInstanceLoopCharacteristics(bpmnParse, (Activity) element);
    }
  }
  
  protected void createMultiInstanceLoopCharacteristics(BpmnParse bpmnParse, Activity modelActivity) {
    //获取流程文档中多实例的配置信息
    MultiInstanceLoopCharacteristics loopCharacteristics = modelActivity.getLoopCharacteristics();
    
    // Activity Behavior
    MultiInstanceActivityBehavior miActivityBehavior = null;
    /*
      获取多实例配置的节点, 如果任务节点还没有解析则直接报错
    * */
    ActivityImpl activity = bpmnParse.getCurrentScope().findActivity(modelActivity.getId());
    if (activity == null) {
      throw new ActivitiException("Activity " + modelActivity.getId() + " needed for multi instance cannot bv found");
    }
    //判断是并行还是串行 多实例,  并添加不同的行为类
    if (loopCharacteristics.isSequential()) {
      miActivityBehavior = bpmnParse.getActivityBehaviorFactory().createSequentialMultiInstanceBehavior(
              activity, (AbstractBpmnActivityBehavior) activity.getActivityBehavior()); 
    } else {
      miActivityBehavior = bpmnParse.getActivityBehaviorFactory().createParallelMultiInstanceBehavior(
              activity, (AbstractBpmnActivityBehavior) activity.getActivityBehavior());
    }
    
    // ActivityImpl settings
    //设置scope为true ,后续多实例任务节点的运转需要区别对待
    activity.setScope(true);
    activity.setProperty("multiInstance", loopCharacteristics.isSequential() ? "sequential" : "parallel");
    activity.setActivityBehavior(miActivityBehavior);
    
    ExpressionManager expressionManager = bpmnParse.getExpressionManager();
    BpmnModel bpmnModel = bpmnParse.getBpmnModel();
    
    // loopcardinality
    if (StringUtils.isNotEmpty(loopCharacteristics.getLoopCardinality())) {
      miActivityBehavior.setLoopCardinalityExpression(expressionManager.createExpression(loopCharacteristics.getLoopCardinality()));
    }
    
    // completion condition
    if (StringUtils.isNotEmpty(loopCharacteristics.getCompletionCondition())) {
      miActivityBehavior.setCompletionConditionExpression(expressionManager.createExpression(loopCharacteristics.getCompletionCondition()));
    }
    
    // activiti:collection
    if (StringUtils.isNotEmpty(loopCharacteristics.getInputDataItem())) {
      if (loopCharacteristics.getInputDataItem().contains("{")) {
        miActivityBehavior.setCollectionExpression(expressionManager.createExpression(loopCharacteristics.getInputDataItem()));
      } else {
        miActivityBehavior.setCollectionVariable(loopCharacteristics.getInputDataItem());
      }
    }

    // activiti:elementVariable
    if (StringUtils.isNotEmpty(loopCharacteristics.getElementVariable())) {
      miActivityBehavior.setCollectionElementVariable(loopCharacteristics.getElementVariable());
    }

    // activiti:elementIndexVariable
    if (StringUtils.isNotEmpty(loopCharacteristics.getElementIndexVariable())) {
      miActivityBehavior.setCollectionElementIndexVariable(loopCharacteristics.getElementIndexVariable());
    }

  }

}
