package org.activiti.engine.impl.event.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.event.logger.handler.ActivityCompensatedEventHandler;
import org.activiti.engine.impl.event.logger.handler.ActivityCompletedEventHandler;
import org.activiti.engine.impl.event.logger.handler.ActivityErrorReceivedEventHandler;
import org.activiti.engine.impl.event.logger.handler.ActivityMessageEventHandler;
import org.activiti.engine.impl.event.logger.handler.ActivitySignaledEventHandler;
import org.activiti.engine.impl.event.logger.handler.ActivityStartedEventHandler;
import org.activiti.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.activiti.engine.impl.event.logger.handler.ProcessInstanceEndedEventHandler;
import org.activiti.engine.impl.event.logger.handler.ProcessInstanceStartedEventHandler;
import org.activiti.engine.impl.event.logger.handler.SequenceFlowTakenEventHandler;
import org.activiti.engine.impl.event.logger.handler.TaskAssignedEventHandler;
import org.activiti.engine.impl.event.logger.handler.TaskCompletedEventHandler;
import org.activiti.engine.impl.event.logger.handler.TaskCreatedEventHandler;
import org.activiti.engine.impl.event.logger.handler.VariableCreatedEventHandler;
import org.activiti.engine.impl.event.logger.handler.VariableDeletedEventHandler;
import org.activiti.engine.impl.event.logger.handler.VariableUpdatedEventHandler;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 * 虽然此类作为 全局日志监听器 ,但其存在不太合理,完全不符合Activiti中引擎配置类处理开关属性的一贯作风,
 * 在 initDatabaseEventLogging() 中 , 直接实例化了 本类,
 * 如果开发人员不打算使用 此类处理日志,就需要重新定义一个类, 然后继承 流程引擎配置类,并重写父类的 initDatabaseEventLogging()
 *
 * 如果在 当前类中定义一个 ActivitiEventListener类型的变量(开关属性) 并根据变量值执行不同的逻辑,岂不是更好???
 *
 * 此类本质上 是一个全局事件监听器,
 */
public class EventLogger implements ActivitiEventListener {
	
	private static final Logger logger = LoggerFactory.getLogger(EventLogger.class);
	
	private static final String EVENT_FLUSHER_KEY = "eventFlusher";
	
	protected Clock clock;
	protected ObjectMapper objectMapper;
	
	// Mapping of type -> handler
	protected Map<ActivitiEventType, Class<? extends EventLoggerEventHandler>> eventHandlers 
		= new HashMap<ActivitiEventType, Class<? extends EventLoggerEventHandler>>();
	
	// Listeners for new events
	protected List<EventLoggerListener> listeners;
	/*
	* 用于初始化一系列的事件处理类
	* */
	public EventLogger() {
		initializeDefaultHandlers(); //负责将常用的事件以及事件对应的日志处理器 通过 addEventHandler() 添加到 eventHandlers集合中
	}
	
