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

import java.util.Arrays;

import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;


/**
 * @author Tom Baeyens
 */
public class DeserializedObject {
  SerializableType type;
  Object deserializedObject;
  byte[] originalBytes;
  VariableInstanceEntity variableInstanceEntity;

  public DeserializedObject(SerializableType type, Object deserializedObject, byte[] serializedBytes, VariableInstanceEntity variableInstanceEntity) {
    this.type = type;
    this.deserializedObject = deserializedObject;
    this.originalBytes = serializedBytes;
    this.variableInstanceEntity = variableInstanceEntity;
  }
  /*
  *
  * */
  public void flush() {
    // this first check verifies if the variable value was not overwritten with another object
    /*
    * 判断 deserializedObject 对象 与 variableInstanceEntity.getCachedValue()的返回值是否相等
    * 并且  variableInstanceEntity.isDeleted() 返回值为false
    * 该判断操作主要是为了确保变量值能否被修改或者被删除
    * 如果变量值被修改了 ,但是还没有删除
    *
    * 序列化类型的流程变量最终会存储到 ACT_GE_BYTEARRAY表 以及 act_ru_variable表中
    * */
    if (deserializedObject == variableInstanceEntity.getCachedValue() && !variableInstanceEntity.isDeleted()) {
      byte[] bytes = type.serialize(deserializedObject, variableInstanceEntity);//
      if (!Arrays.equals(originalBytes, bytes)) {
        
        // Add an additional check to prevent byte differences due to JDK changes etc
        Object originalObject = type.deserialize(originalBytes, variableInstanceEntity); // 转化为byte数组
        byte[] refreshedOriginalBytes = type.serialize(originalObject, variableInstanceEntity);
        
        if (!Arrays.equals(refreshedOriginalBytes, bytes)) { // 如果 refreshedOriginalBytes 和byte 不相等
          //说明变量已经被修改了, 那么执行代码将变量值刷新到会话缓存中
          variableInstanceEntity.setBytes(bytes);
        }
      }
    }
  }
}





















