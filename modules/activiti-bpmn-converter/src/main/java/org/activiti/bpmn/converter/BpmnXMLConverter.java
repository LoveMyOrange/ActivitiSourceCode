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
package org.activiti.bpmn.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.alfresco.AlfrescoStartEventXMLConverter;
import org.activiti.bpmn.converter.alfresco.AlfrescoUserTaskXMLConverter;
import org.activiti.bpmn.converter.child.DocumentationParser;
import org.activiti.bpmn.converter.child.IOSpecificationParser;
import org.activiti.bpmn.converter.child.MultiInstanceParser;
import org.activiti.bpmn.converter.export.ActivitiListenerExport;
import org.activiti.bpmn.converter.export.BPMNDIExport;
import org.activiti.bpmn.converter.export.CollaborationExport;
import org.activiti.bpmn.converter.export.DataStoreExport;
import org.activiti.bpmn.converter.export.DefinitionsRootExport;
import org.activiti.bpmn.converter.export.MultiInstanceExport;
import org.activiti.bpmn.converter.export.ProcessExport;
import org.activiti.bpmn.converter.export.SignalAndMessageDefinitionExport;
import org.activiti.bpmn.converter.parser.BpmnEdgeParser;
import org.activiti.bpmn.converter.parser.BpmnShapeParser;
import org.activiti.bpmn.converter.parser.DataStoreParser;
import org.activiti.bpmn.converter.parser.DefinitionsParser;
import org.activiti.bpmn.converter.parser.ExtensionElementsParser;
import org.activiti.bpmn.converter.parser.ImportParser;
import org.activiti.bpmn.converter.parser.InterfaceParser;
import org.activiti.bpmn.converter.parser.ItemDefinitionParser;
import org.activiti.bpmn.converter.parser.LaneParser;
import org.activiti.bpmn.converter.parser.MessageFlowParser;
import org.activiti.bpmn.converter.parser.MessageParser;
import org.activiti.bpmn.converter.parser.ParticipantParser;
import org.activiti.bpmn.converter.parser.PotentialStarterParser;
import org.activiti.bpmn.converter.parser.ProcessParser;
import org.activiti.bpmn.converter.parser.ResourceParser;
import org.activiti.bpmn.converter.parser.SignalParser;
import org.activiti.bpmn.converter.parser.SubProcessParser;
import org.activiti.bpmn.converter.util.BpmnXMLUtil;
import org.activiti.bpmn.converter.util.InputStreamProvider;
import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.Artifact;
import org.activiti.bpmn.model.Association;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BooleanDataObject;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.DateDataObject;
import org.activiti.bpmn.model.DoubleDataObject;
import org.activiti.bpmn.model.EventSubProcess;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.IntegerDataObject;
import org.activiti.bpmn.model.LongDataObject;
import org.activiti.bpmn.model.Pool;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StringDataObject;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.TextAnnotation;
import org.activiti.bpmn.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @desc 此类中并没有构造()
 * 所以 分析静态代码块
 *
 *
 *
 *
 * 该类的静态代码块主要用于初始化类中的各种属性值 并且类中的静态代码块只会被执行一次
 * 由于实例化BpmnXMLConverter  类的同时 该类已经被JVM 加载     所以静态代码块 会先执行
 * 而静态代码块会调用  addConverter()
 * 最终 会将流程元素 以及其对应的解析器添加 到
 *  convertersToBpmnMap,convertersToXMLMap集合中
 *
 *  流程文档中的大部分元素与之对应的解析器是 一一对应关系, 但是对于dataObject 类型的元素来说
 *  就需要特殊处理一下,因为该类型的元素仅仅是数据类型不同而已, 其他属性定义几乎完全相同
 *  常用数据类型有 String Boolean Integer 等,因此没必要为每一种具体的数据类型单独定义一个解析器
 *  只需要在dataObject元素对应的解析器中根据数据类型进行区分处理即可
 *  附带的好处就是 可以将不同数据类型的元素解析工作集中起来管理, 这样也可以控制不同数据类型的元素
 *  按照指定的先后顺序进行解析
 *
 *  ValuedDataObjectXMLConverter 类负责解析dataObject元素
 *
 *  规律:
 *   元素解析之后 都会将解析结果添加到父级元素(process 或者subprocess中) 有这样一个问题
 *   节点和连线如何关联呢????
 *
 */
