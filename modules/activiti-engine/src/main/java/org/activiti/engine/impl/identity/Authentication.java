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

package org.activiti.engine.impl.identity;


/**
 * @author Tom Baeyens
 * 流程实例的启动人的设置操作必须在流程实例启动之前进行
 * 也可以调用 identityServcie.setAuthenticatedUserId("guorui");
 * 此方法底层调用的就是此类的 这个()  等价的
 */
public abstract class Authentication {
  //使用 authenticatedUserIdThreadLocal 变量维护流程实例的启动人,
  static ThreadLocal<String> authenticatedUserIdThreadLocal = new ThreadLocal<String>();
  
  public static void setAuthenticatedUserId(String authenticatedUserId) {
    authenticatedUserIdThreadLocal.set(authenticatedUserId); //设置流程实例启动人
  }
  //获取流程实例启动人
  public static String getAuthenticatedUserId() {
    return authenticatedUserIdThreadLocal.get();
  }
}
