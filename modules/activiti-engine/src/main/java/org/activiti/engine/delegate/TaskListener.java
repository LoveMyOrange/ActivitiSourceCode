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

package org.activiti.engine.delegate;

import java.io.Serializable;





/**
 * @author Tom Baeyens
 * 任务监听器的实现类
 *
 */
public interface TaskListener extends Serializable {

  String EVENTNAME_CREATE = "create"; //任务被创建 并且所有的属性被设置好之后触发
  String EVENTNAME_ASSIGNMENT = "assignment";//当任务设置执行人之后触发(注意:当流程到达一个任务节点时,会先触发assignment事件再触发create事件)
  String EVENTNAME_COMPLETE = "complete";//在任务完成之后,并且DB中的运行时数据相关表删除数据之前
  String EVENTNAME_DELETE = "delete";//在任务将要被删除之前发生,一般是completeTask完成任务时,他也会被执行
  
  
  /**
   * Not an actual event, used as a marker-value for {@link TaskListener}s that should be called for all events,
   * including {@link #EVENTNAME_CREATE}, {@link #EVENTNAME_ASSIGNMENT} and {@link #EVENTNAME_COMPLETE} and {@link #EVENTNAME_DELETE}.
    以上的事件都可以触发
   */
  String EVENTNAME_ALL_EVENTS = "all"; //  以上的事件都可以触发
  
  void notify(DelegateTask delegateTask);
}