public class BpmnXMLConverter implements BpmnXMLConstants {

  protected static final Logger LOGGER = LoggerFactory.getLogger(BpmnXMLConverter.class);
	
  protected static final String BPMN_XSD = "org/activiti/impl/bpmn/parser/BPMN20.xsd";
  protected static final String DEFAULT_ENCODING = "UTF-8";
    /*
    实例化一系列元素解析器
    此Map 存储的是 元素  以及 元素对应的解析器,
    key 为 String ,存储流程文档中定义的元素名称, 对应 cnverter.getXMLElementName() 返回值(流程文档中元素的名称)
    value为 元素对应的解析器,
    eg:
    解析结束事件(endEvent) 元素的时候 可以直接从 此Map中查找 key为 endEvent的值 这样就查询到了 EndEventXMLConverter类



    使用Map结构的好处
    如果使用List 会存在如下问题
    1)需要U型你换遍历解析器集合才能查找到适配当前元素的解析器,
    2)客户端向该集合添加元素解析器时 可以能会造成同一个元素的解析器有多个,
    如果同一个元素对应多个解析器, 那么引擎该哪一个结果为准?
    如果使用Map就不会出现上述问题

    客户端可以根据key(元素名称) 值 查找元素对应的解析器   而且相同的key值只能存储一个
    所以如果想要使用自定义元素解析器 只需要根据key 值覆盖引擎默认的元素解析器即可
     */
	protected static Map<String, BaseBpmnXMLConverter> convertersToBpmnMap = new HashMap<String, BaseBpmnXMLConverter>();
	protected static Map<Class<? extends BaseElement>, BaseBpmnXMLConverter> convertersToXMLMap = 
	    new HashMap<Class<? extends BaseElement>, BaseBpmnXMLConverter>();
	
	protected ClassLoader classloader;
	protected List<String> userTaskFormTypes;
	protected List<String> startEventFormTypes;
	//初始化各种内置元素解析器   例如 signalParser
	protected BpmnEdgeParser bpmnEdgeParser = new BpmnEdgeParser();
	protected BpmnShapeParser bpmnShapeParser = new BpmnShapeParser();
	protected DefinitionsParser definitionsParser = new DefinitionsParser();
	protected DocumentationParser documentationParser = new DocumentationParser();
	protected ExtensionElementsParser extensionElementsParser = new ExtensionElementsParser();
	protected ImportParser importParser = new ImportParser();
	protected InterfaceParser interfaceParser = new InterfaceParser();
  protected ItemDefinitionParser itemDefinitionParser = new ItemDefinitionParser();
  protected IOSpecificationParser ioSpecificationParser = new IOSpecificationParser();
  protected DataStoreParser dataStoreParser = new DataStoreParser();
  protected LaneParser laneParser = new LaneParser();
  protected MessageParser messageParser = new MessageParser();
  protected MessageFlowParser messageFlowParser = new MessageFlowParser();
  protected MultiInstanceParser multiInstanceParser = new MultiInstanceParser();
  protected ParticipantParser participantParser = new ParticipantParser();
  protected PotentialStarterParser potentialStarterParser = new PotentialStarterParser();
  protected ProcessParser processParser = new ProcessParser();
  protected ResourceParser resourceParser = new ResourceParser();
  protected SignalParser signalParser = new SignalParser();
  protected SubProcessParser subProcessParser = new SubProcessParser();
	
