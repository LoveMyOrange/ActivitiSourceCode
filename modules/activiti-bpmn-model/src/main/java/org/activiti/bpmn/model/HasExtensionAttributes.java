package org.activiti.bpmn.model;

import java.util.List;
import java.util.Map;

/**
 * interface for accessing Element attributes.
 *
 * @author Martin Grofcik
 * @ 该接口定义了用户自定义属性的存储和获取()
 * 因为所有的元素属性承载类都实现了该接口, 所以可以大胆的推测所有的元素都是可以扩展的
 */
public interface HasExtensionAttributes {
  /** get element's attributes */
  Map<String, List<ExtensionAttribute>> getAttributes();

  /**
   * return value of the attribute from given namespace with given name.
   *
   * @param namespace
   * @param name
   * @return attribute value or null in case when attribute was not found
   */
  String getAttributeValue(String namespace, String name);

  /** add attribute to the object */
  void addAttribute(ExtensionAttribute attribute);

  /** set all object's attributes */
  void setAttributes(Map<String, List<ExtensionAttribute>> attributes);
}
