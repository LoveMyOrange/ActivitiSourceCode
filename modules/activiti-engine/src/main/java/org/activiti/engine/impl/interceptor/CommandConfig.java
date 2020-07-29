package org.activiti.engine.impl.interceptor;

import org.activiti.engine.impl.cfg.TransactionPropagation;

/**
 * Configuration settings for the command interceptor chain.
 * 
 * Instances of this class are immutable, and thus thread- and share-safe.
 * 
 * @author Marcus Klimstra (CGI)
 * 命令配置类, 该类负责控制事务的传播行为, 检测命令上下文是否可用
 */
public class CommandConfig {

  private boolean contextReusePossible;// 命令上下文 是否可以继续使用的标识变量
  private TransactionPropagation propagation;//事务传播行为
  
  public CommandConfig() {
    this.contextReusePossible = true;
    this.propagation = TransactionPropagation.REQUIRED;
  }
  
  public CommandConfig(boolean contextReusePossible) {
    this.contextReusePossible = contextReusePossible;
    this.propagation = TransactionPropagation.REQUIRED;
  }
  
  public CommandConfig(boolean contextReusePossible, TransactionPropagation transactionPropagation) {
    this.contextReusePossible = contextReusePossible;
    this.propagation = transactionPropagation;
  }
  
  protected CommandConfig(CommandConfig commandConfig) {
    this.contextReusePossible = commandConfig.contextReusePossible;
    this.propagation = commandConfig.propagation;
  }

  public boolean isContextReusePossible() {
    return contextReusePossible;
  }
  
  public TransactionPropagation getTransactionPropagation() {
    return propagation;
  }

  public CommandConfig setContextReusePossible(boolean contextReusePossible) {
    CommandConfig config = new CommandConfig(this);
    config.contextReusePossible = contextReusePossible;
    return config;
  }
  
  public CommandConfig transactionRequired() {
    CommandConfig config = new CommandConfig(this);
    config.propagation = TransactionPropagation.REQUIRED;
    return config;
  }

  public CommandConfig transactionRequiresNew() {
    CommandConfig config = new CommandConfig();
    config.contextReusePossible = false;
    config.propagation = TransactionPropagation.REQUIRES_NEW;
    return config;
  }

  public CommandConfig transactionNotSupported() {
    CommandConfig config = new CommandConfig();
    config.contextReusePossible = false;
    config.propagation = TransactionPropagation.NOT_SUPPORTED;
    return config;
  }
}
