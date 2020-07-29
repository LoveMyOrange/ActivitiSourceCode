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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.activiti.engine.JobNotFoundException;
import org.activiti.engine.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.persistence.entity.AttachmentEntityManager;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.engine.impl.persistence.entity.CommentEntityManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityManager;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.GroupIdentityManager;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.persistence.entity.MembershipIdentityManager;
import org.activiti.engine.impl.persistence.entity.ModelEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.persistence.entity.TableDataManager;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.activiti.engine.impl.pvm.runtime.AtomicOperation;
import org.activiti.engine.impl.pvm.runtime.InterpretableExecution;
import org.activiti.engine.logging.LogMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Agim Emruli
 * @author Joram Barrez
 * 命令上下文 该类负责在命令中进行参数的传递 ,
 * 不同的命令对象统一从该类中取出需要的参数, 并且把执行结果通过该类传递给上层
 */
public class CommandContext {

  private static Logger log = LoggerFactory.getLogger(CommandContext.class);

  protected Command< ? > command;
  protected TransactionContext transactionContext;
  protected Map<Class< ? >, SessionFactory> sessionFactories;
  protected Map<Class< ? >, Session> sessions = new HashMap<Class< ? >, Session>();
  protected Throwable exception = null;
  protected LinkedList<AtomicOperation> nextOperations = new LinkedList<AtomicOperation>();
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected FailedJobCommandFactory failedJobCommandFactory;
	protected List<CommandContextCloseListener> closeListeners;
  protected Map<String, Object> attributes; // General-purpose storing of anything during the lifetime of a command context

  /*
  *
  * */
  public void performOperation(AtomicOperation executionOperation, InterpretableExecution execution) {
    //添加到集合
    nextOperations.add(executionOperation);
    //如果giao集合只有 一个元素  执行if 否则 不处理
    if (nextOperations.size()==1) {
      try {
        Context.setExecutionContext(execution); //将execution添加到Context类中  方便后续获取
        //从  nextOperations 取出第一个元素
        while (!nextOperations.isEmpty()) {
          AtomicOperation currentOperation = nextOperations.removeFirst();
          if (log.isTraceEnabled()) {
            log.trace("AtomicOperation: {} on {}", currentOperation, this);
          }
          /*
          判断 是否有值,  (流程执行通常作为 执行实例的parent 存在) 最终都会调用原子类 AtomicOperation的execute()
          而在该() 处理当前逻辑的同时会设置下一个需要处理的类, 从而保证职责链中的类 依次执行
           */
          if (execution.getReplacedBy() == null) {
          	currentOperation.execute(execution);
          } else {
          	currentOperation.execute(execution.getReplacedBy());
          }
        }
      } finally {
        Context.removeExecutionContext();
      }
    }
  }
/*
* 构造()
* CommadnConext 构造 根据传入的对象 对 本类 进行属性填充
* command 为 即将要执行的命令类实例对象,
* pcf 为流程引擎配置类对象
* pcf参数可以获取session 会话工厂 和事务上下文 等
*
* CommadnContext中的exception属性封装了 命令拦截器执行过程中 可能出现的异常信息
* 如果程序出现异常, 则该类的close() 不会执行 flushSessions() 操作
*
* CommandContext 类 负责关联所有的实体管理类并且提供获取管理器实例对象的 ()
* transactionContext 事务上下文属性是为一系列的session服务我的
*
* transactionContext 控制事务的提交以及回滚操作,
* 那么可以从更高层次思考CommadnContext类
* 既然该类中持有 transactionContext 对象
* 那么 此类 肯定有一些() 涉及了 DB的入库操作
* */
  public CommandContext(Command<?> command, ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.command = command;
    this.processEngineConfiguration = processEngineConfiguration;
    this.failedJobCommandFactory = processEngineConfiguration.getFailedJobCommandFactory();
    sessionFactories = processEngineConfiguration.getSessionFactories();
    this.transactionContext = processEngineConfiguration
      .getTransactionContextFactory()
      .openTransactionContext(this);
  }
  /*
  *  里面涉及了 事务的提交 和 回滚
  * */
  public void close() {
    // the intention of this method is that all resources are closed properly, even
    // if exceptions occur in close or flush methods of the sessions or the
    // transaction context.

    try {
      try {
        try {
        	//如果没有出现任何异常, 并且 closeListeners 不为空
        	if (exception == null && closeListeners != null) { //
	        	try {
	        	  //循环遍历closing()
	        		for (CommandContextCloseListener listener : closeListeners) {
	        			listener.closing(this);
	        		}
	        	} catch (Throwable exception) {
	        		exception(exception);
	        	}
        	}
          //如果没有出现异常,  执行会话缓存刷新操作,  这个地方就设计了 引擎和DB的交互, 包括数据的入库,删除,更新操作 非常重要
          if (exception == null) {
            flushSessions();
          }

        } catch (Throwable exception) {
          exception(exception);
        } finally {
        	
          try {
            if (exception == null) {
              //开始提交事务, 如果事务提交过程中 没有出现异常信息, 并且 closeListeners 不为null
              transactionContext.commit();
            }
          } catch (Throwable exception) {
            exception(exception);
          }
          
        	if (exception == null && closeListeners != null) {
	        	try {
	        		for (CommandContextCloseListener listener : closeListeners) {
	        			listener.closed(this);//触发此()
	        		}
	        		//如果出现异常
	        	} catch (Throwable exception) {
	        		exception(exception);
	        	}
        	}

          if (exception != null) {
            if (exception instanceof JobNotFoundException || exception instanceof ActivitiTaskAlreadyClaimedException) {
              // reduce log level, because this may have been caused because of job deletion due to cancelActiviti="true"
              log.info("Error while closing command context", exception);
            } else if (exception instanceof ActivitiOptimisticLockingException) {
              // reduce log level, as normally we're not interested in logging this exception
              log.debug("Optimistic locking exception : " + exception);
            } else {
              log.error("Error while closing command context", exception);
            }

            transactionContext.rollback(); //回滚
          }
        }
        //关流 防止内存泄露
      } catch (Throwable exception) {
        exception(exception);
      } finally {
        closeSessions();

      }
    } catch (Throwable exception) {
      exception(exception);
    } 

    // rethrow the original exception if there was one
    if (exception != null) {
      if (exception instanceof Error) {
        throw (Error) exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw new ActivitiException("exception while executing command " + command, exception);
      }
    }
  }
  
