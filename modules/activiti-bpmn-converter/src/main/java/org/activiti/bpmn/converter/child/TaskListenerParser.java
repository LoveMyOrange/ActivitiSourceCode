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
package org.activiti.bpmn.converter.child;

import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.UserTask;

/**
 * @author Tijs Rademakers
 */
public class TaskListenerParser extends ActivitiListenerParser {

  public String getElementName() {
  	return ELEMENT_TASK_LISTENER;
  }
  /*
  *
  *
  * */
  public void addListenerToParent(ActivitiListener listener, BaseElement parentElement) {
    //判断当前元素是否为任务节点
    if (parentElement instanceof UserTask) {
      //如果是 则通过 getTaskListeners ()  获取存储当前监听器的集合 ,然后将Listener元素添加到该集合中, 否则不予添加操作
      ((UserTask) parentElement).getTaskListeners().add(listener);
    }
  }
}


















