package org.activiti.engine.impl.cmd;

/**
 * @author jbarrez
 * 自定义SQL 如何执行的呢 ????
 */
public interface CustomSqlExecution<Mapper, ResultType> {
	//获取Mapper映射类
	Class<Mapper> getMapperClass();
	//执行Mapper类的()  execute()
	ResultType execute(Mapper mapper);

}