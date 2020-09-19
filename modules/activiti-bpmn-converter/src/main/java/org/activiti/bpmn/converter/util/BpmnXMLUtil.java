package org.activiti.bpmn.converter.util;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.child.ActivitiEventListenerParser;
import org.activiti.bpmn.converter.child.ActivitiFailedjobRetryParser;
import org.activiti.bpmn.converter.child.ActivitiMapExceptionParser;
import org.activiti.bpmn.converter.child.BaseChildElementParser;
import org.activiti.bpmn.converter.child.CancelEventDefinitionParser;
import org.activiti.bpmn.converter.child.CompensateEventDefinitionParser;
import org.activiti.bpmn.converter.child.ConditionExpressionParser;
import org.activiti.bpmn.converter.child.DataInputAssociationParser;
import org.activiti.bpmn.converter.child.DataOutputAssociationParser;
import org.activiti.bpmn.converter.child.DataStateParser;
import org.activiti.bpmn.converter.child.DocumentationParser;
import org.activiti.bpmn.converter.child.ErrorEventDefinitionParser;
import org.activiti.bpmn.converter.child.ExecutionListenerParser;
import org.activiti.bpmn.converter.child.FieldExtensionParser;
import org.activiti.bpmn.converter.child.FlowNodeRefParser;
import org.activiti.bpmn.converter.child.FormPropertyParser;
import org.activiti.bpmn.converter.child.IOSpecificationParser;
import org.activiti.bpmn.converter.child.MessageEventDefinitionParser;
import org.activiti.bpmn.converter.child.MultiInstanceParser;
import org.activiti.bpmn.converter.child.SignalEventDefinitionParser;
import org.activiti.bpmn.converter.child.TaskListenerParser;
import org.activiti.bpmn.converter.child.TerminateEventDefinitionParser;
import org.activiti.bpmn.converter.child.TimeCycleParser;
import org.activiti.bpmn.converter.child.TimeDateParser;
import org.activiti.bpmn.converter.child.TimeDurationParser;
import org.activiti.bpmn.converter.child.TimerEventDefinitionParser;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionAttribute;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.apache.commons.lang3.StringUtils;
/*
该类在被JVM加载时 会先触发该类中的静态代码块


 */
public class BpmnXMLUtil implements BpmnXMLConstants {
  /*
  key为流程文档中对应的子元素名称
  value为子元素对应的解析器
   */
  private static Map<String, BaseChildElementParser> genericChildParserMap = new HashMap<String, BaseChildElementParser>();
  /*
  该静态代码块 会加载所有通用的子元素解析器, 只会加载一次 ,
  所说的通用资源都指的是 流程元素中的 子元素 信息
  例如 所有的 流程元素都有文档描述元素  documentation  ,extensionElements  (可以自定义执行监听器 等)
  条件表达式(conditionExpression)
  多实例 (multiInstanceLoopCharacteristics )
  因为这些子元素比较通用 ,所以 单独 抽取出来, 进行定义和解析, 降低维护成本

  所有的子元素 解析器 都需要继承 BaseChildElementParser 类

  会涉及到addGenericParse()
  因为 该()  是 私有静态()
  也就意味着  客户端 不能通过该() 添加自定义子元素解析器,
  也不能入侵 和干预子元素的解析工作,
  有没有其他办法  替换 默认子元素解析器???

   */
  static {
    addGenericParser(new ActivitiEventListenerParser());
    //元素解析器
    addGenericParser(new CancelEventDefinitionParser());
    addGenericParser(new CompensateEventDefinitionParser());
    addGenericParser(new ConditionExpressionParser());
    addGenericParser(new DataInputAssociationParser());
    addGenericParser(new DataOutputAssociationParser());
    addGenericParser(new DataStateParser());
    addGenericParser(new DocumentationParser());
    addGenericParser(new ErrorEventDefinitionParser());
    addGenericParser(new ExecutionListenerParser());
    addGenericParser(new FieldExtensionParser());
    addGenericParser(new FormPropertyParser());
    addGenericParser(new IOSpecificationParser());
    addGenericParser(new MessageEventDefinitionParser());
    addGenericParser(new MultiInstanceParser());
    addGenericParser(new SignalEventDefinitionParser());
    addGenericParser(new TaskListenerParser());
    addGenericParser(new TerminateEventDefinitionParser());
    addGenericParser(new TimerEventDefinitionParser());
    addGenericParser(new TimeDateParser());
    addGenericParser(new TimeCycleParser());
    addGenericParser(new TimeDurationParser());
    addGenericParser(new FlowNodeRefParser());
    addGenericParser(new ActivitiFailedjobRetryParser());
    addGenericParser(new ActivitiMapExceptionParser());
  }
  
