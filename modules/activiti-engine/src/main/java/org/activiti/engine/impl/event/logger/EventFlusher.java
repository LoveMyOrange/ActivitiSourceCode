package org.activiti.engine.impl.event.logger;

import java.util.List;

import org.activiti.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;

/**
 * @author Joram Barrez
 *  在  CommandContextCloseListener 基础上 增加了 获取日志处理器, 添加日志处理器的支持
 */
public interface EventFlusher extends CommandContextCloseListener {
	
	List<EventLoggerEventHandler> getEventHandlers();

	void setEventHandlers(List<EventLoggerEventHandler> eventHandlers);
	
	void addEventHandler(EventLoggerEventHandler databaseEventLoggerEventHandler);
	
}
