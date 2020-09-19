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
package org.activiti.engine.impl.persistence.deploy;


/**
 * Interface for cache implementations.
 * 
 * @author Joram Barrez
 * 缓存接口类
 * 并没有更新缓存的()
 * 原因很简单,
 * 默认实现类 内部使用Map来维护缓存中的数据,
 * 基于Map结构的特性 如果用户期望更新数据, 只需要调用 add() 就可以完成对旧数据的更新 ,
 * 没有必要单独定义更新方法
 * 但是会衍生一个问题
 *  如果客户端不打算使用 DefaultDeploymentCache 那么如何实现 缓存的更新功能呢???
 *
 *  两种思路
 *    1) 定义一个接口 继承 DeploymentCache  在该接口中定义一个 更新缓存的()
 *    虽然 繁琐, 但是 可以保证每一个() 的功能更加单一, 也方便后期维护
 *    2) 自定义一个缓存处理类 并实现  DeploymentCache  在add() 中实现 缓存的添加以及更新逻辑
 *
 *    不推荐使用clear() 清空缓存数据 , 因为是直接把 map 全部清除
 *    最好使用remove() 移除指定的缓存数据
 */
public interface DeploymentCache<T> {
  //获取
  T get(String id);
  //添加
  void add(String id, T object);
  //移除
  void remove(String id);
  //清空
  void clear();
  
}















