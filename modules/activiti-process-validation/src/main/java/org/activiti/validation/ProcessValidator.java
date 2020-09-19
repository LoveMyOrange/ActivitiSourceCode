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

import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.validation.validator.ValidatorSet;

/**
 * Validates a process definition against the rules of the Activiti engine to be executable 
 * 
 * @author jbarrez
 *
 * 默认的模型校验器
 */
public interface ProcessValidator {

	/**
	 * Validates the provided {@link BpmnModel} and returns a list
	 * of all {@link ValidationError} occurences found.
	 *
	 * 校验的()  返回 List<ValidationError> (ValidationError 封装的是验证之后的信息)
	 * 如果集合长度为0,则说明 bpmnModel对象 已经成功通过校验,否则没有验证通过
	 *
	 */
	List<ValidationError> validate(BpmnModel bpmnModel);
	
	/**
	 * Returns the {@link ValidatorSet} instances for this process validator.
	 * Useful if some validation rules need to be disabled. 
	 */
	List<ValidatorSet> getValidatorSets();
	
}
