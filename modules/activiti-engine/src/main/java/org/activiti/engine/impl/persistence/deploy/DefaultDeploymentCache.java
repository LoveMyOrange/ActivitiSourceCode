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
 *  包括使用HashMap 以及LinkedHashMap(LRU 算法的基石)
 *
 *  DefaultDeploymentCache 为泛型类, 如果客户端实例化该类的同时指定了泛型类型, 则该类的成员变量cache
 *  类型已经确定,
 *
 *  如果limit 小于- 或者等于 0  表示 缓存对象的容器大小不受限制,
 *  说明一下容易出错的地方,
 *    如果开发的工作流平台系统是同时让很多外部系统访问使用的,并且每一个系统使用的DB相互独立运行
 *    因此不同系统下的流程文档进行部署时很有可能生成重复的ID值(ACT_RE_PROCDEF的ID字段)
 *
 *    例如A 系统和B系统都存在名为 java:1:1的流程 ,由于Activiti使用Map存储缓存对象
 *    如果key相同则只会加载其中的一个对象到内存中,哪个系统先使用则优先加载,
 *    因此可能出现A系统使用B系统流程的问题,或者B系统使用A系统流程的问题
 *
 *    解决方案:
 *      可以在流程文档部署时,添加租户表示 tenantId 对系统的来源进行区分,
 *      也可以在流程文档定义阶段为流程定义的key值 添加系统标识,从源头上解决问题,这样就可以保证
 *      在全系统中流程定义ID值的唯一性
 *
 *  如果limit参数>0 ,则需要限制 cache容器的 大小为limit参数值
 *  内部LRU 算法 使用LlinkedHashMap  默认就是按照元素的添加顺序进行存储
 *  也支持按照元素的访问顺序 进行存储
 *  也即最新访问的数据 在同期的最前面,
 *
 *  LinkedHashMap 中定义了 判断是否需要移除最老数据的() removeEldestEntyr  该() 默认返回值为fasle 即不移除数据
 *
 *  使用Collections.synchronizedMap 完成线程安全工作 ,
 *  在实例化  LinkedHashMap 同时设置 第三个 输入参数值为 true  即按照访问顺序对元素进行排序
 *  重写了 removeEldestEntyr ()
 *  只要容器中的元素数量> limit参数值 就立刻移除旧数据  从而可以对容器大小进行精准控制
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
