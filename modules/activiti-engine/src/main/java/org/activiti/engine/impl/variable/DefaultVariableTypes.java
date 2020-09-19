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
package org.activiti.engine.impl.variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;

/**
 * @author Tom Baeyens
 * 此类负责管理所有的变量的处理类
 * Activiti 如何设计这些变量处理类呢?
 * 所有的变量处理类都需要实现 VariableType 接口
 */
public class DefaultVariableTypes implements VariableTypes, Serializable {

  private static final long serialVersionUID = 1L;

  private final List<VariableType> typesList = new ArrayList<VariableType>();
  private final Map<String, VariableType> typesMap = new HashMap<String, VariableType>();

  public DefaultVariableTypes addType(VariableType type) {
    return addType(type, typesList.size());
  }
  /*
  *  和addType     添加变量处理类
  *
  *
  *
  * */
  public DefaultVariableTypes addType(VariableType type, int index) {
    typesList.add(index, type); //将type对象添加到typesList集合中
    //key是 对象的 getTypeName() 返回值, value为type对象
    typesMap.put(type.getTypeName(), type);//添加到 typesMap中  .map结构
    return this;
  }

  public void setTypesList(List<VariableType> typesList) {
    this.typesList.clear();
    this.typesList.addAll(typesList);
    this.typesMap.clear();
    for (VariableType type : typesList) {
      typesMap.put(type.getTypeName(), type);
    }
  }
/*
* 根据变量类型获取 VariableType
* */
  public VariableType getVariableType(String typeName) {
    return typesMap.get(typeName);
  }
  /*
  * 查找变量处理类
  * 对于 DefaultVariableTypes类中的实现来说
  * 首先遍历typesList 结合
  * 如果查找到了变量处理类, 该()直接返回 ,否则报错
  * 结论:
  *    对于同种类型的变量来说 , 在集合中的位置越靠前  使用的优先级越高
  * */
  public VariableType findVariableType(Object value) {
    for (VariableType type : typesList) {
      if (type.isAbleToStore(value)) {
        return type;
      }
    }
    throw new ActivitiException("couldn't find a variable type that is able to serialize " + value);
  }
/*
* 获取变量处理类在 typeList集合中位置
* */
  public int getTypeIndex(VariableType type) {
    return typesList.indexOf(type);
  }
  /*
  * 获取变量处理类在typeList集合中的位置
  * */
  public int getTypeIndex(String typeName) {
    VariableType type = typesMap.get(typeName);
    if(type != null) {
      return getTypeIndex(type);
    } else {
      return -1;
    }
  }
  //移除变量处理类
  public VariableTypes removeType(VariableType type) {
    typesList.remove(type);
    typesMap.remove(type.getTypeName());
    return this;
  }
}
