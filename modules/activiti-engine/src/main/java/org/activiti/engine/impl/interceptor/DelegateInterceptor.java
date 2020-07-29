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

import org.activiti.engine.impl.delegate.DelegateInvocation;

/**
 * Interceptor responsible for handling calls to 'user code'. User code
 * represents external Java code (e.g. services and listeners) invoked by
 * activiti. The following is a list of classes that represent user code:
 * <ul>
 * <li>{@link org.activiti.engine.delegate.JavaDelegate}</li>
 * <li>{@link org.activiti.engine.delegate.ExecutionListener}</li>
 * <li>{@link org.activiti.engine.delegate.Expression}</li>
 * <li>{@link org.activiti.engine.delegate.TaskListener}</li>
 * </ul>
 * 
 * The interceptor is passed in an instance of {@link DelegateInvocation}.
 * Implementations are responsible for calling
 * {@link DelegateInvocation#proceed()}.
 * 
 * @author Daniel Meyer
 *   任务代理类
 *    既然已经获取到监听器实例对象, 为何不直接触发 监听器中的notify() ???
 *       因为activiti 在触发用户配置监听器中 的notify() 之前, 做了一层全局功能架构
 *       也即使用代理模式 对监听器的访问进行控制
 *       这样设计之后 就可以对所有需要执行的监听器进行拦截, 从而控制监听器是否可以执行,
 *       在实际开发中 可以通过该特性 对废弃的监听器进行屏蔽   delegateinterceptor
 */
public interface DelegateInterceptor {
  /*
  * 该接口定义了 handleInvocation()  拦截以及调用所有需要执行的监听器实例对象
  *  此() 直接 委托 DelegateInvocation 对象中的 proced() 调用监听器,
  *
  * DelegateInvocation 中定义了Proceed() 执行用户自定义监听器, invoke() target()
  * */
  public void handleInvocation(DelegateInvocation invocation) throws Exception;

}


















