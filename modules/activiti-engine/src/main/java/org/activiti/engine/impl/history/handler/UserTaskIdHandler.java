package org.activiti.engine.impl.history.handler;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.TaskEntity;


/**
 * Called when a task is created for a user-task activity. Allows recoring task-id in
 * historic activity.
 * 
 * @author Frederik Heremans
 * 负责更新 ACT_HI_ACTINST 表中的任务ID 值
 */
public class UserTaskIdHandler implements TaskListener {

  public void notify(DelegateTask task) {
    Context.getCommandContext().getHistoryManager()
      .recordTaskId((TaskEntity) task);
  }
  
}
