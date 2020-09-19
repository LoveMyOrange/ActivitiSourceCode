package org.activiti.engine.impl.event.logger;

import org.activiti.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * 并 closing () 进行实现
 */
public class DatabaseEventFlusher extends AbstractEventFlusher {
	
	private static final Logger logger = LoggerFactory.getLogger(DatabaseEventFlusher.class);
	/*
	此() 完成了 日志数据的入库操作
	 */
	@Override
	public void closing(CommandContext commandContext) {
		// 根据  CommandContext 获取 事件日志 实体管理类  elem对象   EventLogEntryEntityManager 负责 ACT_EVT_LOG 表的操作
		EventLogEntryEntityManager eventLogEntryEntityManager = commandContext.getEventLogEntryEntityManager();
		//循环遍历  eventHandlers  通过 eventHandlers . generateEventLogEntry  () 生成  EventLogEntry 对象
		for (EventLoggerEventHandler eventHandler : eventHandlers) {
			try {
				//用elem的 insert() 将日志数据 添加到会话缓存
				eventLogEntryEntityManager.insert(eventHandler.generateEventLogEntry(commandContext));
			} catch (Exception e) {
				logger.warn("Could not create event log", e);
			}
		}
	}
	
}












