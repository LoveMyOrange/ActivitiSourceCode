package org.activiti.engine.impl.persistence;

import org.activiti.engine.impl.history.DefaultHistoryManager;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;

/**
 * @author Joram Barrez
 * 该类 负责 负责 维护 DefaultHistoryManager ,
 * 为什么需要将历史实体管理类进行单独维护
 *
 * 如果 开发二元觉得 Activiti  对历史数据处理不够友好,  可以自定义一个类 并且 重写 DefaultHistoryManager
 * 从而自己控制历史数据
 *
 */
public class DefaultHistoryManagerSessionFactory implements SessionFactory {
	
	public java.lang.Class<?> getSessionType() {
		return HistoryManager.class; 
	}
	
	@Override
	public Session openSession() {
		return new DefaultHistoryManager();
	}

}
