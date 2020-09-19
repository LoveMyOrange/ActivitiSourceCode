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
package org.activiti.bpmn.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 * @desc 该类 实现 HasExtensionAttributes   封装了 id,xmlRowNumber,,xmlColumnNumber(元素在XML文件中的坐标信息) 等属性以及克隆BaseElement对象的抽象()
 *   此类是 所有元素属性承载类的父类
 *   由此可知 所有的流程元素都可以扩展, 例如 任务节点,
 *   任务节点的属性 承载类 UserTask 就是BaseElement 类的子类之一
 *
 */
public abstract class BaseElement implements HasExtensionAttributes {

  protected String id;
  protected int xmlRowNumber;
  protected int xmlColumnNumber;
  /*
  负责存储 元素的扩展信息,
  所以自定义元素的信息值 可以通过 BaseElement类来获取
   */
  protected Map<String, List<ExtensionElement>> extensionElements = new LinkedHashMap<String, List<ExtensionElement>>();
  /** extension attributes could be part of each element */
  protected Map<String, List<ExtensionAttribute>> attributes = new LinkedHashMap<String, List<ExtensionAttribute>>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getXmlRowNumber() {
    return xmlRowNumber;
  }

  public void setXmlRowNumber(int xmlRowNumber) {
    this.xmlRowNumber = xmlRowNumber;
  }

  public int getXmlColumnNumber() {
    return xmlColumnNumber;
  }

  public void setXmlColumnNumber(int xmlColumnNumber) {
    this.xmlColumnNumber = xmlColumnNumber;
  }

  public Map<String, List<ExtensionElement>> getExtensionElements() {
    return extensionElements;
  }
  /*
 此() 主要功能就是 :自定义元素的解析结果存储到父级元素中
 此类作为所有元素承载类的父类存在
  由此可知,所有的流程元素都可以扩展,
  例如 任务节点 任务节点的属性承载类, UserTask就是BaseElement的子类之一
   */
  public void addExtensionElement(ExtensionElement extensionElement) {
    if (extensionElement != null && StringUtils.isNotEmpty(extensionElement.getName())) {
      List<ExtensionElement> elementList = null;
      //扩展元素集合可以为多个, 所以List存储
      if (this.extensionElements.containsKey(extensionElement.getName()) == false) {
        //集合初始化
        elementList = new ArrayList<ExtensionElement>();
        this.extensionElements.put(extensionElement.getName(), elementList);
      }
      this.extensionElements.get(extensionElement.getName()).add(extensionElement);
    }
  }

  public void setExtensionElements(Map<String, List<ExtensionElement>> extensionElements) {
    this.extensionElements = extensionElements;
  }

  @Override
  public Map<String, List<ExtensionAttribute>> getAttributes() {
    return attributes;
  }

  @Override
  public String getAttributeValue(String namespace, String name) {
    List<ExtensionAttribute> attributes = getAttributes().get(name);
    if (attributes != null && !attributes.isEmpty()) {
      for (ExtensionAttribute attribute : attributes) {
        if ( (namespace == null && attribute.getNamespace() == null)
            || namespace.equals(attribute.getNamespace()) )
          return attribute.getValue();
      }
    }
    return null;
  }

  @Override
  public void addAttribute(ExtensionAttribute attribute) {
    if (attribute != null && StringUtils.isNotEmpty(attribute.getName())) {
      List<ExtensionAttribute> attributeList = null;
      if (this.attributes.containsKey(attribute.getName()) == false) {
        attributeList = new ArrayList<ExtensionAttribute>();
        this.attributes.put(attribute.getName(), attributeList);
      }
      this.attributes.get(attribute.getName()).add(attribute);
    }
  }

  @Override
  public void setAttributes(Map<String, List<ExtensionAttribute>> attributes) {
    this.attributes = attributes;
  }

  public void setValues(BaseElement otherElement) {
    setId(otherElement.getId());

    extensionElements = new LinkedHashMap<String, List<ExtensionElement>>();
    if (otherElement.getExtensionElements() != null && !otherElement.getExtensionElements().isEmpty()) {
      for (String key : otherElement.getExtensionElements().keySet()) {
        List<ExtensionElement> otherElementList = otherElement.getExtensionElements().get(key);
        if (otherElementList != null && !otherElementList.isEmpty()) {
          List<ExtensionElement> elementList = new ArrayList<ExtensionElement>();
          for (ExtensionElement extensionElement : otherElementList) {
            elementList.add(extensionElement.clone());
          }
          extensionElements.put(key, elementList);
        }
      }
    }

    attributes = new LinkedHashMap<String, List<ExtensionAttribute>>();
    if (otherElement.getAttributes() != null && !otherElement.getAttributes().isEmpty()) {
      for (String key : otherElement.getAttributes().keySet()) {
        List<ExtensionAttribute> otherAttributeList = otherElement.getAttributes().get(key);
        if (otherAttributeList != null && !otherAttributeList.isEmpty()) {
          List<ExtensionAttribute> attributeList = new ArrayList<ExtensionAttribute>();
          for (ExtensionAttribute extensionAttribute : otherAttributeList) {
            attributeList.add(extensionAttribute.clone());
          }
          attributes.put(key, attributeList);
        }
      }
    }
  }

  public abstract BaseElement clone();
}
