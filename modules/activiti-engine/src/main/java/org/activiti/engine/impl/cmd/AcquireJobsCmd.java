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
package org.activiti.engine.impl.cmd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.MessageEntity;


/**
 * @author Nick Burch
 * @author Daniel Meyer
 */
public class AcquireJobsCmd implements Command<AcquiredJobs> {

  private final JobExecutor jobExecutor;

  public AcquireJobsCmd(JobExecutor jobExecutor) {
    this.jobExecutor = jobExecutor;
  }
  
  public AcquiredJobs execute(CommandContext commandContext) {
    //获取作业的锁定时间 默认为5min
    String lockOwner = jobExecutor.getLockOwner();
    int lockTimeInMillis = jobExecutor.getLockTimeInMillis();
    int maxNonExclusiveJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

    //该类记录所有可以执行的作业Id ZHI1
    AcquiredJobs acquiredJobs = new AcquiredJobs();
    // 从act_ru_job表中 查询所有可以执行的作业,
    List<JobEntity> jobs = commandContext
      .getJobEntityManager()
      .findNextJobsToExecute(new Page(0, maxNonExclusiveJobsPerAcquisition));

    for (JobEntity job: jobs) {
      List<String> jobIds = new ArrayList<String>();
      if (job != null && !acquiredJobs.contains(job.getId())) {
        //判断job是否是独占模式  最后判断 job的 流程实例id 是否为空 ,如果都是true,
        if (job instanceof MessageEntity && job.isExclusive() && job.getProcessInstanceId() != null) {
          // wait to get exclusive jobs within 100ms
          try {

            Thread.sleep(100); //休眠100ms
          } catch (InterruptedException e) {}
          
          // acquire all exclusive jobs in the same process instance
          // (includes the current job)
          List<JobEntity> exclusiveJobs = commandContext.getJobEntityManager()
            .findExclusiveJobsToExecute(job.getProcessInstanceId());
          for (JobEntity exclusiveJob : exclusiveJobs) {
            if (exclusiveJob != null) {
              //该() 主要是为job对象填充属性
              lockJob(commandContext, exclusiveJob, lockOwner, lockTimeInMillis);
              jobIds.add(exclusiveJob.getId());
            }
          }
          
        } else {
          lockJob(commandContext, job, lockOwner, lockTimeInMillis);
          jobIds.add(job.getId());
        }
        
      }

      acquiredJobs.addJobIdBatch(jobIds);
    }

    return acquiredJobs;
  }

  protected void lockJob(CommandContext commandContext, JobEntity job, String lockOwner, int lockTimeInMillis) {    
    job.setLockOwner(lockOwner);
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
    gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
    job.setLockExpirationTime(gregorianCalendar.getTime());    
  }
}