  private static void addGenericParser(BaseChildElementParser parser) {
    genericChildParserMap.put(parser.getElementName(), parser);
  }
  /*
  获取元素坐标
  获取连线元素再XML文档中的行号和列号信息
  因为此() 是静态() ,所以该() 被调用的同时 会触发 该类的静态代码块
   */
  public static void addXMLLocation(BaseElement element, XMLStreamReader xtr) {
    Location location = xtr.getLocation(); //获取到Location对象
    element.setXmlRowNumber(location.getLineNumber());//得到行号
    element.setXmlColumnNumber(location.getColumnNumber());//列号
    //获取完毕之后 赋值到BaseElement 对象中 (对于连线来说 该对象为 sequenceFlow )
  }
  
  public static void addXMLLocation(GraphicInfo graphicInfo, XMLStreamReader xtr) {
    Location location = xtr.getLocation();
    graphicInfo.setXmlRowNumber(location.getLineNumber());
    graphicInfo.setXmlColumnNumber(location.getColumnNumber());
  }
  
  public static void parseChildElements(String elementName, BaseElement parentElement, XMLStreamReader xtr, BpmnModel model) throws Exception {
    parseChildElements(elementName, parentElement, xtr, null, model); 
  }
  /*
  解析 子元素
   */
  public static void parseChildElements(String elementName, BaseElement parentElement, XMLStreamReader xtr, 
      Map<String, BaseChildElementParser> childParsers, BpmnModel model) throws Exception {
    /*
    该集合的初始化工作 分为两步
    1)获取内置子元素 解析器 集合
    genericChildParserMap 的值 添加到 localParserMap 中
    genericChildParserMap 集合的初始化过程 可以参考该类的静态代码块
    2)获取childParsers  参数值 ,如果该参数值 不为空 ,则将该值 添加到子元素解析器 集合  localarserMap中
     因为 是Map 结构
     所以 用户自定义的子元素解析器, 可以覆盖, Activiti 默认的子元素解析器
     该参数值的 有无非常重要
     直接影响 子元素解析 时 所需要使用的解析器
     */
    Map<String, BaseChildElementParser> localParserMap =
        new HashMap<String, BaseChildElementParser>(genericChildParserMap);  //初始化  子元素解析器 集合
    if (childParsers != null) {
      localParserMap.putAll(childParsers);
    }

    boolean inExtensionElements = false;
    boolean readyWithChildElements = false;
    while (readyWithChildElements == false && xtr.hasNext()) {
      xtr.next(); //移动读取器游标
      if (xtr.isStartElement()) { //开始元素
        if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
          inExtensionElements = true;//如果是扩展元素 设置该变量值为true
        } else if (localParserMap.containsKey(xtr.getLocalName())) {
          BaseChildElementParser childParser = localParserMap.get(xtr.getLocalName()); //从通用子元素集合中获取查找
          //if we're into an extension element but the current element is not accepted by this parentElement then is read as a custom extension element
          /*
           如果当前元素是 extensionElememnt 元素的子元素      &&  扩展元素的判断
           accepts() 至关重要, 如果父类不能识别文档中定义的子元素, 那么引擎就认为这个元素是
           扩展元素 然后对其进行处理
           此()的处理过程 ,可以参考  FieldExtensionParser  或者 FormPropertyParser类的实现
           */
          if (inExtensionElements && !childParser.accepts(parentElement)) {
          /*
          开始解析用户自定义扩展元素

          子元素的解析可以分为 通用子元素解析  如 documentation  元素 和用户自定义扩展元素解析

            处理逻辑为 :
            如果经过判断 发现 子元素是 通用子元素 ,然后开始解析通用子元素,


           */
            ExtensionElement extensionElement = BpmnXMLUtil.parseExtensionElement(xtr);
            //将解析之后的元素对象  添加到 父级对象中
            parentElement.addExtensionElement(extensionElement);
            continue;
          }
          //开始解析 通用子元素   根据子元素的名称 获取子元素解析器进行子元素的解析工作
          localParserMap.get(xtr.getLocalName()).parseChildElement(xtr, parentElement, model);
        } else if (inExtensionElements) { //如果是用户自定义扩展元素
          ExtensionElement extensionElement = BpmnXMLUtil.parseExtensionElement(xtr);
          /*
          不管是解析何种类型的子元素,都需要将元素解析之后的结果添加到父元素 parentElement中
          那么  parentElement 是哪个对象呢???
            首先:  parseChildElements() 是由哪一个对象调用的呢??
            很显然该() 由具体解析类的实例对象进行调用
            比如 现在开始解析任务节点, 则任务节点的解析类 UserTaskXMLConverter 解析任务节点时
            会调用parseChildElements() 进行子元素的解析工作,
            这时 parentElement 对象就对应 UserTaskXMLConverter 对象

           */
          parentElement.addExtensionElement(extensionElement);
        }

      } else if (xtr.isEndElement()) {
        if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
          inExtensionElements = false;
        } else if (elementName.equalsIgnoreCase(xtr.getLocalName())) {
          readyWithChildElements = true;
        }
      }
    }
  }
  /*
  自定义元素解析原理
  自定义元素的属性个数不限, 定义多少个 就解析多少个
  最后 会通过
  parentElement.addExtensionElement(extensionElement);  将自定义元素的解析结果存储到父级元素中
  addExtensionElement() 位于BaseElement中 ,此类作为所有元素承载类的父类存在
  由此可知,所有的流程元素都可以扩展,
  例如 任务节点 任务节点的属性承载类, UserTask就是BaseElement的子类之一


   */
  public static ExtensionElement parseExtensionElement(XMLStreamReader xtr) throws Exception {
    ExtensionElement extensionElement = new ExtensionElement(); //扩展元素承载类
    extensionElement.setName(xtr.getLocalName()); //获取元素名称
    if (StringUtils.isNotEmpty(xtr.getNamespaceURI())) {
      extensionElement.setNamespace(xtr.getNamespaceURI()); //命名空间
    }
    if (StringUtils.isNotEmpty(xtr.getPrefix())) {
      extensionElement.setNamespacePrefix(xtr.getPrefix()); //元素命名空间前缀
    }
    
    for (int i = 0; i < xtr.getAttributeCount(); i++) {//遍历元素的属性值
      ExtensionAttribute extensionAttribute = new ExtensionAttribute(); //  元素属性承载类
      extensionAttribute.setName(xtr.getAttributeLocalName(i));//属性名称
      extensionAttribute.setValue(xtr.getAttributeValue(i));//属性值
      if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {
        extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));  //属性命名空间
      }
      if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
        //属性的命名空间前缀
        extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i));
      }
      //将extensionAttribute 设置到 extensionElement 中
      extensionElement.addAttribute(extensionAttribute);
    }
    
    boolean readyWithExtensionElement = false;
    while (readyWithExtensionElement == false && xtr.hasNext()) {
      xtr.next();
      if (xtr.isCharacters() || XMLStreamReader.CDATA == xtr.getEventType()) {
        if (StringUtils.isNotEmpty(xtr.getText().trim())) {
          extensionElement.setElementText(xtr.getText().trim()); //获取元素内容
        }
      } else if (xtr.isStartElement()) {
        //如果自定义元素中存在子元素 需要解析子元素并且将其解析结果添加到父类中
        ExtensionElement childExtensionElement = parseExtensionElement(xtr);
        extensionElement.addChildElement(childExtensionElement);
      } else if (xtr.isEndElement() && extensionElement.getName().equalsIgnoreCase(xtr.getLocalName())) {
        readyWithExtensionElement = true;
      }
    }
    return extensionElement;
  }
  
  public static void writeDefaultAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
    if (StringUtils.isNotEmpty(value) && "null".equalsIgnoreCase(value) == false) {
      xtw.writeAttribute(attributeName, value);
    }
  }
  
  public static void writeQualifiedAttribute(String attributeName, String value, XMLStreamWriter xtw) throws Exception {
    if (StringUtils.isNotEmpty(value)) {
      xtw.writeAttribute(ACTIVITI_EXTENSIONS_PREFIX, ACTIVITI_EXTENSIONS_NAMESPACE, attributeName, value);
    }
  }
  
  public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
    return didWriteExtensionStartElement = writeExtensionElements(baseElement, didWriteExtensionStartElement, null, xtw);
  }
 
  public static boolean writeExtensionElements(BaseElement baseElement, boolean didWriteExtensionStartElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
    if (!baseElement.getExtensionElements().isEmpty()) {
      if (didWriteExtensionStartElement == false) {
        xtw.writeStartElement(ELEMENT_EXTENSIONS);
        didWriteExtensionStartElement = true;
      }
      
      if (namespaceMap == null) {
        namespaceMap = new HashMap<String, String>();
      }
      
      for (List<ExtensionElement> extensionElements : baseElement.getExtensionElements().values()) {
        for (ExtensionElement extensionElement : extensionElements) {
          writeExtensionElement(extensionElement, namespaceMap, xtw);
        }
      }
    }
    return didWriteExtensionStartElement;
  }
  
  protected static void writeExtensionElement(ExtensionElement extensionElement, Map<String, String> namespaceMap, XMLStreamWriter xtw) throws Exception {
    if (StringUtils.isNotEmpty(extensionElement.getName())) {
      Map<String, String> localNamespaceMap = new HashMap<String, String>();
      if (StringUtils.isNotEmpty(extensionElement.getNamespace())) {
        if (StringUtils.isNotEmpty(extensionElement.getNamespacePrefix())) {
          xtw.writeStartElement(extensionElement.getNamespacePrefix(), extensionElement.getName(), extensionElement.getNamespace());
          
          if (namespaceMap.containsKey(extensionElement.getNamespacePrefix()) == false ||
              namespaceMap.get(extensionElement.getNamespacePrefix()).equals(extensionElement.getNamespace()) == false) {
            
            xtw.writeNamespace(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
            namespaceMap.put(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
            localNamespaceMap.put(extensionElement.getNamespacePrefix(), extensionElement.getNamespace());
          }
        } else {
          xtw.writeStartElement(extensionElement.getNamespace(), extensionElement.getName());
        }
      } else {
        xtw.writeStartElement(extensionElement.getName());
      }
      
      for (List<ExtensionAttribute> attributes : extensionElement.getAttributes().values()) {
        for (ExtensionAttribute attribute : attributes) {
          if (StringUtils.isNotEmpty(attribute.getName()) && attribute.getValue() != null) {
            if (StringUtils.isNotEmpty(attribute.getNamespace())) {
              if (StringUtils.isNotEmpty(attribute.getNamespacePrefix())) {
                
                if (namespaceMap.containsKey(attribute.getNamespacePrefix()) == false ||
                    namespaceMap.get(attribute.getNamespacePrefix()).equals(attribute.getNamespace()) == false) {
                  
                  xtw.writeNamespace(attribute.getNamespacePrefix(), attribute.getNamespace());
                  namespaceMap.put(attribute.getNamespacePrefix(), attribute.getNamespace());
                }
                
                xtw.writeAttribute(attribute.getNamespacePrefix(), attribute.getNamespace(), attribute.getName(), attribute.getValue());
              } else {
                xtw.writeAttribute(attribute.getNamespace(), attribute.getName(), attribute.getValue());
              }
            } else {
              xtw.writeAttribute(attribute.getName(), attribute.getValue());
            }
          }
        }
      }
      
      if (extensionElement.getElementText() != null) {
        xtw.writeCData(extensionElement.getElementText());
      } else {
        for (List<ExtensionElement> childElements : extensionElement.getChildElements().values()) {
          for (ExtensionElement childElement : childElements) {
            writeExtensionElement(childElement, namespaceMap, xtw);
          }
        }
      }
      
      for (String prefix : localNamespaceMap.keySet()) {
        namespaceMap.remove(prefix);
      }
      
      xtw.writeEndElement();
    }
  }
  
  public static List<String> parseDelimitedList(String s) {
    List<String> result = new ArrayList<String>();
    if (StringUtils.isNotEmpty(s)) {

      StringCharacterIterator iterator = new StringCharacterIterator(s);
      char c = iterator.first();

      StringBuilder strb = new StringBuilder();
      boolean insideExpression = false;

      while (c != StringCharacterIterator.DONE) {
        if (c == '{' || c == '$') {
          insideExpression = true;
        } else if (c == '}') {
          insideExpression = false;
        } else if (c == ',' && !insideExpression) {
          result.add(strb.toString().trim());
          strb.delete(0, strb.length());
        }

        if (c != ',' || (insideExpression)) {
          strb.append(c);
        }

        c = iterator.next();
      }

      if (strb.length() > 0) {
        result.add(strb.toString().trim());
      }

    }
    return result;
  }
  
  public static String convertToDelimitedString(List<String> stringList) {
    StringBuilder resultString = new StringBuilder();
    
    if(stringList != null) {
    	for (String result : stringList) {
    		if (resultString.length() > 0) {
    			resultString.append(",");
    		}
    		resultString.append(result);
    	}
    }
    return resultString.toString();
  }

  /**
   * add all attributes from XML to element extensionAttributes (except blackListed).
   *
   * @param xtr
   * @param element
   * @param blackLists
   *
   * 黑名单 处理机制
   *     以UserTask为例
   *      从UserTaskXMLConverter 的 convertToXMLElement()
   *
   *      此() 首先解析任务节点中定义的常规属性,
   *      然后 调用本()   解析自定义属性,
   *
   *
   */
  public static void addCustomAttributes(XMLStreamReader xtr, BaseElement element, List<ExtensionAttribute>... blackLists) {
    for (int i = 0; i < xtr.getAttributeCount(); i++) {
      ExtensionAttribute extensionAttribute = new ExtensionAttribute();//扩展属性
      extensionAttribute.setName(xtr.getAttributeLocalName(i));//名称
      extensionAttribute.setValue(xtr.getAttributeValue(i));//值
      if (StringUtils.isNotEmpty(xtr.getAttributeNamespace(i))) {//命名空间
        extensionAttribute.setNamespace(xtr.getAttributeNamespace(i));
      }
      if (StringUtils.isNotEmpty(xtr.getAttributePrefix(i))) {
        extensionAttribute.setNamespacePrefix(xtr.getAttributePrefix(i)); //命名空间前缀
      }
      //elment为UserTask
      /*
      如果属性不在黑名单列表中  则将其作为扩展属性信息  进行存储

       */
      if (!isBlacklisted(extensionAttribute, blackLists)) { //黑名单校验
        element.addAttribute(extensionAttribute);
      }
    }
  }

  public static void writeCustomAttributes(Collection<List<ExtensionAttribute>> attributes, XMLStreamWriter xtw, List<ExtensionAttribute>... blackLists) throws XMLStreamException {
    writeCustomAttributes(attributes, xtw, new LinkedHashMap<String, String>(), blackLists);
  }
  
  /**
   * write attributes to xtw (except blacklisted)
   * @param attributes
   * @param xtw
   * @param blackLists
   */
  public static void writeCustomAttributes(Collection<List<ExtensionAttribute>> attributes, XMLStreamWriter xtw, Map<String, String> namespaceMap,
      List<ExtensionAttribute>... blackLists) throws XMLStreamException {
    
    for (List<ExtensionAttribute> attributeList : attributes) {
      if (attributeList != null && !attributeList.isEmpty()) {
        for (ExtensionAttribute attribute : attributeList) {
          if (!isBlacklisted(attribute, blackLists)) {
            if (attribute.getNamespacePrefix() == null) {
              if (attribute.getNamespace() == null)
                xtw.writeAttribute(attribute.getName(), attribute.getValue());
              else {
                xtw.writeAttribute(attribute.getNamespace(), attribute.getName(), attribute.getValue());
              }
            } else {
              if (!namespaceMap.containsKey(attribute.getNamespacePrefix())) {
                namespaceMap.put(attribute.getNamespacePrefix(), attribute.getNamespace());
                xtw.writeNamespace(attribute.getNamespacePrefix(), attribute.getNamespace());
              }
              xtw.writeAttribute(attribute.getNamespacePrefix(), attribute.getNamespace(),
                  attribute.getName(), attribute.getValue());
            }
          }
        }
      }
    }
  }
  /*
  经过  isBlacklisted() 处理之后, 如果存在扩展属性
  则通过 model
   */
  public static boolean isBlacklisted(ExtensionAttribute attribute, List<ExtensionAttribute>... blackLists) {
    //如果blackList为空 ,则返回fasle .如果不是
    if (blackLists != null) {
      for (List<ExtensionAttribute> blackList : blackLists) { // 遍历集合
        for (ExtensionAttribute blackAttribute : blackList) {//循环遍历blackList
          // 如果 blackAttribute对象中的name 和attribute对象中的name 相同
          if (blackAttribute.getName().equals(attribute.getName())) {
            //继续 比对 命名空间是否相同
            if ( blackAttribute.getNamespace() != null && attribute.getNamespace() != null
                && blackAttribute.getNamespace().equals(attribute.getNamespace()))
              return true;
            if (blackAttribute.getNamespace() == null && attribute.getNamespace() == null)
              return true;
          }
        }
      }
    }
    return false;
  }
}
