/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.interceptor;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tom Baeyens
 *
 * 该类负责 命令上下文 CommadnContext
 *    CommadnContext 类 负责在命令执行时进行参数传递, 不同的命令对象统一从 CommadnContext 中取出需要的信息
 *    例如 异常信息, 流程引擎对象, 原子类 等等
 *
 *
 * 以及上下文 Context的初始化工作
 * 并且在所有的命令对象执行完毕, 调用上下文的关闭()
 * 该类很重要 负责处理Activiti中数据 例如  插入  更新 删除
 *
 *
 */
public class CommandContextInterceptor extends AbstractCommandInterceptor {
  private static final Logger log = LoggerFactory.getLogger(CommandContextInterceptor.class);

  protected CommandContextFactory commandContextFactory;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  public CommandContextInterceptor() {
  }

  public CommandContextInterceptor(CommandContextFactory commandContextFactory, ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.commandContextFactory = commandContextFactory;
    this.processEngineConfiguration = processEngineConfiguration;
  }
  /*
  *  也需要分析一下
  * Activiti 存储了会话缓存数据之后 ,是如何将 缓存中的数据刷新到DB的
  *  查看 finaaly中   关闭命令拦截器
  * */
  public <T> T execute(CommandConfig config, Command<T> command) {
    //1)获取命令上下文
    CommandContext context = Context.getCommandContext();
    /*
    设置 上下文是否需要初始化
    默认为false
    如果 没有获取到命令上下文,
    或者命令配置信息中明确指定已经存在的命令上下文类是不可以重复使用的(之前已经获取的命令上下文实例对象)
    或者已经存在的命令上下文对象中记录有异常信息,
    则
     */
    boolean contextReused = false;
    // We need to check the exception, because the transaction can be in a rollback state,
    // and some other command is being fired to compensate (eg. decrementing job retries)
    if (!config.isContextReusePossible() || context == null || context.getException() != null) {
      //重新实例化命令上下文
    	context = commandContextFactory.createCommandContext(command);    	
    }
    //如果经过层层判断 发现不需要重新实例化命令上下文 则 直接设置为true
    else {
    	log.debug("Valid context found. Reusing it for the current command '{}'", command.getClass().getCanonicalName());
    	contextReused = true;
    }

    try {
      // Push on stack
      //填充对象
      Context.setCommandContext(context);
      Context.setProcessEngineConfiguration(processEngineConfiguration);
      //调用下一个拦截器
      return next.execute(config, command);
      
    } catch (Exception e) {
    	/*
    	异常处理
    	如果以上执行过程中 任何一个环节出错,
    	需要将异常信息捕获并且设置到 context 对象中
    	CommandContext 对象的 exception 属性 负责记录异常信息
    	 */
      context.exception(e);
      
    } finally {
      try {
        //关闭命令上下问
    	  if (!contextReused) {
    		  context.close();
    	  }
      } finally {
    	  // Pop from stack
    	  Context.removeCommandContext();
    	  Context.removeProcessEngineConfiguration();
    	  Context.removeBpmnOverrideContext();
      }
    }
    
    return null;
  }
  
  public CommandContextFactory getCommandContextFactory() {
    return commandContextFactory;
  }
  
  public void setCommandContextFactory(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }

  public void setProcessEngineContext(ProcessEngineConfigurationImpl processEngineContext) {
    this.processEngineConfiguration = processEngineContext;
  }
}
