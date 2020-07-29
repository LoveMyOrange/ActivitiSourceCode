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

/**
 * @author Tom Baeyens
 * 变量处理类父类
 *
 * ValueFields接口的实现类之一是 VariableInstanceEntity 类 ,该类为 ACT_RU_VARIABLE 表对应的实体类
 */
public interface VariableType {

  /**
   * name of variable type (limited to 100 characters length)
   * 获取变量类型的名称 例如 Boolean类中 该()返回值为boolean
   */
  public String getTypeName();
  
  /**
   * <p>Indicates if this variable type supports caching.</p>
   * <p>If caching is supported, the result of {@link #getValue(ValueFields)} is saved for the 
   *    duration of the session and used for subsequent reads of the variable's value.</p>
   * <p>If caching is not supported, all reads of a variable's value require a 
   *    fresh call to {@link #getValue(ValueFields)}.</p> 
   * @return whether variables of this type are cacheable.
   *
   * 是否支持缓存功能
   */
  boolean isCachable();
  
  /**
   * @return whether this variable type can store the specified value.
   * 变量处理类是否有能力处理指定的变量, 例如BooleanType只负责处理Boolean类型的变量
   */
  boolean isAbleToStore(Object value);
  
  /**
   * Stores the specified value in the supplied {@link ValueFields}.
   * 为valueFields 对象填充值   对应 ACT_RU_VARIABLE 表的 TYPE_ 列
   */
  void setValue(Object value, ValueFields valueFields);
  
  /**
   * @return the value of a variable based on the specified {@link ValueFields}.
   * 为valueFields 对象取值   对应 ACT_RU_VARIABLE 表的 TYPE_ 列
   */
  Object getValue(ValueFields valueFields);

}



















