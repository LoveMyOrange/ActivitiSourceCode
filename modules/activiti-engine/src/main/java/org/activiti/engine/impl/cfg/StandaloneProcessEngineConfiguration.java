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

package org.activiti.engine.impl.cfg;

import org.activiti.engine.impl.interceptor.CommandInterceptor;

/**
 * @author Tom Baeyens
 * @desc 标准的流程引擎配置类
 * 单独运行的流程引擎。Activiti会自己处理事务。 默认，数据库只在引擎启动时检测
 * （如果没有Activiti的表或者表结构不正确就会抛出异常）。
 */
public class StandaloneProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

  @Override
  protected CommandInterceptor createTransactionInterceptor() {
    return null;
  }
  
}
