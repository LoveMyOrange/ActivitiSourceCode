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
package org.activiti.engine.impl.bpmn.parser;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.constants.BpmnXMLConstants;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BoundaryEvent;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Event;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.Import;
import org.activiti.bpmn.model.Interface;
import org.activiti.bpmn.model.Message;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.bpmn.data.ClassStructureDefinition;
import org.activiti.engine.impl.bpmn.data.ItemDefinition;
import org.activiti.engine.impl.bpmn.data.ItemKind;
import org.activiti.engine.impl.bpmn.data.StructureDefinition;
import org.activiti.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.activiti.engine.impl.bpmn.webservice.BpmnInterface;
import org.activiti.engine.impl.bpmn.webservice.BpmnInterfaceImplementation;
import org.activiti.engine.impl.bpmn.webservice.MessageDefinition;
import org.activiti.engine.impl.bpmn.webservice.Operation;
import org.activiti.engine.impl.bpmn.webservice.OperationImplementation;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.HasDIBounds;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.impl.util.io.InputStreamSource;
import org.activiti.engine.impl.util.io.ResourceStreamSource;
import org.activiti.engine.impl.util.io.StreamSource;
import org.activiti.engine.impl.util.io.StringStreamSource;
import org.activiti.engine.impl.util.io.UrlStreamSource;
import org.activiti.validation.ProcessValidator;
import org.activiti.validation.ValidationError;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific parsing of one BPMN 2.0 XML file, created by the {@link BpmnParser}.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 * pvm的入口
 *  该对象存储了对象解析的公共属性值,
 * 在BpmnDeployer类的 deploy()  调用了 此类的 execute()
 */
public class BpmnParse implements BpmnXMLConstants {

  protected static final Logger LOGGER = LoggerFactory.getLogger(BpmnParse.class);

  public static final String PROPERTYNAME_INITIAL = "initial";
  public static final String PROPERTYNAME_INITIATOR_VARIABLE_NAME = "initiatorVariableName";
  public static final String PROPERTYNAME_CONDITION = "condition";
  public static final String PROPERTYNAME_CONDITION_TEXT = "conditionText";
  public static final String PROPERTYNAME_TIMER_DECLARATION = "timerDeclarations";
  public static final String PROPERTYNAME_ISEXPANDED = "isExpanded";
  public static final String PROPERTYNAME_START_TIMER = "timerStart";
  public static final String PROPERTYNAME_COMPENSATION_HANDLER_ID = "compensationHandler";
  public static final String PROPERTYNAME_IS_FOR_COMPENSATION = "isForCompensation";
  public static final String PROPERTYNAME_ERROR_EVENT_DEFINITIONS = "errorEventDefinitions";
  public static final String PROPERTYNAME_EVENT_SUBSCRIPTION_DECLARATION = "eventDefinitions";

  protected String name;

  protected boolean validateSchema = true;
  protected boolean validateProcess = true;
  
  protected StreamSource streamSource;
  protected String sourceSystemId;

  protected BpmnModel bpmnModel;

  protected String targetNamespace;

  /** The deployment to which the parsed process definitions will be added. */
  protected DeploymentEntity deployment;

  /** The end result of the parsing: a list of process definition. */
  protected List<ProcessDefinitionEntity> processDefinitions = new ArrayList<ProcessDefinitionEntity>();

  /** A map for storing sequence flow based on their id during parsing. */
  protected Map<String, TransitionImpl> sequenceFlows;

  protected BpmnParseHandlers bpmnParserHandlers;

  protected ProcessDefinitionEntity currentProcessDefinition;

  protected FlowElement currentFlowElement;

  protected ActivityImpl currentActivity;

  protected LinkedList<SubProcess> currentSubprocessStack = new LinkedList<SubProcess>();

  protected LinkedList<ScopeImpl> currentScopeStack = new LinkedList<ScopeImpl>();

