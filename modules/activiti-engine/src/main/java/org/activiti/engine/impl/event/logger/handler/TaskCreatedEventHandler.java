package org.activiti.engine.impl.event.logger.handler;

import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 *  创建任务  触发
 *
 *   日志数据是 如何生成的?????
 *   DatabaseEventFlusher 中的 closing() 在何种场景下被调用???
 */
public class TaskCreatedEventHandler extends AbstractTaskEventHandler {
	
	@Override
	public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
		//获取taskEntity
		TaskEntity task = (TaskEntity) ((ActivitiEntityEvent) event).getEntity();
		//封装通用字段
		Map<String, Object> data = handleCommonTaskFields(task);
		// 调用此() 生成 日志数据
    return createEventLogEntry(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getExecutionId(), task.getId(), data);
	}

}













