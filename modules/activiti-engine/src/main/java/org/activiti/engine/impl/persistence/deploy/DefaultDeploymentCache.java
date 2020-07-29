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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default cache: keep everything in memory, unless a limit is set.
 * 
 * @author Joram Barrez
 *  m默认的缓存处理类
 *  内部使用Map结构 维护所有的缓存数据
 *  包括使用HashMap 以及LinkedHashMap(LRU 算法的根)
 *
 *  DefaultDeploymentCache 为泛型类, 如果客户端实例化该类的同时指定了泛型类型, 则该类的成员变量cache
 *  类型已经确定,
 *
 *  如果limit 小于- 或者等于 0  表示 缓存对象的容器大小不受限制,
 *
 *  如果limit参数>0 ,则需要限制 cache容器的 大小为limit参数值
 *  内部LRU 算法 使用LlinkedHashMap  默认就是按照元素的添加顺序进行存储
 *  也支持按照元素的访问熟悉怒 进行存储
 *  也即最新访问的数据 在同期的最前面,
 *
 *  LinkedHashMap 中定义了 判断是否需要移除最老数据的() removeEldestEntyr  该() 默认返回值为fasle 即不移除数据
 *
 *  在实例化  LinkedHashMap 同时设置 第三个 输入参数值为 true  即按照访问顺序对元素进行排序
 *  重写了 removeEldestEntyr ()
 *  只要容器中的元素数量> limit参数值 就like移除旧数据  从而可以对容器大小进行精准控制
 *
 *
 *  因为内部使用的Map  对于客户端而言  缓存数据的添加 获取 更新操作 不方便 而且不灵活
 *  假如系统重启 缓存就会丢失, 所以实际项目开发中
 *  希望使用Redis Memcahe
 *
 */
public class DefaultDeploymentCache<T> implements DeploymentCache<T> {
  
  private static final Logger logger = LoggerFactory.getLogger(DefaultDeploymentCache.class);
  
  protected Map<String, T> cache;//该集合最终存储所有需要缓存的对象
  
  /** Cache with no limit */
  public DefaultDeploymentCache() {
    this(-1);//默认值 -1
  }
  
  /** Cache which has a hard limit: no more elements will be cached than the limit. */
  public DefaultDeploymentCache(final int limit) {
    if (limit > 0) { //判断limit值
      this.cache = Collections.synchronizedMap(new LinkedHashMap<String, T>(limit + 1, 0.75f, true) { // +1 is needed, because the entry is inserted first, before it is removed
                                                                         // 0.75 is the default (see javadocs)
                                                                         // true will keep the 'access-order', which is needed to have a real LRU cache
        private static final long serialVersionUID = 1L;
  
        protected boolean removeEldestEntry(Map.Entry<String, T> eldest) {
          boolean removeEldest = size() > limit; //判断是否可以移除集合中的数据
          if (removeEldest) {
            logger.trace("Cache limit is reached, {} will be evicted",  eldest.getKey());
          }
          return removeEldest;
        }
        
      });
    } else {
      this.cache = Collections.synchronizedMap(new HashMap<String, T>());//初始化cache集合
    }
  }
  
  public T get(String id) {
    return cache.get(id);
  }
  
  public void add(String id, T obj) {
    cache.put(id, obj);
  }
  
  public void remove(String id) {
    cache.remove(id);
  }
  
  public void clear() {
    cache.clear();
  }
  
  // For testing purposes only
  public int size() {
    return cache.size();
  }
  
}
