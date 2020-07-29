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

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;


/**
 * @author Tom Baeyens
 * 
 * BE AWARE: For Start Events this is done in the ProcessDefinitionEntity!
 *  负责将 历史数据插入 到 ACT_HI_ACTINST 表中
 */
public class ActivityInstanceStartHandler implements ExecutionListener {
  /*
  *  获取历史管理器  ,然后 调用 记录活动开始()
  * */
  public void notify(DelegateExecution execution) {
    Context.getCommandContext().getHistoryManager()
      .recordActivityStart((ExecutionEntity) execution);
  }
}
