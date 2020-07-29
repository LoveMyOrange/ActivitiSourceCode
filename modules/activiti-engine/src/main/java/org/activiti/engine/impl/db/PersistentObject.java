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
package org.activiti.engine.impl.db;


/**
 * @author Tom Baeyens
 * 开发人员只需要组装   PersistentObject 对象 并将其交给流程引擎,
 * 流程引擎根据该实例对象查找其管理类
 * 然后通过管理类执行相应的操作
 * 最终调用 SqlSession 实例对象 完成数据的持久化操作
 *
 * 所有的Activiti 实体类 都  需要实现该类
 * 该类主要用来封装 业务数据,
 * 那么 Activiti 是如何调度这些实体类 进行数据的持久化操作呢  ???
 *   由于Activiti表非常多, (也就是映射文件需要操作的类) 非常庞大 , 因此Actiiti引入了 实体管理类,
 *   通常情况下 实体管理类与实体类是 一一对应的,,
 *
 *   这样 既可分散每个实体管理类的职责, 又可以对所有的实体管理类进行一次全局架构
 *
 *
 *   所有的实体管理类都  继承 AbstractManager 类
 *   该类定义了 本对象的 insert.delet等等
 *   为何 AbstractManager 类中没有定义 查询和 更新操作呢 ????
 */
public interface PersistentObject {
  //得到DB表的主键值 (不包括特殊的表)
  String getId();
  void setId(String id);

  /**
   * Returns a representation of the object,
   *  as would be stored in the database.
   * Used when deciding if updates have
   *  occurred to the object or not since
   *  it was last loaded.
   *
   *  会话缓存技术
   */
  Object getPersistentState();
}

















