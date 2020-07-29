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

package org.activiti.engine.impl.persistence;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;


/**
 * @author Tom Baeyens
 * 通用实体管理工厂类, 该类负责维护以上所说的之外的 实体管理类, 这些管理类都需要实现Session接口
 */
public class GenericManagerFactory implements SessionFactory {

  protected Class<? extends Session> managerImplementation;
  
  public GenericManagerFactory(Class< ? extends Session> managerImplementation) {
    this.managerImplementation = managerImplementation;
  }

  public Class< ? > getSessionType() {
    return managerImplementation;
  }

  public Session openSession() {
    try {
      return managerImplementation.newInstance(); //反射实例化对象
    } catch (Exception e) {
      throw new ActivitiException("couldn't instantiate "+managerImplementation.getName()+": "+e.getMessage(), e);
    }
  }
}