	static {
	    //省略一系列的元素解析器添加过程
		// events
	  addConverter(new EndEventXMLConverter());
	  addConverter(new StartEventXMLConverter());
    
    // tasks
	  addConverter(new BusinessRuleTaskXMLConverter());
    addConverter(new ManualTaskXMLConverter());
    addConverter(new ReceiveTaskXMLConverter());
    addConverter(new ScriptTaskXMLConverter());
    addConverter(new ServiceTaskXMLConverter());
    addConverter(new SendTaskXMLConverter());
    addConverter(new UserTaskXMLConverter());
    addConverter(new TaskXMLConverter());
    addConverter(new CallActivityXMLConverter());
    
    // gateways
    addConverter(new EventGatewayXMLConverter());
    addConverter(new ExclusiveGatewayXMLConverter());
    addConverter(new InclusiveGatewayXMLConverter());
    addConverter(new ParallelGatewayXMLConverter());
    addConverter(new ComplexGatewayXMLConverter());
    
    // connectors
    addConverter(new SequenceFlowXMLConverter());
    
    // catch, throw and boundary event
    addConverter(new CatchEventXMLConverter());
    addConverter(new ThrowEventXMLConverter());
    addConverter(new BoundaryEventXMLConverter());
    
    // artifacts
    addConverter(new TextAnnotationXMLConverter());
    addConverter(new AssociationXMLConverter());
    
    // data store reference
    addConverter(new DataStoreReferenceXMLConverter());
    /*
    流程文档中的大部分元素与之对应的解析器都是一一对应的关系
    但是对于dataObject 元素来说 就需要特殊处理一下,
    因为该类型的元素仅仅是数据类型的不同而已,
    其他的属性定义几乎完全相同, 常用的 有String ,Boolean  Integer 等
    因此没有必要为每一种具体的数据类型 单独定义一个解析器
    只需要在dataObject元素对应的解析器中 根据数据类型 进行区分处理即可

    附带的好处 就是 可以将不同数据类型的元素解析工作集中起来管理,
    这样也可以控制不同数据类型的元素按照指定的先后顺序进行解析

    ValuedDataObjectXMLConverter  负责解析 dataObject元素
     */
    // data objects
    addConverter(new ValuedDataObjectXMLConverter(), StringDataObject.class);
    addConverter(new ValuedDataObjectXMLConverter(), BooleanDataObject.class);
    addConverter(new ValuedDataObjectXMLConverter(), IntegerDataObject.class);
    addConverter(new ValuedDataObjectXMLConverter(), LongDataObject.class);
    addConverter(new ValuedDataObjectXMLConverter(), DoubleDataObject.class);
    addConverter(new ValuedDataObjectXMLConverter(), DateDataObject.class);
    
    // Alfresco types
    addConverter(new AlfrescoStartEventXMLConverter());
    addConverter(new AlfrescoUserTaskXMLConverter());
  }
  /*
  向此类中的 convertersToBpmnMap  和  convertersToXMLMap 中添加 元素解析器
  开发人员可以通过该() 添加自定义元素解析器, 从而替换引擎默认的元素解析器, 该方法 非常重要
   */
  public static void addConverter(BaseBpmnXMLConverter converter) {
    addConverter(converter, converter.getBpmnElementType());
  }
  /*
  该Map  convertersToBpmnMap     存储元素以及元素对应的解析器,
    key为String   存储流程文档定义的元素名称    converter.getXMLElementName()
    value为元素对应的解析器
    例如
        解析结束事件(endEvent)  可以直接从 此Map 中
        查找key为endEvent的值 这样就可以查询到 EndEventXMLConverter类

        为何使用Map结构????
         方便我们扩展
   */
  public static void addConverter(BaseBpmnXMLConverter converter, Class<? extends BaseElement> elementType) {
    convertersToBpmnMap.put(converter.getXMLElementName(), converter);
    convertersToXMLMap.put(elementType, converter);
  }
  
  public void setClassloader(ClassLoader classloader) {
    this.classloader = classloader;
  }

  public void setUserTaskFormTypes(List<String> userTaskFormTypes) {
    this.userTaskFormTypes = userTaskFormTypes;
  }
  
