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

package org.activiti.engine.impl.history.handler;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.TaskEntity;


/**
 * @author Tom Baeyens
 *  负责 更新历史环节表   ACT_HI_ACTINST中的 任务节点处理人 对应 该表的 ASSIGNEE_列
 */
public class UserTaskAssignmentHandler implements TaskListener {

  public void notify(DelegateTask task) {
   Context.getCommandContext().getHistoryManager()
     .recordTaskAssignment((TaskEntity) task);
  }
  
}
