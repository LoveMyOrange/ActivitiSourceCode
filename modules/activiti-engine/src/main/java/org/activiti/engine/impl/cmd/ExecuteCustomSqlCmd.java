package org.activiti.engine.impl.cmd;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

/**
 * @author jbarrez
 */
public class ExecuteCustomSqlCmd<Mapper, ResultType> implements Command<ResultType> {
	
	protected Class<Mapper> mapperClass;
	protected CustomSqlExecution<Mapper, ResultType> customSqlExecution;
	
	public ExecuteCustomSqlCmd(Class<Mapper> mapperClass, CustomSqlExecution<Mapper, ResultType> customSqlExecution) {
		this.mapperClass = mapperClass;
		this.customSqlExecution = customSqlExecution;
	}
	/*
	*
	* */
	@Override
	public ResultType execute(CommandContext commandContext) {
		//通过命令拦截器 获取 DBSqlSession对象  然后获取SqlSession
		Mapper mapper = commandContext.getDbSqlSession().getSqlSession().getMapper(mapperClass);
		return customSqlExecution.execute(mapper);
	}
	
}
