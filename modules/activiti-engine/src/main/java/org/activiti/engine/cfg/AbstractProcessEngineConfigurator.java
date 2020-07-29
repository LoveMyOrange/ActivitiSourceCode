package org.activiti.engine.cfg;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * @author jbarrez
 *  对ProcessEngineConfigurator 接口进行了实现 ,作为模板抽象类存在, 并且重写了getPriporty() 默认值 1w
 */
public abstract class AbstractProcessEngineConfigurator implements ProcessEngineConfigurator {
	
	public static int DEFAULT_CONFIGURATOR_PRIORITY = 10000;
	
	@Override
	public int getPriority() {
		return DEFAULT_CONFIGURATOR_PRIORITY;
	}
	
	public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    
  }
	
	public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
	  
	}

}
