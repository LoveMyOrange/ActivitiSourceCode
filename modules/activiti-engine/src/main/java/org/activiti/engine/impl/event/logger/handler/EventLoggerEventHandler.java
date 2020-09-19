package org.activiti.engine.impl.event.logger.handler;

import java.util.Date;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 * 日志处理器顶层类
 */
public interface EventLoggerEventHandler {
	// 用于生成   EventLogEntryEntity 对象   此对象为 ACT_EVT_LOG 表对应的实体类
	EventLogEntryEntity generateEventLogEntry(CommandContext commandContext);
		//用于设置 事件类型
	void setEvent(ActivitiEvent event);
	//设置事件发生的时间
	void setTimeStamp(Date timeStamp);
	//设置objectMapper 对象
	void setObjectMapper(ObjectMapper objectMapper);
	
}












