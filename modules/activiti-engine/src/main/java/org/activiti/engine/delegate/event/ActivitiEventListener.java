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

/**
 * Describes a class that listens for {@link ActivitiEvent}s dispatched by the engine.
 *  
 * @author Frederik Heremans
 * @desc  定义了转发事件的()  onEvent 以及事件 监听器 执行时 如果程序出现 异常
 * 该如何处理的() isFailOnException
 *
 *
 * 实现事件监听器的唯一要求是实现org.activiti.engine.delegate.event.ActivitiEventListener。 西面是一个实现监听器的例子，它会把所有监听到的事件打印到标准输出中，包括job执行的事件异常：
 */
public interface ActivitiEventListener {

	/**
	 * Called when an event has been fired
	 * @param event the event
	 */
	void onEvent(ActivitiEvent event);
	
	/**
	 * @return whether or not the current operation should fail when this listeners execution
	 * throws an exception.
	 * isFailOnException()方法决定了当事件分发时，onEvent(..)方法抛出异常时的行为。
	 * 这里返回的是false，会忽略异常。 当返回true时，异常不会忽略，继续向上传播，迅速导致当前命令失败。
	 * 当事件是一个API调用的一部分时（或其他事务性操作，比如job执行）， 事务就会回滚。
	 * 当事件监听器中的行为不是业务性时，建议返回false。
	 *
	 */
	boolean isFailOnException();
}

