  /**
   * Mapping containing values stored during the first phase of parsing since
   * other elements can reference these messages.
   * 
   * All the map's elements are defined outside the process definition(s), which
   * means that this map doesn't need to be re-initialized for each new process
   * definition.
   */
  protected Map<String, MessageDefinition> messages = new HashMap<String, MessageDefinition>();
  protected Map<String, StructureDefinition> structures = new HashMap<String, StructureDefinition>();
  protected Map<String, BpmnInterfaceImplementation> interfaceImplementations = new HashMap<String, BpmnInterfaceImplementation>();
  protected Map<String, OperationImplementation> operationImplementations = new HashMap<String, OperationImplementation>();
  protected Map<String, ItemDefinition> itemDefinitions = new HashMap<String, ItemDefinition>();
  protected Map<String, BpmnInterface> bpmnInterfaces = new HashMap<String, BpmnInterface>();
  protected Map<String, Operation> operations = new HashMap<String, Operation>();
  protected Map<String, XMLImporter> importers = new HashMap<String, XMLImporter>();
  protected Map<String, String> prefixs = new HashMap<String, String>();

  // Factories
  protected ExpressionManager expressionManager;
  protected ActivityBehaviorFactory activityBehaviorFactory;
  protected ListenerFactory listenerFactory;

  /**
   * Constructor to be called by the {@link BpmnParser}.
   */
  public BpmnParse(BpmnParser parser) {
    this.expressionManager = parser.getExpressionManager();
    this.activityBehaviorFactory = parser.getActivityBehaviorFactory();
    this.listenerFactory = parser.getListenerFactory();
    this.bpmnParserHandlers = parser.getBpmnParserHandlers();
    this.initializeXSDItemDefinitions();
  }

  protected void initializeXSDItemDefinitions() {
    this.itemDefinitions.put("http://www.w3.org/2001/XMLSchema:string", new ItemDefinition("http://www.w3.org/2001/XMLSchema:string",
            new ClassStructureDefinition(String.class)));
  }

  public BpmnParse deployment(DeploymentEntity deployment) {
    this.deployment = deployment;
    return this;
  }
  /*
  PVM的入口
  这个()主要是用于 元素的解析 和对象的解析,  但是 最后 有一部分 却在 初始化 WebService 所需要的元素对象
  这里设计不合理,
       createImports();
      createItemDefinitions();
      createMessages();
      createOperations();

      这些代码 放到 WebServiceActivityBehavior 类中 会更加的合理 ,
      即  用的时候 再去解析, 而提升性能
   */
  public BpmnParse execute() {
    try {
        //获取 流程引擎配置类
    	ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    	//实例化BpmnXMLConverter
      BpmnXMLConverter converter = new BpmnXMLConverter();

      boolean enableSafeBpmnXml = false;
      String encoding = null;
      if (processEngineConfiguration != null) {
        enableSafeBpmnXml = processEngineConfiguration.isEnableSafeBpmnXml();//默认为fasle
        encoding = processEngineConfiguration.getXmlEncoding();// 默认 UTF-8
      }
      //将流程文档转化为Activiti内部表示   转化之后 保存在BpmnModel对象中
      if (encoding != null) {
        bpmnModel = converter.convertToBpmnModel(streamSource, validateSchema, enableSafeBpmnXml, encoding);
      } else {
        bpmnModel = converter.convertToBpmnModel(streamSource, validateSchema, enableSafeBpmnXml);
      }
      
      if (validateProcess) {
        //默认为true 开启流程验证    自定义的模型校验器 会生效
      	ProcessValidator processValidator = processEngineConfiguration.getProcessValidator();
      	if (processValidator == null) {
      		LOGGER.warn("Process should be validated, but no process validator is configured on the process engine configuration!");
      	} else {
      		List<ValidationError> validationErrors = processValidator.validate(bpmnModel);
      		if(validationErrors != null && !validationErrors.isEmpty()) {
      			
      			StringBuilder warningBuilder = new StringBuilder();
	      		StringBuilder errorBuilder = new StringBuilder();
	      		
	          for (ValidationError error : validationErrors) {
	          	if (error.isWarning()) {
	          		warningBuilder.append(error.toString());
	          		warningBuilder.append("\n");
	          	} else {
	          		errorBuilder.append(error.toString());
	          		errorBuilder.append("\n");
	          	}
	          }
	           
	          // Throw exception if there is any error
	          if (errorBuilder.length() > 0) {
	          	throw new ActivitiException("Errors while parsing:\n" + errorBuilder.toString());
	          }
	          
	          // Write out warnings (if any)
	          if (warningBuilder.length() > 0) {
	          	LOGGER.warn("Following warnings encountered during process validation: " + warningBuilder.toString());
	          }
	          
      		}
      	}
      }

      // 初始化一系列 WebService 服务所需要的元素对象
      createImports();
      createItemDefinitions();
      createMessages();
      createOperations();
      // 此() 用于全局调度对象解析工作,  内部通过BpmnParseHandler接口的具体实现类 完成对象的解析工作
      transformProcessDefinitions();
      
    } catch (Exception e) {
      if (e instanceof ActivitiException) {
        throw (ActivitiException) e;
      } else if (e instanceof XMLException) {
        throw (XMLException) e;
      } else {
        throw new ActivitiException("Error parsing XML", e);
      }
    }

    return this;
  }

