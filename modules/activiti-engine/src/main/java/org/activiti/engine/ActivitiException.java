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
 * Runtime exception that is the superclass of all Activiti exceptions.
 * 
 * @author Tom Baeyens
 *
 * Activiti中的基础异常为org.activiti.engine.ActivitiException，一个非检查异常。
 * 这个异常可以在任何时候被API抛出，不过特定方法抛出的“特定”的异常都记录在 javadocs中。 例如，下面的TaskService：
 *  void complete(String taskId);
 *
 *  在上面的例子中，当传入一个不存在的任务的id时，就会抛出异常。 同时，javadoc明确指出taskId不能为null，如果传入null，
 *  就会抛出ActivitiIllegalArgumentException。
 *  我们希望避免过多的异常继承，下面的子类用于特定的场合。 流程引擎和API调用的其他场合不会使用下面的异常，
 *  它们会抛出一个普通的ActivitiExceptions。
 *
 *
 *  ActivitiWrongDbException：当Activiti引擎发现数据库版本号和引擎版本号不一致时抛出。
 *
 * ActivitiOptimisticLockingException：对同一数据进行并发方法并出现乐观锁时抛出。
 *
 * ActivitiClassLoadingException：当无法找到需要加载的类或在加载类时出现了错误（比如，JavaDelegate，TaskListener等。
 *
 * ActivitiObjectNotFoundException：当请求或操作的对应不存在时抛出。
 *
 * ActivitiIllegalArgumentException：这个异常表示调用Activiti API时传入了一个非法的参数，可能是引擎配置中的非法值，或提供了一个非法制，或流程定义中使用的非法值。
 *
 * ActivitiTaskAlreadyClaimedException：当任务已经被认领了，再调用taskService.claim(...)就会抛出。
 */
public class ActivitiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ActivitiException(String message, Throwable cause) {
    super(message, cause);
  }

  public ActivitiException(String message) {
    super(message);
  }
}






