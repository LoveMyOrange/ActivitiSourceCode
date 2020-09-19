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
package org.activiti.validation;

import org.activiti.validation.validator.ValidatorSetFactory;

/**
 * @author jbarrez
 * 模型校验器工厂   用于校验 BpmnModel部署时
 *
 * BpmnModel校验器可以用来校验BpmnModel是否构造成功，可以对BpmnModel每一个元素进行合法校验，
 * 除了BpmnModel构造正确性校验之外，我们还可以根据业务场景设计自定义模型校验器。
 * 比如业务需要流程中如果包含用户任务，那么用户任务必须指定处理人等等，
 * 这种需求我们完全可以通过自定义模型校验器进行实现。
 *
 */
public class ProcessValidatorFactory {
//			创建默认的模型校验器
	public ProcessValidator createDefaultProcessValidator() {
		/*
		创建一个 ProcessValidatorImpl ,该对象持有 validatorSets 的一个集合

		顾名思义,这个集合中包含着所有的校验器, ProcessValidatorImpl进行模型校验时
		会遍历这个集合,再针对每一个 validatorSet中持有的 Validator 实现遍历  对BpmnModel进行校验
		并且将 校验错误 添加到 List<ValidationError> 中, 遍历结束之后返回 ValidationError结果集

		如果想实现自定义的校验器, 只需要实现我们自己的ValidatorSet然后添加到 ProcessValidatorImpl
		的 validatorSets 集合中,
		那么如何自定义 ValidatorSet 呢?? 首先看看 ValidatorSet 有哪些东西

		从ValidatorSet构成来看, 校验器的重点是 Validator  也就是说所有的模型校验器应该实现
		Validator接口 ,这里我们查看 Validator 结构, 可以看出来 activiti 有很多实现
		这些实现分别校验bpmn流程文档中不同的元素节点
		eg:
		ProcessLevelValidator  校验process 元素以及它的所有子节点
		ErrorValidator: 校验error节点 该节点为 process兄弟节点
		其他校验器也是同理, 如果我们想定义用户任务节点校验器,
		所以只需要继承和实现 ProcessLevelValidator 就可以了

		 */

		ProcessValidatorImpl processValidator = new ProcessValidatorImpl();
		//
		processValidator.addValidatorSet(new ValidatorSetFactory().createActivitiExecutableProcessValidatorSet());
		return processValidator;
	}

}

