  public BpmnParse name(String name) {
    this.name = name;
    return this;
  }

  public BpmnParse sourceInputStream(InputStream inputStream) {
    if (name == null) {
      name("inputStream");
    }
    setStreamSource(new InputStreamSource(inputStream));
    return this;
  }

  public BpmnParse sourceResource(String resource) {
    return sourceResource(resource, null);
  }

  public BpmnParse sourceUrl(URL url) {
    if (name == null) {
      name(url.toString());
    }
    setStreamSource(new UrlStreamSource(url));
    return this;
  }

  public BpmnParse sourceUrl(String url) {
    try {
      return sourceUrl(new URL(url));
    } catch (MalformedURLException e) {
      throw new ActivitiIllegalArgumentException("malformed url: " + url, e);
    }
  }

  public BpmnParse sourceResource(String resource, ClassLoader classLoader) {
    if (name == null) {
      name(resource);
    }
    setStreamSource(new ResourceStreamSource(resource, classLoader));
    return this;
  }

  public BpmnParse sourceString(String string) {
    if (name == null) {
      name("string");
    }
    setStreamSource(new StringStreamSource(string));
    return this;
  }

  protected void setStreamSource(StreamSource streamSource) {
    if (this.streamSource != null) {
      throw new ActivitiIllegalArgumentException("invalid: multiple sources " + this.streamSource + " and " + streamSource);
    }
    this.streamSource = streamSource;
  }

  protected void createImports() {
    for (Import theImport : bpmnModel.getImports()) {
      XMLImporter importer = this.getImporter(theImport);
      if (importer == null) {
        throw new ActivitiException("Could not import item of type " + theImport.getImportType());
      } else {
        importer.importFrom(theImport, this);
      }
    }
  }

  protected XMLImporter getImporter(Import theImport) {
    if (this.importers.containsKey(theImport.getImportType())) {
      return this.importers.get(theImport.getImportType());
    } else {
      if (theImport.getImportType().equals("http://schemas.xmlsoap.org/wsdl/")) {
        Class< ? > wsdlImporterClass;
        try {
          wsdlImporterClass = Class.forName("org.activiti.engine.impl.webservice.CxfWSDLImporter", true, Thread.currentThread().getContextClassLoader());
          XMLImporter newInstance = (XMLImporter) wsdlImporterClass.newInstance();
          this.importers.put(theImport.getImportType(), newInstance);
          return newInstance;
        } catch (Exception e) {
          throw new ActivitiException("Could not find importer for type " + theImport.getImportType());
        }
      }
      return null;
    }
  }

