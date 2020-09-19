package org.activiti.engine.impl.event.logger.handler;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 *   继承  AbstractDatabaseEventLoggerEventHandler 同时作为  所有与任务相关的 事件处理类的父类 ,
 *   并定义了 模板()   handleCommonTaskFields
 *   	具体的实现类 有 3个
 *   		TaskCreatedEventHandler  (创建任务)
 *   		TaskCompleteedEventHandler  (完成任务)
 *   		TaskAssignedEventHandler  (给任务分配处理人)
 *
 */
public abstract class AbstractTaskEventHandler extends AbstractDatabaseEventLoggerEventHandler {
	/*
	此()只服务于任务相关的操作, 所以定义在此类中
	 */
	protected Map<String, Object> handleCommonTaskFields(TaskEntity task) {
		Map<String, Object> data = new HashMap<String, Object>(); //实例化 data集合 作为 承载日志数据的容器
		//将 一系列的数据添加到集合中
		putInMapIfNotNull(data, Fields.ID, task.getId());
		putInMapIfNotNull(data, Fields.NAME, task.getName());
		putInMapIfNotNull(data, Fields.TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
		putInMapIfNotNull(data, Fields.DESCRIPTION, task.getDescription());
		putInMapIfNotNull(data, Fields.ASSIGNEE, task.getAssignee());
		putInMapIfNotNull(data, Fields.OWNER, task.getOwner());
		putInMapIfNotNull(data, Fields.CATEGORY, task.getCategory());
		putInMapIfNotNull(data, Fields.CREATE_TIME, task.getCreateTime());
		putInMapIfNotNull(data, Fields.DUE_DATE, task.getDueDate());
		putInMapIfNotNull(data, Fields.FORM_KEY, task.getFormKey());
		putInMapIfNotNull(data, Fields.PRIORITY, task.getPriority());
		putInMapIfNotNull(data, Fields.PROCESS_DEFINITION_ID, task.getProcessDefinitionId());
		putInMapIfNotNull(data, Fields.PROCESS_INSTANCE_ID, task.getProcessInstanceId());
		putInMapIfNotNull(data, Fields.EXECUTION_ID, task.getExecutionId());
		
		if (task.getTenantId() != null && !ProcessEngineConfigurationImpl.NO_TENANT_ID.equals(task.getTenantId())) {
			putInMapIfNotNull(data, Fields.TENANT_ID, task.getTenantId()); // Important for standalone tasks
		}
		return data;
  }
	
}
