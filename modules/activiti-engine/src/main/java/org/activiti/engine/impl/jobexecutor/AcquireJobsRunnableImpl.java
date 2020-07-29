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
package org.activiti.engine.impl.jobexecutor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Daniel Meyer
 */
public class AcquireJobsRunnableImpl implements AcquireJobsRunnable {

  private static Logger log = LoggerFactory.getLogger(AcquireJobsRunnableImpl.class);

  protected final JobExecutor jobExecutor;

  protected volatile boolean isInterrupted = false;
  protected volatile boolean isJobAdded = false;
  protected final Object MONITOR = new Object();
  protected final AtomicBoolean isWaiting = new AtomicBoolean(false);
  
  protected long millisToWait = 0;

  public AcquireJobsRunnableImpl(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }
  /*
  AcquireJobsRunnableImpl 因为此类为线程类 所有 只要将   AcquireJobsRunnableImpl 为true
  则run() 会一直执行

   */
  public synchronized void run() {
    log.info("{} starting to acquire jobs", jobExecutor.getName());
    //1)获取命令执行器
    final CommandExecutor commandExecutor = jobExecutor.getCommandExecutor();

    while (!isInterrupted) {
      isJobAdded = false;
      //2)获取当前作业执行器可以完成的作业个数, 该值默认为1
      int maxJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

      try {
        //执行 命令, 并使用acquiredJobs 存储执行命令之后的返回结果
        AcquiredJobs acquiredJobs = commandExecutor.execute(jobExecutor.getAcquireJobsCmd());
        //循环遍历 executeJobs 处理作业
        for (List<String> jobIds : acquiredJobs.getJobIdBatches()) {
          jobExecutor.executeJobs(jobIds);
        }

          // 获取当前线程需要等待的时间 默认为5s
        millisToWait = jobExecutor.getWaitTimeInMillis();
        //获取当前作业执行器 需要处理的作业个数
        int jobsAcquired = acquiredJobs.getJobIdBatches().size();
        // jobsAcquired >= maxJobsPerAcquisition  那么 就需要设置 millisToWait 为0  该操作视为了 确保需要处理的作业都可以被处理
        if (jobsAcquired >= maxJobsPerAcquisition) {
          millisToWait = 0; 
        }

      } catch (ActivitiOptimisticLockingException optimisticLockingException) { 
        // See https://activiti.atlassian.net/browse/ACT-1390
        if (log.isDebugEnabled()) {
          log.debug("Optimistic locking exception during job acquisition. If you have multiple job executors running against the same database, " +
          		"this exception means that this thread tried to acquire a job, which already was acquired by another job executor acquisition thread." +
          		"This is expected behavior in a clustered environment. " +
          		"You can ignore this message if you indeed have multiple job executor acquisition threads running against the same database. " +
          		"Exception message: {}", optimisticLockingException.getMessage());
        }
      } catch (Throwable e) {
        log.error("exception during job acquisition: {}", e.getMessage(), e);          
        millisToWait = jobExecutor.getWaitTimeInMillis();
      }
    // 需要让当前的线程等待一端时间  ,当前线程需要等待的时间为 millisToWait
      if ((millisToWait > 0) && (!isJobAdded)) {
        try {
          if (log.isDebugEnabled()) {
            log.debug("job acquisition thread sleeping for {} millis", millisToWait);
          }
          synchronized (MONITOR) {
            if(!isInterrupted) {
              isWaiting.set(true);
              MONITOR.wait(millisToWait);
            }
          }
          
          if (log.isDebugEnabled()) {
            log.debug("job acquisition thread woke up");
          }
        } catch (InterruptedException e) {
          if (log.isDebugEnabled()) {
            log.debug("job acquisition wait interrupted");
          }
        } finally {
          isWaiting.set(false);
        }
      }
    }
    
    log.info("{} stopped job acquisition", jobExecutor.getName());
  }
  /*

   */
  public void stop() {
    synchronized (MONITOR) {
      isInterrupted = true; //只要 此值 为true 当前线程类 就会停止运行
      if(isWaiting.compareAndSet(true, false)) { 
          MONITOR.notifyAll();
        }
      }
  }
  /*

   */
  public void jobWasAdded() {    
    isJobAdded = true;//表示 已经有需要执行的作业了,   isWaiting() 表示当前线程池 也就是当前类 是否处于 等待执行作业状态  如果为true
    if(isWaiting.compareAndSet(true, false)) { 
      // ensures we only notify once
      // I am OK with the race condition      
      synchronized (MONITOR) {
        MONITOR.notifyAll();// 当此() 被调用之后 JVM 可以选择 任何一个调用了 MONITOR.wait() 线程投入运行,
        //那么如何知道哪些线程 调用了  MONITOR.wait()   看此类的run()

      }
    }    
  }

  
  public long getMillisToWait() {
    return millisToWait;
  }
  
  public void setMillisToWait(long millisToWait) {
    this.millisToWait = millisToWait;
  }
}