  public void createMessages() {
    for (Message messageElement : bpmnModel.getMessages()) {
      MessageDefinition messageDefinition = new MessageDefinition(messageElement.getId(), name);
      if (StringUtils.isNotEmpty(messageElement.getItemRef())) {
        if (this.itemDefinitions.containsKey(messageElement.getItemRef())) {
          ItemDefinition itemDefinition = this.itemDefinitions.get(messageElement.getItemRef());
          messageDefinition.setItemDefinition(itemDefinition);
        }
      }
      this.messages.put(messageDefinition.getId(), messageDefinition);

    }
  }

  protected void createItemDefinitions() {
    for (org.activiti.bpmn.model.ItemDefinition itemDefinitionElement : bpmnModel.getItemDefinitions().values()) {
      StructureDefinition structure = null;

      try {
        // it is a class
        Class< ? > classStructure = ReflectUtil.loadClass(itemDefinitionElement.getStructureRef());
        structure = new ClassStructureDefinition(classStructure);
      } catch (ActivitiException e) {
        // it is a reference to a different structure
        structure = this.structures.get(itemDefinitionElement.getStructureRef());
      }

      ItemDefinition itemDefinition = new ItemDefinition(itemDefinitionElement.getId(), structure);
      if (StringUtils.isNotEmpty(itemDefinitionElement.getItemKind())) {
        itemDefinition.setItemKind(ItemKind.valueOf(itemDefinitionElement.getItemKind()));
      }
      itemDefinitions.put(itemDefinition.getId(), itemDefinition);
    }
  }

  protected void createOperations() {
    for (Interface interfaceObject : bpmnModel.getInterfaces()) {
      BpmnInterface bpmnInterface = new BpmnInterface(interfaceObject.getId(), interfaceObject.getName());
      bpmnInterface.setImplementation(this.interfaceImplementations.get(interfaceObject.getImplementationRef()));

      for (org.activiti.bpmn.model.Operation operationObject : interfaceObject.getOperations()) {
        if (this.messages.containsKey(operationObject.getInMessageRef())) {
          MessageDefinition inMessage = this.messages.get(operationObject.getInMessageRef());
          Operation operation = new Operation(operationObject.getId(), operationObject.getName(), bpmnInterface, inMessage);
          operation.setImplementation(this.operationImplementations.get(operationObject.getImplementationRef()));

          if (StringUtils.isNotEmpty(operationObject.getOutMessageRef())) {
            if (this.messages.containsKey(operationObject.getOutMessageRef())) {
              MessageDefinition outMessage = this.messages.get(operationObject.getOutMessageRef());
              operation.setOutMessage(outMessage);
            }
          }

          operations.put(operation.getId(), operation);
        }
      }
    }
  }

