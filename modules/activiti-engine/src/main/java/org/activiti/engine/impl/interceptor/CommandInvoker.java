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

import org.activiti.engine.impl.context.Context;

/**
 * @author Tom Baeyens
 * 命令调度者 当程序执行到该类时 ,开始调度命令类的执行工作
 *
 * 因为此类 是拦截器链中的最后一个节点
 * 该类中不能设置下一个处理类 并且 getNext()永远为null
 *
 */
public class CommandInvoker extends AbstractCommandInterceptor {
  /*
  *  命令拦截器 是如何被 CommandInvoker 调用执行的呢 ????
  * */
  @Override
  public <T> T execute(CommandConfig config, Command<T> command) {

    /*
    首先获取CommandContext 对象  ,然后低啊用command.execute()

     */
    return command.execute(Context.getCommandContext());
  }

  @Override
  public CommandInterceptor getNext() {
    return null;
  }

  @Override
  public void setNext(CommandInterceptor next) {
    throw new UnsupportedOperationException("CommandInvoker must be the last interceptor in the chain");
  }

}