	public EventLogger(Clock clock, ObjectMapper objectMapper) {
		this();
		this.clock = clock;
		this.objectMapper = objectMapper;
	}
	/*
		此() 负责将常用的事件 以及该事件对应的日志处理器通过  addEventHandler 添加到 eventHandler集合中   Map结构

		如果觉得 此() 的初始化事件不能满足业务日志收集需求, 可以自定义一个日志监听器 并且继承EventLogger类
		然后通过 addEventHandler() 将 期望处理的事件 以及该事件对应的日志处理器, 添加到eventHandlers 集合中 即可
	 */
	protected void initializeDefaultHandlers() {
		//添加 一系列的日志处理器
	  addEventHandler(ActivitiEventType.TASK_CREATED, TaskCreatedEventHandler.class);
		addEventHandler(ActivitiEventType.TASK_COMPLETED, TaskCompletedEventHandler.class);
		addEventHandler(ActivitiEventType.TASK_ASSIGNED, TaskAssignedEventHandler.class);
		
		addEventHandler(ActivitiEventType.SEQUENCEFLOW_TAKEN, SequenceFlowTakenEventHandler.class);
		
		addEventHandler(ActivitiEventType.ACTIVITY_COMPLETED, ActivityCompletedEventHandler.class);
		addEventHandler(ActivitiEventType.ACTIVITY_STARTED, ActivityStartedEventHandler.class);
		addEventHandler(ActivitiEventType.ACTIVITY_SIGNALED, ActivitySignaledEventHandler.class);
		addEventHandler(ActivitiEventType.ACTIVITY_MESSAGE_RECEIVED, ActivityMessageEventHandler.class);
		addEventHandler(ActivitiEventType.ACTIVITY_COMPENSATE, ActivityCompensatedEventHandler.class);
		addEventHandler(ActivitiEventType.ACTIVITY_ERROR_RECEIVED, ActivityErrorReceivedEventHandler.class);
		
		addEventHandler(ActivitiEventType.VARIABLE_CREATED, VariableCreatedEventHandler.class);
		addEventHandler(ActivitiEventType.VARIABLE_DELETED, VariableDeletedEventHandler.class);
		addEventHandler(ActivitiEventType.VARIABLE_UPDATED, VariableUpdatedEventHandler.class);
  }
	/*
		日志监听器的转发事件方法
	 */
	@Override
	public void onEvent(ActivitiEvent event) {
		// 获取 事件对应的日志处理器,
		EventLoggerEventHandler eventHandler = getEventHandler(event);
		if (eventHandler != null) {
			// 如果 获取到了 事件处理器, 则开始执行如下操作,  获取  currentCommandContext
			CommandContext currentCommandContext = Context.getCommandContext();
			// commandContext类中的attributes 属性就是专门为 存储日志清洗器 服务的
			EventFlusher eventFlusher = (EventFlusher) currentCommandContext.getAttribute(EVENT_FLUSHER_KEY);
			//如果 eventHandler 不为空 并且 日志清洗器不存在
			if (eventHandler != null && eventFlusher == null) {
				//创建 日志清洗器 , 该方法 返回值为null 并没有创建任何对象
				/*
				该方法存在的意义就是  方便开发人员自定义日志清洗器
				 */
				eventFlusher = createEventFlusher(); //
				//如果createEventFlusher()  并没有成功创建日志清洗器
				if (eventFlusher == null) {
					//实例化系统内置的清洗器
					eventFlusher = new DatabaseEventFlusher(); // Default
				}
				//并设置到 attributes 以便后续获取
				currentCommandContext.addAttribute(EVENT_FLUSHER_KEY, eventFlusher);
				//将 日志清洗器 添加到  CloseListener 集合中
				currentCommandContext.addCloseListener(eventFlusher);
				//用匿名类 遍历  当前类 中的listners 集合  并开始添加 日志监听器
				currentCommandContext
				    .addCloseListener(new CommandContextCloseListener() {

					    @Override
					    public void closing(CommandContext commandContext) {
					    }

					    @Override
					    public void closed(CommandContext commandContext) {
						    // For those who are interested: we can now broadcast the events were added
								if (listeners != null) {
									for (EventLoggerListener listener : listeners) {
										listener.eventsAdded(EventLogger.this);
									}
								}
					    }
					    
				    });
			}
			//将事件对应的日志处理器 注册到  eventFlusher 类的 evnetHandler集合中
			eventFlusher.addEventHandler(eventHandler);
		}
	}
	/*

	 */
	protected EventLoggerEventHandler getEventHandler(ActivitiEvent event) {

		Class<? extends EventLoggerEventHandler> eventHandlerClass = null;
		/*
		1)判断事件类型 ,如果类型为  ENTITY_INITIALIZED  则获取该事件对应的 日志处理器 ActivitiEntityEvent
		并且 从 该处理器中 提取当前的实体对象   entity 接着判断 entity对象是否为 ExecutionEntity 对象
		如果 是 就需要对其进行转换并且开始设置 当前事件的日志处理器
		 */
		if (event.getType().equals(ActivitiEventType.ENTITY_INITIALIZED)) {
			Object entity = ((ActivitiEntityEvent) event).getEntity();
			if (entity instanceof ExecutionEntity) {
				ExecutionEntity executionEntity = (ExecutionEntity) entity;
				if (executionEntity.getProcessInstanceId().equals(executionEntity.getId())) {
					eventHandlerClass = ProcessInstanceStartedEventHandler.class;
				}
			}
			/*
					1)判断事件类型 ,如果类型为  ENTITY_DELETED  则获取该事件对应的 日志处理器 ActivitiEntityEvent
		并且 从 该处理器中 提取当前的实体对象   entity 接着判断 entity对象是否为 ExecutionEntity 对象
		如果 是 就需要对其进行转换并且开始设置 当前事件的日志处理器
			* */
		} else if (event.getType().equals(ActivitiEventType.ENTITY_DELETED)) {
			Object entity = ((ActivitiEntityEvent) event).getEntity();
			if (entity instanceof ExecutionEntity) {
				ExecutionEntity executionEntity = (ExecutionEntity) entity;
				if (executionEntity.getProcessInstanceId().equals(executionEntity.getId())) {
					eventHandlerClass = ProcessInstanceEndedEventHandler.class;
				}
			}
		} else {
			/*
			 如果不是以上2种 那么 根据事件类型 eventHandlers 集合中查找该事件对应的日志处理器,
			 从 该() 可以看出
			 eventHandlers 的初始化很重要  所有事件的日志处理器 都存储在该集合中
			 */
			eventHandlerClass = eventHandlers.get(event.getType());
		}
		
		if (eventHandlerClass != null) {
			//实例化  日志 处理器,  然后此() 通过反射 创建日志处理器,然后为其填充属性值 并作为该() 的返回值
			return instantiateEventHandler(event, eventHandlerClass); //() 内部通过反射
		}
		//没找到 返回null
		return null;
	}

	protected EventLoggerEventHandler instantiateEventHandler(ActivitiEvent event,
      Class<? extends EventLoggerEventHandler> eventHandlerClass) {
		try {
			EventLoggerEventHandler eventHandler = eventHandlerClass.newInstance();
			eventHandler.setTimeStamp(clock.getCurrentTime());
			eventHandler.setEvent(event);
			eventHandler.setObjectMapper(objectMapper);
			return eventHandler;
		} catch (Exception e) {
			logger.warn("Could not instantiate " + eventHandlerClass + ", this is most likely a programmatic error");
		}
		return null;
  }
	
	@Override
  public boolean isFailOnException() {
		return false;
  }
	
	public void addEventHandler(ActivitiEventType eventType, Class<? extends EventLoggerEventHandler> eventHandlerClass) {
		eventHandlers.put(eventType, eventHandlerClass);
	}
	
	public void addEventLoggerListener(EventLoggerListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<EventLoggerListener>(1);
		}
		listeners.add(listener);
	}
	
	/**
	 * Subclasses that want something else than the database flusher should override this method
	 */
	protected EventFlusher createEventFlusher() {
		return null;
	}

	public Clock getClock() {
		return clock;
	}

	public void setClock(Clock clock) {
		this.clock = clock;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<EventLoggerListener> getListeners() {
		return listeners;
	}

	public void setListeners(List<EventLoggerListener> listeners) {
		this.listeners = listeners;
	}
	
}
