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
package org.activiti.engine;


/**
 * Interface implemented by all classes that expose the Activiti services.
 * 
 * @author Joram Barrez
 * @desc 该接口中定义了获取各种服务实例对象的()
 */
public interface EngineServices {

  //操作 流程定义的()      存储 流程图,  web 流程设计器
  RepositoryService getRepositoryService();
  //操作流程实例    启动  挂起,  添加流程变量 等等
  RuntimeService getRuntimeService();
  //操作 流程表单  hi_detail
  FormService getFormService();
  //操作任务   ru_task   hi_taskinst
  TaskService getTaskService();
  //操作历史servie  hi
  HistoryService getHistoryService();
  //操作用户 用户组 act_id
  IdentityService getIdentityService();

  ////高级 ////
  /*
		// 1)用来执行 自定义的命令类
			// 2) 对Activiti内置的job  进行 管理
			// 3) 可以用来执行 自定义的SQL
			//4 )  可以进行查询  act_evt_log表中的数据
			//5)  通过此类得到 具体的 某张表中的  列名, 列值 ,  列类型  表数据量
  * */
  //操作 查询ID表的数据, 表的元数据, 以及 执行命令 等()
  ManagementService getManagementService();
  /*
  *
  * */
  //动态修改
  DynamicBpmnService getDynamicBpmnService();
  //获取流程引擎配置类
  ProcessEngineConfiguration getProcessEngineConfiguration();
}



















