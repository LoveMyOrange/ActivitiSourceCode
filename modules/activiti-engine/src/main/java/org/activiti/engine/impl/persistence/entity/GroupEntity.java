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
package org.activiti.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;


/**
 * @author Tom Baeyens
 */
public class GroupEntity implements Group, Serializable, PersistentObject, HasRevision {

  private static final long serialVersionUID = 1L;

  protected String id;
  /*
  * identityService.saveGroup() 会判断Group的revsion属性(对应ACT_ID_GROUP)的REV_字段
  * 是否为0.版本号为0则做新增处理,反之则做修改处理,Group接口并不提供设置和获取revision的()
  * 因此该属性的变化,我们不需要关心
  * */
  protected int revision;
  protected String name;
  protected String type;
  
  public GroupEntity() {
  }
  
  public GroupEntity(String id) {
    this.id = id;
  }
  /*
  * 假如客户端A 发送了一个命令 将 GroupEntity对象 添加到DB
  * 该命令经过了一系列的命令拦截器链之后 命令开始原路返回,, 并且内存中已经存储了 GroupEntity对象 (将其称之为原值
  * 当执行到命令拦截器之前 程序还没有开始进行 flushSessions() 动作时,
  *
  * 客户端A 或者 客户端B 对 GroupEntity 中的属性值进行了修改,
  *
  * 那么当客户端A 执行 flushSession() 时   因为GroupEntity对象中的属性值与原值不一致 则会触发更新操作
  *
  *
  * 假如同样的修改操作
  * 客户端B修改了对象的id值 , 思考一下会发生更新操作吗???
  * 不会,为什么??
  *  因为GroupEntity 类中的 getPersistentState() 返回对象中 ,并没有对id值进行定义
  * 因此修改id(getPersistentState()中之前没有定义的属性值 ) 不会触发更新操作
  *
  * 更新操作判断的核心点 只在于 关心实体类中 getPersistentState() 所定义的属性, 其他属性是否变化一概不理会,
  *
  * 容易出现2个缺陷
  * 1) 例如 对于ID   (通常对应于DB中的主键,  如果Activiti运行在一个高并发的场景中, 那么 就有可能出现问题
  * 又如客户端A生成了一个主键ID 值, 程序还没有来得及进行flushSessions操作,
  * 这时客户端B 又需要生成一个主键id值 ,这个值就有可能占用了 ,
  * 由于DB中的主键值不能相同,  由于DB中的主键值不能相同,因此通常情况下 ,并发量大的长泾镇 主键id建议使用 UUID
  * 2) 会话缓存中的数据在没有刷新到BD之前,如果系统宕机, 则会话缓存中的数据丢失,
  * 因此使用该功能有风险, 因为引擎并没有提供容错机制
  *
  *
  * 通过上面分析 可知 只有 Persistentobject 接口中 getPersistentState() 定义的属性值发生了变化
  * Activiti 才回去更新对象, 这对于客户端来说 ,可能有点不灵活
  * Activiti 也提供了 update(String statement, Object parameters)  可以直接通过SqlSession 对象更新数据
  *
  * Activiti自身会话缓存机制设计的很简单, 性能稳定,但是有局限性 ,不适用于复杂场景,
  * 在实际开发中, 如果比较复杂,可以结合MyBatis中的缓存机制使用
  *
  * */
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("name", name);
    persistentState.put("type", type);
    return persistentState;
  }
  
  public int getRevisionNext() {
    return revision+1;
  }

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public int getRevision() {
    return revision;
  }
  public void setRevision(int revision) {
    this.revision = revision;
  }
}
