package org.activiti.engine.impl.cmd;

/**
 * @author jbarrez
 * 该抽象类 实现父类
 * 并对 getMapperClass() 提供了实现 ,
 * 由于该抽象类 没有显式定义无参构造()
 * 因此开发人员自定义并且继承该类的时候 必须要有一个有参构造 (参数的个数  以及 类型 需要与父类中的有参构造 一致
 */
public abstract class AbstractCustomSqlExecution<Mapper, ResultType> implements CustomSqlExecution<Mapper, ResultType> {
	
	protected Class<Mapper> mapperClass;
	
	public AbstractCustomSqlExecution(Class<Mapper> mapperClass) {
		this.mapperClass = mapperClass;
	}
	
	@Override
	public Class<Mapper> getMapperClass() {
	  return mapperClass;
	}
	
}