  /**
   * Parses the 'definitions' root element
   *
   * 流程定义转换准备,
   *  负责调度对象的解析工作
   *     该() 做了一些 辅助性的工作,
   *      核心工作 在  bpmnParserHandlers.parseElement ()中
   */
  protected void transformProcessDefinitions() {
    sequenceFlows = new HashMap<String, TransitionImpl>(); //初始化集合
    for (Process process : bpmnModel.getProcesses()) {//遍历所有的process对象
      if (process.isExecutable()) {//如果process是可以执行的
        bpmnParserHandlers.parseElement(this, process); //开始解析对象
      }
    }

    if (!processDefinitions.isEmpty()) {
      processDI(); //解析元素坐标信息
    }
  }
/*
  *   流程子元素对象解析入口
  * 用于解析process元素下的所有子元素对象 (开始节点, 任务节点, 结束节点 等
  *
  * 下面代码可以看出 process对象的解析 与其子元素对象的解析处理逻辑差异还是很大
* */
  public void processFlowElements(Collection<FlowElement> flowElements) {

    // Parsing the elements is done in a strict order of types,
    // as otherwise certain information might not be available when parsing a
    // certain type.
    
    // Using lists as we want to keep the order in which they are defined
    //所有的连线信息集合
    List<SequenceFlow> sequenceFlowToParse = new ArrayList<SequenceFlow>();
    //所有的边界事件集合
    List<BoundaryEvent> boundaryEventsToParse = new ArrayList<BoundaryEvent>();
    
    // Flow elements that depend on other elements are parse after the first run-through
    /*
    所有的元素对象信息,除此之外的所有常规活动对象直接委托 parseElement() 进行对象的解析工作 (如userTask节点 )

    从这里可以看出 子元素对象的解析有先后顺序之分, 回想一下元素的类型,
    例如任务节点对象不会依赖其它对象, 但是连线元素就需要依赖源节点和目标节点
    可以结合流程文档中元素定义的先后顺序进行思考,
      例如下面一个场景,:

        程序需要解析元素对象,  那么肯定需要获取连线对象 的源头 和 目的地两个节点的对象信息
        如果没有区分流程对象的解析顺序 势必会造成 同一个节点反复解析, 而且解析顺序混乱

        如果对元素对象的类型进行区分归档,这样就可以首先 解析没有依赖关系的对象
        然后再解析有依赖关系的对象, 并且将解析结果 存储到BpmnParse对象中,
        这样后续直接解析对象, 可以直接从BpmnParses 对象中获取已经解析的对象而无需再次重复解析
     */

    List<FlowElement> defferedFlowElementsToParse = new ArrayList<FlowElement>();

    // Activities are parsed first
    /*
    遍历所有的元素对象
    按照指定的顺序 分别遍历以上3个集合
    并且调用parseElement() 解析集合中的对象
    附带的好处就是 解除元素对象之间的相互依赖问题, 一箭双雕
     */
    for (FlowElement flowElement : flowElements) {

      // Sequence flow are also flow elements, but are only parsed once everyactivity is found
      if (flowElement instanceof SequenceFlow) { //如果对象为连线对象
        sequenceFlowToParse.add((SequenceFlow) flowElement);
      } else if (flowElement instanceof BoundaryEvent) { //如果是边界事件
        boundaryEventsToParse.add((BoundaryEvent) flowElement);
      } else if (flowElement instanceof Event) {
        defferedFlowElementsToParse.add(flowElement);//如果对象为事件
      } else {
        bpmnParserHandlers.parseElement(this, flowElement);//以上3者都不是开始解析
      }

    }
    
    // Deferred elements
    //遍历 集合 并且对集合中元素进行解析
    for (FlowElement flowElement : defferedFlowElementsToParse) {
      bpmnParserHandlers.parseElement(this, flowElement);
    }

    // Boundary events are parsed after all the regular activities are parsed
    for (BoundaryEvent boundaryEvent : boundaryEventsToParse) {
      bpmnParserHandlers.parseElement(this, boundaryEvent);
    }

    // sequence flows
    for (SequenceFlow sequenceFlow : sequenceFlowToParse) {
      bpmnParserHandlers.parseElement(this, sequenceFlow);
    }
   
  }

  // Diagram interchange
  // /////////////////////////////////////////////////////////////////

