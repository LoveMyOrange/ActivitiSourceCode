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

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;

/**
 * Class capable of dispatching events.
 * 
 * @author Frederik Heremans
 * @desc
 * 该类对 ActivitiEventDispatcher 进行实现,  内部持有  ActivitiEventSupport 实例
 * 并且可以根据当前类中的enabled 属性值决定是否可以 对事件进行转发
 * 该类 委托 ActivitiEventSupport 对象完成 事件监听器的注册 移除 和 转发功能
 *
 * 由于 ProcessEngineConfigurationImpl 类中持有 ActivitiEventDispatcher 对象的引用
 * 因此开发人员 可以通过  ProcessEngineConfigurationImpl 获取 ActivitiEventDispatcher
 * 对象进行 事件监听器的 注册 移除 以及事件转发工作
 */
public class ActivitiEventDispatcherImpl implements ActivitiEventDispatcher {

	protected ActivitiEventSupport eventSupport;
	protected boolean enabled = true;

	public ActivitiEventDispatcherImpl() {
		eventSupport = new ActivitiEventSupport();
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void addEventListener(ActivitiEventListener listenerToAdd) {
		eventSupport.addEventListener(listenerToAdd);
	}

	@Override
	public void addEventListener(ActivitiEventListener listenerToAdd, ActivitiEventType... types) {
		eventSupport.addEventListener(listenerToAdd, types);
	}

	@Override
	public void removeEventListener(ActivitiEventListener listenerToRemove) {
		eventSupport.removeEventListener(listenerToRemove);
	}
		/*
		事件监听器 如何 被触发??
	这个()
	该() 只是做了一些辅助工作
	核心处理工作还是委托给了 eventSupport 对象中的  dispatchEvent()


		为什么 Actcitii   要使用 ProcessDefinitionEntity 类中的 eventSupport 对象
		对事件进行转发呢???

		ProcessDefinitionEntity中持有的 eventSupport 对象
		和 ActivitiEventSupport 是两个完全不同的对象

		因为这是 Activiti 5.15的遗留问题,
		在Activiti 5.15 之前的版本中
		事件类型以及事件监听器 仅仅可以配置在流程文档中 并且只能配置在process元素中

		如果开发人员需要修改事件监听器 ,  那么 就必须对流程文档进行重新配置和部署

		这里的代码 主要是为了 新老版本的兼容

		* */
	@Override
	public void dispatchEvent(ActivitiEvent event) {
		// 判断为true  执行 eventSupport的dispatchEvent()
		if (enabled) {
			eventSupport.dispatchEvent(event);
		}
		//判断 执行上下文是否存活
		if (Context.isExecutionContextActive()) {
			/*
			如果  存在 则说明 流程实例正在 运转并且 流程实例对象已经被初始化
			通过 ExecutionContext 对象 获取  definition 对象
			 */
			ProcessDefinitionEntity definition = Context.getExecutionContext().getProcessDefinition();
			/*
			判断 此对象是否为空 ,如果不为空,
			委托 getEventSupport对象进行事件转发操作
			 */

			if (definition != null) {
				definition.getEventSupport().dispatchEvent(event);
			}
			//如果ExecutionContext 不存在 说明流程实例 还没有开始运转, 那么开始查找commandContext对象
		} else {

			CommandContext commandContext = Context.getCommandContext();
			//如果 commandContext 不为空
			if (commandContext != null) {
				//获取ProcessDefinitionEntity
				ProcessDefinitionEntity processDefinition = extractProcessDefinitionEntityFromEvent(event);
				//如果该对象不为空
				if (processDefinition != null) {
					processDefinition.getEventSupport().dispatchEvent(event);
				}
			}
		}
	}

	/**
	 * In case no process-context is active, this method attempts to extract a
	 * process-definition based on the event. In case it's an event related to an
	 * entity, this can be deducted by inspecting the entity, without additional
	 * queries to the database.
	 * 
	 * If not an entity-related event, the process-definition will be retrieved
	 * based on the processDefinitionId (if filled in). This requires an
	 * additional query to the database in case not already cached. However,
	 * queries will only occur when the definition is not yet in the cache, which
	 * is very unlikely to happen, unless evicted.
	 * 
	 * @param event
	 * @return
	 */
	protected ProcessDefinitionEntity extractProcessDefinitionEntityFromEvent(ActivitiEvent event) {
		ProcessDefinitionEntity result = null;

		if (event.getProcessDefinitionId() != null) {
			result = Context.getProcessEngineConfiguration().getDeploymentManager().getProcessDefinitionCache()
			    .get(event.getProcessDefinitionId());
			if (result != null) {
				result = Context.getProcessEngineConfiguration().getDeploymentManager().resolveProcessDefinition(result);
			}
		}

		if(result == null && event instanceof ActivitiEntityEvent) {
			Object entity = ((ActivitiEntityEvent) event).getEntity();
			if(entity instanceof ProcessDefinition) {
				result = (ProcessDefinitionEntity) entity;
			}
		}
		return result;
	}

}