  public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
  	if (closeListeners == null) {
  		closeListeners = new ArrayList<CommandContextCloseListener>(1);
  	}
  	closeListeners.add(commandContextCloseListener);
  }
  
  public List<CommandContextCloseListener> getCloseListeners() {
  	return closeListeners;
  }
  /*
  * 循环 然后 依次flush
  * */
  protected void flushSessions() {
    for (Session session : sessions.values()) {
      session.flush();
    }
  }

  protected void closeSessions() {
    for (Session session : sessions.values()) {
      try {
        session.close();
      } catch (Throwable exception) {
        exception(exception);
      }
    }
  }

  public void exception(Throwable exception) {
    if (this.exception == null) {
      this.exception = exception;
    } else {
      if (Context.isExecutionContextActive()) {
        LogMDC.putMDCExecution(Context.getExecutionContext().getExecution());
      }
    	log.error("masked exception in command context. for root cause, see below as it will be rethrown later.", exception);    	
    	LogMDC.clear();
    }
  }
  
  public void addAttribute(String key, Object value) {
  	if (attributes == null) {
  		attributes = new HashMap<String, Object>(1);
  	}
  	attributes.put(key, value);
  }
  
  public Object getAttribute(String key) {
  	if (attributes != null) {
  		return attributes.get(key);
  	}
  	return null;
  }

  @SuppressWarnings({"unchecked"})
  /*
  * 这里传递的参数值是 DbSqlSession.class
  * */
  public <T> T getSession(Class<T> sessionClass) {
    //根据sessionClass 从 session集合中 获取Session对象
    Session session = sessions.get(sessionClass);
    if (session == null) {
      //从 sessionFactoryies 中获取,  没找到直接报错
      SessionFactory sessionFactory = sessionFactories.get(sessionClass);
      if (sessionFactory==null) {
        throw new ActivitiException("no session factory configured for "+sessionClass.getName());
      }
      //重新打开session并且将其天际到session集合中
      session = sessionFactory.openSession();
      sessions.put(sessionClass, session);
    }

    return (T) session;
  }
  
  public DbSqlSession getDbSqlSession() {
    return getSession(DbSqlSession.class);
  }
  
  public DeploymentEntityManager getDeploymentEntityManager() {
    return getSession(DeploymentEntityManager.class);
  }

  public ResourceEntityManager getResourceEntityManager() {
    return getSession(ResourceEntityManager.class);
  }
  
  public ByteArrayEntityManager getByteArrayEntityManager() {
    return getSession(ByteArrayEntityManager.class);
  }
  
  public ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
    return getSession(ProcessDefinitionEntityManager.class);
  }
  
  public ModelEntityManager getModelEntityManager() {
    return getSession(ModelEntityManager.class);
  }
  
  public ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
    return getSession(ProcessDefinitionInfoEntityManager.class);
  }

  public ExecutionEntityManager getExecutionEntityManager() {
    return getSession(ExecutionEntityManager.class);
  }

  public TaskEntityManager getTaskEntityManager() {
    return getSession(TaskEntityManager.class);
  }

  public IdentityLinkEntityManager getIdentityLinkEntityManager() {
    return getSession(IdentityLinkEntityManager.class);
  }

  public VariableInstanceEntityManager getVariableInstanceEntityManager() {
    return getSession(VariableInstanceEntityManager.class);
  }

  public HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
    return getSession(HistoricProcessInstanceEntityManager.class);
  }

  public HistoricDetailEntityManager getHistoricDetailEntityManager() {
    return getSession(HistoricDetailEntityManager.class);
  }
  
  public HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
    return getSession(HistoricVariableInstanceEntityManager.class);
  }

  public HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
    return getSession(HistoricActivityInstanceEntityManager.class);
  }
  
  public HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
    return getSession(HistoricTaskInstanceEntityManager.class);
  }
  
  public HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
    return getSession(HistoricIdentityLinkEntityManager.class);
  }
  
  public EventLogEntryEntityManager getEventLogEntryEntityManager() {
  	return getSession(EventLogEntryEntityManager.class);
  }
  
  public JobEntityManager getJobEntityManager() {
    return getSession(JobEntityManager.class);
  }

  public UserIdentityManager getUserIdentityManager() {
    return getSession(UserIdentityManager.class);
  }

  public GroupIdentityManager getGroupIdentityManager() {
    return getSession(GroupIdentityManager.class);
  }

  public IdentityInfoEntityManager getIdentityInfoEntityManager() {
    return getSession(IdentityInfoEntityManager.class);
  }

  public MembershipIdentityManager getMembershipIdentityManager() {
    return getSession(MembershipIdentityManager.class);
  }
  
  public AttachmentEntityManager getAttachmentEntityManager() {
    return getSession(AttachmentEntityManager.class);
  }

  public TableDataManager getTableDataManager() {
    return getSession(TableDataManager.class);
  }

  public CommentEntityManager getCommentEntityManager() {
    return getSession(CommentEntityManager.class);
  }
  
  public PropertyEntityManager getPropertyEntityManager() {
    return getSession(PropertyEntityManager.class);
  }
  
  public EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
    return getSession(EventSubscriptionEntityManager.class);
  }

  public Map<Class< ? >, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }

  public HistoryManager getHistoryManager() {
    return getSession(HistoryManager.class);
  }
  
  // getters and setters //////////////////////////////////////////////////////

  public TransactionContext getTransactionContext() {
    return transactionContext;
  }
  public Command< ? > getCommand() {
    return command;
  }
  public Map<Class< ? >, Session> getSessions() {
    return sessions;
  }
  public Throwable getException() {
    return exception;
  }
  public FailedJobCommandFactory getFailedJobCommandFactory() {
    return failedJobCommandFactory;
  }
  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
	  return processEngineConfiguration;
  }
  public ActivitiEventDispatcher getEventDispatcher() {
  	return processEngineConfiguration.getEventDispatcher();
  }
}