  public void processDI() {
    if (!bpmnModel.getLocationMap().isEmpty()) {

      // Verify if all referenced elements exist
      for (String bpmnReference : bpmnModel.getLocationMap().keySet()) {
        if (bpmnModel.getFlowElement(bpmnReference) == null) {
        	// ACT-1625: don't warn when	artifacts are referenced from DI
        	if (bpmnModel.getArtifact(bpmnReference) == null) {
        	  // check if it's a Pool or Lane, then DI is ok
            if (bpmnModel.getPool(bpmnReference) == null && bpmnModel.getLane(bpmnReference) == null) {
              LOGGER.warn("Invalid reference in diagram interchange definition: could not find " + bpmnReference);
            }
        	}
        } else if (! (bpmnModel.getFlowElement(bpmnReference) instanceof FlowNode)) {
          LOGGER.warn("Invalid reference in diagram interchange definition: " + bpmnReference + " does not reference a flow node");
        }
      }
      for (String bpmnReference : bpmnModel.getFlowLocationMap().keySet()) {
        if (bpmnModel.getFlowElement(bpmnReference) == null) {
          // ACT-1625: don't warn when	artifacts are referenced from DI
        	if (bpmnModel.getArtifact(bpmnReference) == null) {
        		LOGGER.warn("Invalid reference in diagram interchange definition: could not find " + bpmnReference);
        	}	
        } else if (! (bpmnModel.getFlowElement(bpmnReference) instanceof SequenceFlow)) {
          LOGGER.warn("Invalid reference in diagram interchange definition: " + bpmnReference + " does not reference a sequence flow");
        }
      }
      
      for (Process process : bpmnModel.getProcesses()) {
        if (!process.isExecutable()) {
          continue;
        }
        
        // Parse diagram interchange information
        ProcessDefinitionEntity processDefinition = getProcessDefinition(process.getId());
        if (processDefinition != null) {
          processDefinition.setGraphicalNotationDefined(true);
          for (String shapeId : bpmnModel.getLocationMap().keySet()) {
            if (processDefinition.findActivity(shapeId) != null) {
              createBPMNShape(shapeId, bpmnModel.getGraphicInfo(shapeId), processDefinition);
            }
          }

          for (String edgeId : bpmnModel.getFlowLocationMap().keySet()) {
            if (bpmnModel.getFlowElement(edgeId) != null) {
              createBPMNEdge(edgeId, bpmnModel.getFlowLocationGraphicInfo(edgeId));
            }
          }
        }
      }
    }
  }
  
  public void createBPMNShape(String key, GraphicInfo graphicInfo, ProcessDefinitionEntity processDefinition) {
    ActivityImpl activity = processDefinition.findActivity(key);
    if (activity != null) {
      createDIBounds(graphicInfo, activity);

    } else {
      org.activiti.engine.impl.pvm.process.Lane lane = processDefinition.getLaneForId(key);

      if (lane != null) {
        // The shape represents a lane
        createDIBounds(graphicInfo, lane);
      }
    }
  }

  protected void createDIBounds(GraphicInfo graphicInfo, HasDIBounds target) {
    target.setX((int) graphicInfo.getX());
    target.setY((int) graphicInfo.getY());
    target.setWidth((int) graphicInfo.getWidth());
    target.setHeight((int) graphicInfo.getHeight());
  }

  public void createBPMNEdge(String key, List<GraphicInfo> graphicList) {
    FlowElement flowElement = bpmnModel.getFlowElement(key);
    if (flowElement != null && sequenceFlows.containsKey(key)) {
      TransitionImpl sequenceFlow = sequenceFlows.get(key);
      List<Integer> waypoints = new ArrayList<Integer>();
      for (GraphicInfo waypointInfo : graphicList) {
        waypoints.add((int) waypointInfo.getX());
        waypoints.add((int) waypointInfo.getY());
      }
      sequenceFlow.setWaypoints(waypoints);
    } else if (bpmnModel.getArtifact(key) != null) {
      // it's an association, so nothing to do
    } else {
      LOGGER.warn("Invalid reference in 'bpmnElement' attribute, sequenceFlow " + key + " not found");
    }
  }

  public ProcessDefinitionEntity getProcessDefinition(String processDefinitionKey) {
    for (ProcessDefinitionEntity processDefinition : processDefinitions) {
      if (processDefinition.getKey().equals(processDefinitionKey)) {
        return processDefinition;
      }
    }
    return null;
  }

  public void addStructure(StructureDefinition structure) {
    this.structures.put(structure.getId(), structure);
  }

  public void addService(BpmnInterfaceImplementation bpmnInterfaceImplementation) {
    this.interfaceImplementations.put(bpmnInterfaceImplementation.getName(), bpmnInterfaceImplementation);
  }