  public void setStartEventFormTypes(List<String> startEventFormTypes) {
    this.startEventFormTypes = startEventFormTypes;
  }
  /*

  使用BPMN20.XSD 文件以及该文件所引入的 其他XSD 文件来验证流程文档中定义的元素 是否符合其约束
  只有validateSchema 为true 才会开启流程文档元素的验证工作
  开启之后,根据enableSafeBpmnXML 参数执行不同的逻辑
    结论:

  不管使用什么方式验证 schema   首先都会调用 createScheam 创建Schema 对象
  然后基于该对象获取验证器, 最后直接使用验证器进行流程文档的验证工作,
   */
  public void validateModel(InputStreamProvider inputStreamProvider) throws Exception {
    Schema schema = createSchema();
    
    Validator validator = schema.newValidator();
    validator.validate(new StreamSource(inputStreamProvider.getInputStream())); //使用的是StreamSource对象
  }
  /*
  验证是否符合Bpmn规范
   */
  public void validateModel(XMLStreamReader xmlStreamReader) throws Exception {
    Schema schema = createSchema();
    
    Validator validator = schema.newValidator();
    validator.validate(new StAXSource(xmlStreamReader)); //使用的是STAXSource对象
  }
    /*
    BPMN_XSD 位于 activiti-bpmn-converter.jar包中

        classloader 的使用优先级最高
        如果开发人员 想要为classloader 赋值
        只需要自定义一个 文档转换器 并且继承BpmnXMLConverter类
        然后为其设置classloader属性值即可
     */
  protected Schema createSchema() throws SAXException {
      //获取工厂类
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); //获取工厂类
    Schema schema = null;
    //判断当前类的 classloader 是否存在
    if (classloader != null) {

        /*
        如果存在则, 直接通过类加载器 获取BPMN_XSD 获取文件流
        newSchema() 依赖 BPMN_XSD资源文件

         */
      schema = factory.newSchema(classloader.getResource(BPMN_XSD)); //
    }
    //判断schema为空
    if (schema == null) {
        //创建 Schema 对象 依赖BPMN_XSD 资源文件
        // 通过 BpmnXMLConverter 类获取类加载器, 然后再通过该类加载器 获取BPMN_XSD 文件流
      schema = factory.newSchema(BpmnXMLConverter.class.getClassLoader().getResource(BPMN_XSD));
    }
    //判断schema 为空     则报错
    if (schema == null) {
      throw new XMLException("BPMN XSD could not be found");
    }
    return schema;
  }
  /*
  解析流程文档中的元素,最终将元素解析结果封装为  BaseElement  最终返回BpmnModel 对象
  可以将BPMNModel 理解为 流程文档解析之后的内存对象
  流程文档中的所有元素的解析结果 都存储在 该对象中
  开发人员可以直接通过该对象 获取流程文档中定义的所有元素的信息

    在正是环境中 enableSafeBpmnXml 建议设置为 true,这样Activiit引擎解析流程文档时会理解验证
    流程文档中定义的元素是否符合BPMN20.xsd文件的约束要求, 方便及早发现错误信息

   */
  public BpmnModel convertToBpmnModel(InputStreamProvider inputStreamProvider, boolean validateSchema, boolean enableSafeBpmnXml) {
    return convertToBpmnModel(inputStreamProvider, validateSchema, enableSafeBpmnXml, DEFAULT_ENCODING);
  }
  
  public BpmnModel convertToBpmnModel(InputStreamProvider inputStreamProvider, boolean validateSchema, boolean enableSafeBpmnXml, String encoding) {
    XMLInputFactory xif = XMLInputFactory.newInstance(); //实例化工厂
    //为 此对象 添加防护措施, 防止外部DTD 或者XSD入侵
    if (xif.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
      xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    }

    if (xif.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
      xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    if (xif.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
      xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }
        // 初始化此类
    InputStreamReader in = null;
    try {
      in = new InputStreamReader(inputStreamProvider.getInputStream(), encoding);
      XMLStreamReader xtr = xif.createXMLStreamReader(in); // 创建此类
  
      try {
        if (validateSchema) { //判断是否 开启了Schema验证,  如果开启了 则需要验证流程文档中定义的元素是否符合XSD 文件约束要求
          
          if (!enableSafeBpmnXml) {
            validateModel(inputStreamProvider);
          } else {
            validateModel(xtr);
          }
            // 验证完成之后  需要重新打开  InputStreamReader 并且实例化 XMLStreamReader类
          in = new InputStreamReader(inputStreamProvider.getInputStream(), encoding);
          //因为Schema文件验证完毕之后该流已经被关闭了, 因此需要重新打开该流
          xtr = xif.createXMLStreamReader(in);
        }
  
      } catch (Exception e) {
        throw new XMLException(e.getMessage(), e);
      }
  
     // 以上 所有步骤 无误之后  直接 调用该() 开始解析流程文档元素
      return convertToBpmnModel(xtr);
    } catch (UnsupportedEncodingException e) {
      throw new XMLException("The bpmn 2.0 xml is not UTF8 encoded", e);
    } catch (XMLStreamException e) {
      throw new XMLException("Error while reading the BPMN 2.0 XML", e);
    } finally { //关流
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOGGER.debug("Problem closing BPMN input stream", e);
        }
      }
    }
  }
    /*
        当元素解析的运行环境(加载 和验证 流程文档信息) 准备完毕
        开始调用此()

        关于此类 解析流程文档命名空间
            因为STAX 解析流程文档的熟悉怒是按照 流程文档中元素定义的先后顺序自上而下解析的

            所以首先解析 父元素 definitions 该元素对应的解析器为 DefinitionsParser
            该元素可以定义一系列命名空间, URL
            形如<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"></definitions>
            因为流程文档中的元素名称是由开发者定义的 ,为了避免命名冲突
            需要引入命名空间对元素加以区分

        解析外围元素
            什么是外围元素
            首先需要搞清楚这个概念, 平时定义的流程元素, 例如开始节点等元素  都作为process元素的子元素存在
           这里所说的外围元素
           指的是作为definition元素的子元素的同时 还作为process元素的兄弟节点存在
           如消息元素 <definitions> <message>
           最常见的外围元素有message,signal 等
           流程文档中定义的大部分外围元素是没有先后顺序之分的,既可以在 process元素之上,可以在下

            并且外围元素怒 种类不多 并且不容易变动
            所以该类型的元素解析器都会在当前类中进行实例化,
            有关外围元素的解析处理逻辑可以查看对应的解析器
            例如 : 消息元素的解析
            messageFlowParser.parse(xtr,model)  此() 需要2个入参
            xtr 参数   程序可以根据该参数值从流程文档中解析元素的属性值
            model BpmnModel对象,元素解析完毕, 可以直接将元解析结果存储到该元素对应的属性承载类实例对象中
                    然后再将其添加到BpmnModel对象中 ,因为外围元素种类不多
                    平时开发也不经常使用,为了减少风险,增加可控度
                    外围元素的定义以及解析不建议修改和扩展



        解析通用元素
                流程文档中通用元素的种类非常多
                例如文档元素 documentation   扩展元素  extensionElements 等

        这些元素作为流程定义三大要素的子元素存在
        试想一下: 如果每个元素都在自己的解析处理逻辑中对通用元素进行解析,
        势必会造成相同的解析代码  分散在各个模块中  无形之间增加了项目维护的复杂度
        如果通用元素的解析规则变了, 则需要修改 每一个模块的对应的解析逻辑, 工作量非常大

        如果将通用元素的解析功能抽离出来进行统一管理,  则以上这些问题 都不会出现
        虽然暂时还没有看到具体元素的解析过程,但是已经看到了大量类似 xx.parse()的调用 因为元素解析是基于STAX迭代器方式
        所以首先会获取元素类型, 然后再根据元素类型委托不同的解析进行解析,

        开闭原则
     */
	public BpmnModel convertToBpmnModel(XMLStreamReader xtr) { 
	  BpmnModel model = new BpmnModel();// 此类负责  存储所有元素解析之后的结果
	  model.setStartEventFormTypes(startEventFormTypes);
	  model.setUserTaskFormTypes(userTaskFormTypes);
		try {
			Process activeProcess = null;
			List<SubProcess> activeSubProcessList = new ArrayList<SubProcess>();
			while (xtr.hasNext()) {
				try {
					xtr.next();
				} catch(Exception e) {
					LOGGER.debug("Error reading XML document", e);
					throw new XMLException("Error reading XML", e);
				}

				if (xtr.isEndElement()  && ELEMENT_SUBPROCESS.equals(xtr.getLocalName())) {
					activeSubProcessList.remove(activeSubProcessList.size() - 1);
				}
				
				if (xtr.isEndElement()  && ELEMENT_TRANSACTION.equals(xtr.getLocalName())) {
          activeSubProcessList.remove(activeSubProcessList.size() - 1);
        }

				if (xtr.isStartElement() == false) {
					continue;
				}
                //解析definitions元素
				if (ELEMENT_DEFINITIONS.equals(xtr.getLocalName())) {
				  definitionsParser.parse(xtr, model);
				//
        } else if (ELEMENT_RESOURCE.equals(xtr.getLocalName())) {
          resourceParser.parse(xtr, model);
          
				} else if (ELEMENT_SIGNAL.equals(xtr.getLocalName())) {
					signalParser.parse(xtr, model);
					
				} else if (ELEMENT_MESSAGE.equals(xtr.getLocalName())) {
          messageParser.parse(xtr, model);
          
				} else if (ELEMENT_ERROR.equals(xtr.getLocalName())) {
          
          if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_ID))) {
            model.addError(xtr.getAttributeValue(null, ATTRIBUTE_ID),
                xtr.getAttributeValue(null, ATTRIBUTE_ERROR_CODE));
          }
          
				} else if (ELEMENT_IMPORT.equals(xtr.getLocalName())) {
				  importParser.parse(xtr, model);
          
				} else if (ELEMENT_ITEM_DEFINITION.equals(xtr.getLocalName())) {
				  itemDefinitionParser.parse(xtr, model);
          
				} else if (ELEMENT_DATA_STORE.equals(xtr.getLocalName())) {
				  dataStoreParser.parse(xtr, model);
				  
				} else if (ELEMENT_INTERFACE.equals(xtr.getLocalName())) {
				  interfaceParser.parse(xtr, model);
				  
				} else if (ELEMENT_IOSPECIFICATION.equals(xtr.getLocalName())) {
				  ioSpecificationParser.parseChildElement(xtr, activeProcess, model);
					
				} else if (ELEMENT_PARTICIPANT.equals(xtr.getLocalName())) {
				  participantParser.parse(xtr, model);
				  
				} else if (ELEMENT_MESSAGE_FLOW.equals(xtr.getLocalName())) {
          messageFlowParser.parse(xtr, model);
                //解析process元素
				} else if (ELEMENT_PROCESS.equals(xtr.getLocalName())) {
					
				  Process process = processParser.parse(xtr, model);
				  if (process != null) {
            activeProcess = process;	
				  }
				
				} else if (ELEMENT_POTENTIAL_STARTER.equals(xtr.getLocalName())) {
				  potentialStarterParser.parse(xtr, activeProcess);
				  
				} else if (ELEMENT_LANE.equals(xtr.getLocalName())) {
          laneParser.parse(xtr, activeProcess, model);
					
				} else if (ELEMENT_DOCUMENTATION.equals(xtr.getLocalName())) {
				  
					BaseElement parentElement = null;
					if (!activeSubProcessList.isEmpty()) {
						parentElement = activeSubProcessList.get(activeSubProcessList.size() - 1);
					} else if (activeProcess != null) {
						parentElement = activeProcess;
					}
					documentationParser.parseChildElement(xtr, parentElement, model);
				
				} else if (activeProcess == null && ELEMENT_TEXT_ANNOTATION.equals(xtr.getLocalName())) {
				  String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
          TextAnnotation textAnnotation = (TextAnnotation) new TextAnnotationXMLConverter().convertXMLToElement(xtr, model);
          textAnnotation.setId(elementId);
          model.getGlobalArtifacts().add(textAnnotation);
          
				} else if (activeProcess == null && ELEMENT_ASSOCIATION.equals(xtr.getLocalName())) {
          String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
          Association association = (Association) new AssociationXMLConverter().convertXMLToElement(xtr, model);
          association.setId(elementId);
          model.getGlobalArtifacts().add(association);
				
				} else if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
				  extensionElementsParser.parse(xtr, activeSubProcessList, activeProcess, model);
				
				} else if (ELEMENT_SUBPROCESS.equals(xtr.getLocalName())) {
          subProcessParser.parse(xtr, activeSubProcessList, activeProcess);
          
				} else if (ELEMENT_TRANSACTION.equals(xtr.getLocalName())) {
          subProcessParser.parse(xtr, activeSubProcessList, activeProcess);
					
				} else if (ELEMENT_DI_SHAPE.equals(xtr.getLocalName())) {
          bpmnShapeParser.parse(xtr, model);
				
				} else if (ELEMENT_DI_EDGE.equals(xtr.getLocalName())) {
				  bpmnEdgeParser.parse(xtr, model);

				} else {
                    //开始解析 元素  主要解析 事件 网关 活动等 元素信息
					if (!activeSubProcessList.isEmpty() && ELEMENT_MULTIINSTANCE.equalsIgnoreCase(xtr.getLocalName())) {
						
					  multiInstanceParser.parseChildElement(xtr, activeSubProcessList.get(activeSubProcessList.size() - 1), model);
					  
					} else if (convertersToBpmnMap.containsKey(xtr.getLocalName())) {
					    /*
					    判断activateProcess 是否为空 ,如果为空
					    如果该对象为空 ,表示流程文档中没有定义子元素,所以就不需要解析
					    如果该对象不为空 则第44行 首先根据xtr.getLocalName() 获取元素名称
					    然后根据元素名称从 convertersToBpmnMap 集合中查找该元素对应的解析器
					    例如xtr.getLocalName() 值为userTask 那么 对应解析器为 UserTaskXMLConverter


					     */
					  if (activeProcess != null) {
  					  BaseBpmnXMLConverter converter = convertersToBpmnMap.get(xtr.getLocalName());
  					  converter.convertToBpmnModel(xtr, model, activeProcess, activeSubProcessList);
					  }
					}
				}
			}

			for (Process process : model.getProcesses()) {
			  for (Pool pool : model.getPools()) {
			    if (process.getId().equals(pool.getProcessRef())) {
			      pool.setExecutable(process.isExecutable());
			    }
			  }
			  processFlowElements(process.getFlowElements(), process);
			}
		
		} catch (XMLException e) {
		  throw e;
		  
		} catch (Exception e) {
			LOGGER.error("Error processing BPMN document", e);
			throw new XMLException("Error processing BPMN document", e);
		}
		return model;
	}
	/*
	节点 和连线 如何关联???
	    1)循环遍历所有已经解析完毕的process对象, 如果流程文档中定义 有participant 元素 (泳道)
	    则循环遍历所有的  泳道(pool)
	    并且判断process对象中的id 值 是否与pool对象中的processRef 值相等,,
	    如果2者相等,  则设置pool对象中的 executuable 属性值  (是否 可以执行 )

	    然后调用 本() 进行 节点和连线的关联操作
	 */
	private void processFlowElements(Collection<FlowElement> flowElementList, BaseElement parentScope) {
	    //循环遍历集合
	  for (FlowElement flowElement : flowElementList) {
	      /*
	      如果flowElement对象 类型是  SequenceFlow
	      首先获取 连线中的源节点  sourceNode
	      并将sequnecFlow 设置到 sourceNode中
	      然后获取连线中的目标 targetNode   并且 sequneceFlow 设置到 targetNdoe中
	       */
  	  if (flowElement instanceof SequenceFlow) {
        SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
        FlowNode sourceNode = getFlowNodeFromScope(sequenceFlow.getSourceRef(), parentScope);
        if (sourceNode != null) {
          sourceNode.getOutgoingFlows().add(sequenceFlow);
        }
        FlowNode targetNode = getFlowNodeFromScope(sequenceFlow.getTargetRef(), parentScope);
        if (targetNode != null) {
          targetNode.getIncomingFlows().add(sequenceFlow);
        }
        //如果flowElemtn 是边界事件 , 则需要将边界事件与其吸附的对象进行关联
      } else if (flowElement instanceof BoundaryEvent) {
        BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
        FlowElement attachedToElement = getFlowNodeFromScope(boundaryEvent.getAttachedToRefId(), parentScope);
        if(attachedToElement != null) {
          boundaryEvent.setAttachedToRef((Activity) attachedToElement);
          ((Activity) attachedToElement).getBoundaryEvents().add(boundaryEvent);
        }
        //如果是子流程,  调用 processFlowElements 继续执行以上两个步骤
      } else if(flowElement instanceof SubProcess) {
        SubProcess subProcess = (SubProcess) flowElement;
        processFlowElements(subProcess.getFlowElements(), subProcess);
      }
	  }
	}
	
	private FlowNode getFlowNodeFromScope(String elementId, BaseElement scope) {
	  FlowNode flowNode = null;
	  if (StringUtils.isNotEmpty(elementId)) {
  	  if (scope instanceof Process) {
  	    flowNode = (FlowNode) ((Process) scope).getFlowElement(elementId);
  	  } else if (scope instanceof SubProcess) {
  	    flowNode = (FlowNode) ((SubProcess) scope).getFlowElement(elementId);
  	  }
	  }
	  return flowNode;
	}
	/*
	将BpmnModel 转化为流程文档内容
	该操作 与convertBpmnModel()的操作完全相反,

	 */
	public byte[] convertToXML(BpmnModel model) {
	  return convertToXML(model, DEFAULT_ENCODING);
	}
	
	public byte[] convertToXML(BpmnModel model, String encoding) {
    try {

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      
      XMLOutputFactory xof = XMLOutputFactory.newInstance();
      OutputStreamWriter out = new OutputStreamWriter(outputStream, encoding);

      XMLStreamWriter writer = xof.createXMLStreamWriter(out);
      XMLStreamWriter xtw = new IndentingXMLStreamWriter(writer);

      DefinitionsRootExport.writeRootElement(model, xtw, encoding);
      CollaborationExport.writePools(model, xtw);
      DataStoreExport.writeDataStores(model, xtw);
      SignalAndMessageDefinitionExport.writeSignalsAndMessages(model, xtw);
      
      for (Process process : model.getProcesses()) {
        
        if (process.getFlowElements().isEmpty() && process.getLanes().isEmpty()) {
          // empty process, ignore it 
          continue;
        }
      
        ProcessExport.writeProcess(process, xtw);
        
        for (FlowElement flowElement : process.getFlowElements()) {
          createXML(flowElement, model, xtw);
        }
        
        for (Artifact artifact : process.getArtifacts()) {
          createXML(artifact, model, xtw);
        }
        
        // end process element
        xtw.writeEndElement();
      }

      BPMNDIExport.writeBPMNDI(model, xtw);

      // end definitions root element
      xtw.writeEndElement();
      xtw.writeEndDocument();

      xtw.flush();

      outputStream.close();

      xtw.close();
      
      return outputStream.toByteArray();
      
    } catch (Exception e) {
      LOGGER.error("Error writing BPMN XML", e);
      throw new XMLException("Error writing BPMN XML", e);
    }
  }

  private void createXML(FlowElement flowElement, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    
    if (flowElement instanceof SubProcess) {
      
      SubProcess subProcess = (SubProcess) flowElement;
      if (flowElement instanceof Transaction) {
        xtw.writeStartElement(ELEMENT_TRANSACTION);
      } else {
        xtw.writeStartElement(ELEMENT_SUBPROCESS);
      }
      
      xtw.writeAttribute(ATTRIBUTE_ID, subProcess.getId());
      if (StringUtils.isNotEmpty(subProcess.getName())) {
        xtw.writeAttribute(ATTRIBUTE_NAME, subProcess.getName());
      } else {
        xtw.writeAttribute(ATTRIBUTE_NAME, "subProcess");
      }
      
      if (subProcess instanceof EventSubProcess) {
        xtw.writeAttribute(ATTRIBUTE_TRIGGERED_BY, ATTRIBUTE_VALUE_TRUE);
        
      } else if (subProcess instanceof Transaction == false) {
        if (subProcess.isAsynchronous()) {
          BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, ATTRIBUTE_VALUE_TRUE, xtw);
          if (subProcess.isNotExclusive()) {
            BpmnXMLUtil.writeQualifiedAttribute(ATTRIBUTE_ACTIVITY_EXCLUSIVE, ATTRIBUTE_VALUE_FALSE, xtw);
          }
        }
      }
      
      if (StringUtils.isNotEmpty(subProcess.getDocumentation())) {

        xtw.writeStartElement(ELEMENT_DOCUMENTATION);
        xtw.writeCharacters(subProcess.getDocumentation());
        xtw.writeEndElement();
      }
      
      boolean didWriteExtensionStartElement = ActivitiListenerExport.writeListeners(subProcess, false, xtw);
      
      didWriteExtensionStartElement = BpmnXMLUtil.writeExtensionElements(subProcess, didWriteExtensionStartElement, model.getNamespaces(), xtw);
      if (didWriteExtensionStartElement) {
        // closing extensions element
        xtw.writeEndElement();
      }
      
      MultiInstanceExport.writeMultiInstance(subProcess, xtw);
      
      for (FlowElement subElement : subProcess.getFlowElements()) {
        createXML(subElement, model, xtw);
      }
      
      for (Artifact artifact : subProcess.getArtifacts()) {
        createXML(artifact, model, xtw);
      }
      
      xtw.writeEndElement();
      
    } else {
    
      BaseBpmnXMLConverter converter = convertersToXMLMap.get(flowElement.getClass());
      
      if (converter == null) {
        throw new XMLException("No converter for " + flowElement.getClass() + " found");
      }
      
      converter.convertToXML(xtw, flowElement, model);
    }
  }
  
  private void createXML(Artifact artifact, BpmnModel model, XMLStreamWriter xtw) throws Exception {
    
    BaseBpmnXMLConverter converter = convertersToXMLMap.get(artifact.getClass());
      
    if (converter == null) {
      throw new XMLException("No converter for " + artifact.getClass() + " found");
    }
      
    converter.convertToXML(xtw, artifact, model);
  }
}
