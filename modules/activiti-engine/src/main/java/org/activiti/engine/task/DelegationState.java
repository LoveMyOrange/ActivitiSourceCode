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

package org.activiti.engine.task;


/** Defines the different states of delegation that a task can be in.
 * 
 * @author Tom Baeyens
 * 任务委派的枚举
 */
public enum DelegationState {

  /**
   * The owner delegated the task and wants to review the result 
   * after the assignee has resolved the task.  When the assignee 
   * completes the task, the task is marked as {@link #RESOLVED} and 
   * sent back to the owner. When that happens, the owner is set as 
   * the assignee so that the owner gets this task back in the ToDo.
   */
  PENDING, //会用来查看   被委派且未处理的任务(委派给我)
  
  /**
   * The assignee has resolved the task, the assignee was set to the owner 
   * again and the owner now finds this task back in the ToDo list for review.
   * The owner now is able to complete the task. 
   */
  RESOLVED //会用来查看  被委派且已经处理完成的任务 (委派人来查看  我的委派)
}
