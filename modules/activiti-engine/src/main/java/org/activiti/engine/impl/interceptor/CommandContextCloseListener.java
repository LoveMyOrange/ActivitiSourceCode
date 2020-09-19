package org.activiti.engine.impl.interceptor;

/**
 * A listener that can be used to be notified of the closure of a {@link CommandContext}.
 * 
 * @author Joram Barrez
 *  DB日志 清洗器的 顶层类
 *  定义了2个()
 *
 */
public interface CommandContextCloseListener {
	//命令上下文正在关闭中
	void closing(CommandContext commandContext);
	///命令上下文已经关闭
	void closed(CommandContext commandContext);

}
