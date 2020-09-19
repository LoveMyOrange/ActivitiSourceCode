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
package org.activiti.engine.delegate.event.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that allows adding and removing event listeners and dispatching events
 * to the appropriate listeners.
 * 
 * @author Frederik Heremans
 * @desc
 * 事件监听器 的注册 和移除工作 最终是通过 此类完成的.
 *
 * 该类内部维护了一个 全局事件监听器集合 eventListenrs 和具体类型的事件监听器 typedListeners
 * 并且定义了 这两个集合的 添加 删除 以及转发不同事件的方法
 *
 * 一种是 全局事件监听器  一种是具体类型的事件监听器  只负责部分事件的监听
 *
 *  此类 主要是 完成事件监听器的注册 和移除工作
 *
 *
 *  		因为向eventListenrs 集合中 添加元素是 可能存在多个 线程同时 对集合内容进行操作
 * 		所以使用了 synchronized 进行修饰
 * 	ActivitiEventSupport  中的   eventListeners  是 List结构 使用了 CopyOnWriteArrayList
 *
 */
public class ActivitiEventSupport {

	private static final Logger LOG = LoggerFactory.getLogger(ActivitiEventSupport.class);

	protected List<ActivitiEventListener> eventListeners;
	protected Map<ActivitiEventType, List<ActivitiEventListener>> typedListeners;

	public ActivitiEventSupport() {
		eventListeners = new CopyOnWriteArrayList<ActivitiEventListener>();
		typedListeners = new HashMap<ActivitiEventType, List<ActivitiEventListener>>();
	}
	/*
		用于注册 全局事件监听器

	 */
	public synchronized void addEventListener(ActivitiEventListener listenerToAdd) {
		if (listenerToAdd == null) {//非空校验
			//
			throw new ActivitiIllegalArgumentException("Listener cannot be null.");
		}
		//判断   eventListeners 集合中是否已经存在了  listenerToAdd  如果集合中不存在   则添加到 集合中
		// 为了避免重复注册监听器
		if (!eventListeners.contains(listenerToAdd)) {
			eventListeners.add(listenerToAdd);
		}
	}
	/*
		注册具体类型事件监听器
	 */
	public synchronized void addEventListener(ActivitiEventListener listenerToAdd, ActivitiEventType... types) {
		if (listenerToAdd == null) { //
			throw new ActivitiIllegalArgumentException("Listener cannot be null.");
		}

		if (types == null || types.length == 0) { //判断 type是否为空 .如果为空则直接将当前的事件监听器 作为全局事件监听器 进行处理
			//并且通过第1行定义的() 将其 注册到 eventListeners 集合中
			addEventListener(listenerToAdd);//注册全局事件监听器
		
		}
		//如果不为空 循环数组 ,  注册具体类型的事件监听器
		else {
  		for (ActivitiEventType type : types) {
  			addTypedEventListener(listenerToAdd, type);//注册具体类型的事件监听器
  		}
		}
	}

	public void removeEventListener(ActivitiEventListener listenerToRemove) {
		eventListeners.remove(listenerToRemove);

		for (List<ActivitiEventListener> listeners : typedListeners.values()) {
			listeners.remove(listenerToRemove);
		}
	}
	/*

	 */
	public void dispatchEvent(ActivitiEvent event) {
/*]
		对转发的事件进行校验 如果任意一个为空 直接报错 ,
		因为转发一个空事件没有任何意义

		如果事件类型为空 ,事件监听器 又该如何检测到该类型的事件呢???
		其实 这个地方的处理还有一个 小缺陷
		因为不论 event 参数为空 还是 getType() 为空
		 都抛出 相同的异常信息 , 那么 开发人员 无法定位是 这两个 谁为空 而导致程序出错的
		 Activiti 5.22 解决了这个问题
 */
		if (event == null) {
			throw new ActivitiIllegalArgumentException("Event cannot be null.");
		}

		if (event.getType() == null) {
			throw new ActivitiIllegalArgumentException("Event type cannot be null.");
		}
		//如果不为空
		if (!eventListeners.isEmpty()) {
			//循环遍历
			//1) 先执行 所有的全局事件监听器,
			for (ActivitiEventListener listener : eventListeners) {
				dispatchEvent(event, listener);
			}
		}
		//2) 然后开始执行具体类型的监听器

		/*
		那么结论是
		如果 开发人员定义了 一个 全局事件监听器 A   和 具体类型的事件监听器B   分别对TASK_CREATED 的事件进行监听处理
		则首先执行全局事件监听器A 然后执行具体类型的事件监听器B
		同一个事件 可以对应多个 事件监听器
		 */
		List<ActivitiEventListener> typed = typedListeners.get(event.getType());
		//
		if (typed != null && !typed.isEmpty()) {
			for (ActivitiEventListener listener : typed) {
				dispatchEvent(event, listener);
			}
		}
	}
	/*
	该() 很重要
	首先 触发 listner 对象的 onEvent() 并为该() 传入event 参数值(事件所对应的类 ,
	如果程序出现异常,
		根据  isFailOnException 返回值进行处理
		如果为true 对异常零容忍,  程序直接报错, 流程实例不会继续向下运转
		如果false  则忽略异常信息, 流程实例继续运转而不受干扰

		观察者模式
			定义对象之间 一对一 或者一对多的依赖关系 ,
			当一个对象的状态发生变化时, 所有依赖于它的对象都会得到通知并自动更新
			ActivitiEventSupport 就是 目标对象
			该对象中定义了 观察者对象 ActivitiEventListener  的添加和 删除操作 以及通知 观察者的() dispatchEvent

	 */
	protected void dispatchEvent(ActivitiEvent event, ActivitiEventListener listener) {
		try {
			listener.onEvent(event);
		} catch (Throwable t) {
			//根据
			if (listener.isFailOnException()) {
				throw new ActivitiException("Exception while executing event-listener", t);
			} else {
				// Ignore the exception and continue notifying remaining listeners. The
				// listener
				// explicitly states that the exception should not bubble up
				LOG.warn("Exception while executing event-listener, which was ignored", t);
			}
		}
	}
	/*
	用于注册 具体类型的事件监听器
	 */
	protected synchronized void addTypedEventListener(ActivitiEventListener listener, ActivitiEventType type) {
		//根据type参数 从typedListeners 集合中获取该事件下所有的事件监听器集合 listeners
		List<ActivitiEventListener> listeners = typedListeners.get(type);
		//如果为空  初始化该集合并且添加到  typedListeners 集合中
		if (listeners == null) {
			// Add an empty list of listeners for this type
			listeners = new CopyOnWriteArrayList<ActivitiEventListener>();
			typedListeners.put(type, listeners);
		}
		/*
		如果集合中不存在 该元素  则将其添加到listeners 集合中
		 */
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
}










