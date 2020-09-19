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
package org.activiti.engine.delegate.event;

import org.activiti.engine.EngineServices;

/**
 * Describes an event that occurred in the Activiti Engine which is dispatched to external
 * listeners, if any.
 * 
 * @author Frederik Heremans
 * 定义了获取事件类型 执行实例ID  流程实例ID   流程定义ID 的()
 * 以及获取EngineServices对象的()  所有的事件转发器都需要 直接或者间接  的实现 该接口
 *
 *
 * Activiti 5.15中实现了一种事件机制。它允许在引擎触发事件时获得提醒。 参考所有支持的事件类型了解有效的事件。
 *
 * 可以为对应的事件类型注册监听器，在这个类型的任何时间触发时都会收到提醒。
 * 你可以添加引擎范围的事件监听器通过配置， 添加引擎范围的事件监听器在运行阶段使用API，
 * 或添加event-listener到特定流程定义的BPMN XML中。
 *
 * 所有分发的事件，都是org.activiti.engine.delegate.event.ActivitiEvent的子类。
 * 事件包含（如果有效）type，executionId，processInstanceId和processDefinitionId。
 * 对应的事件会包含事件发生时对应上下文的额外信息， 这些额外的载荷可以在支持的所有事件类型中找到。
 */
public interface ActivitiEvent {

	/**
	 * @return type of event.
	 */
	ActivitiEventType getType();

	/**
	 * @return the id of the execution this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getExecutionId();
	
	/**
	 * @return the id of the process instance this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getProcessInstanceId();
	
	/**
	 * @return the id of the process definition this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getProcessDefinitionId();
	
	/**
	 * @return the {@link EngineServices} associated to the engine this event
	 * originated from. Returns null, when not called from within a listener call or when no
	 * Activiti context is active.
	 */
	EngineServices getEngineServices();
}