  public void addOperation(OperationImplementation operationImplementation) {
    this.operationImplementations.put(operationImplementation.getId(), operationImplementation);
  }

  /*
   * ------------------- GETTERS AND SETTERS -------------------
   */
  
  public boolean isValidateSchema() {
		return validateSchema;
	}

	public void setValidateSchema(boolean validateSchema) {
		this.validateSchema = validateSchema;
	}

	public boolean isValidateProcess() {
		return validateProcess;
	}

	public void setValidateProcess(boolean validateProcess) {
		this.validateProcess = validateProcess;
	}
	
	public List<ProcessDefinitionEntity> getProcessDefinitions() {
		return processDefinitions;
	}

	public String getTargetNamespace() {
    return targetNamespace;
  }

  public BpmnParseHandlers getBpmnParserHandlers() {
    return bpmnParserHandlers;
  }

  public void setBpmnParserHandlers(BpmnParseHandlers bpmnParserHandlers) {
    this.bpmnParserHandlers = bpmnParserHandlers;
  }

  public DeploymentEntity getDeployment() {
    return deployment;
  }

  public void setDeployment(DeploymentEntity deployment) {
    this.deployment = deployment;
  }

  public BpmnModel getBpmnModel() {
    return bpmnModel;
  }

  public void setBpmnModel(BpmnModel bpmnModel) {
    this.bpmnModel = bpmnModel;
  }

  public ActivityBehaviorFactory getActivityBehaviorFactory() {
    return activityBehaviorFactory;
  }

  public void setActivityBehaviorFactory(ActivityBehaviorFactory activityBehaviorFactory) {
    this.activityBehaviorFactory = activityBehaviorFactory;
  }

  public ListenerFactory getListenerFactory() {
    return listenerFactory;
  }

  public void setListenerFactory(ListenerFactory listenerFactory) {
    this.listenerFactory = listenerFactory;
  }

  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  public Map<String, TransitionImpl> getSequenceFlows() {
    return sequenceFlows;
  }

  public Map<String, MessageDefinition> getMessages() {
    return messages;
  }

  public Map<String, BpmnInterfaceImplementation> getInterfaceImplementations() {
    return interfaceImplementations;
  }

  public Map<String, ItemDefinition> getItemDefinitions() {
    return itemDefinitions;
  }

  public Map<String, XMLImporter> getImporters() {
    return importers;
  }

  public Map<String, Operation> getOperations() {
    return operations;
  }

  public void setOperations(Map<String, Operation> operations) {
    this.operations = operations;
  }

  public ProcessDefinitionEntity getCurrentProcessDefinition() {
    return currentProcessDefinition;
  }

  public void setCurrentProcessDefinition(ProcessDefinitionEntity currentProcessDefinition) {
    this.currentProcessDefinition = currentProcessDefinition;
  }

  public FlowElement getCurrentFlowElement() {
    return currentFlowElement;
  }

  public void setCurrentFlowElement(FlowElement currentFlowElement) {
    this.currentFlowElement = currentFlowElement;
  }

  public ActivityImpl getCurrentActivity() {
    return currentActivity;
  }

  public void setCurrentActivity(ActivityImpl currentActivity) {
    this.currentActivity = currentActivity;
  }

  public void setCurrentSubProcess(SubProcess subProcess) {
    currentSubprocessStack.push(subProcess);
  }

  public SubProcess getCurrentSubProcess() {
    return currentSubprocessStack.peek();
  }

  public void removeCurrentSubProcess() {
    currentSubprocessStack.pop();
  }

  public void setCurrentScope(ScopeImpl scope) {
    currentScopeStack.push(scope);
  }

  public ScopeImpl getCurrentScope() {
    return currentScopeStack.peek();
  }

  public void removeCurrentScope() {
    currentScopeStack.pop();
  }
  
  public BpmnParse setSourceSystemId(String systemId) {
    sourceSystemId = systemId;
    return this;
  }
  
  public String getSourceSystemId() {
    return this.sourceSystemId;
  }
  
}