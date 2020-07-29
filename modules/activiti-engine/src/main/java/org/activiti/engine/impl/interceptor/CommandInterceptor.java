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

/**
 * @author Tom Baeyens
 * 命令拦截器
 * 在命令类执行之前 进行拦截器
 * 一个命令 可以有多个拦截器
 * 这一系列的拦截器最终构造成拦截器链 然后 依次调用
 */
public interface CommandInterceptor {

  <T> T execute(CommandConfig config, Command<T> command);
 
  CommandInterceptor getNext();

  void setNext(CommandInterceptor next);

}
