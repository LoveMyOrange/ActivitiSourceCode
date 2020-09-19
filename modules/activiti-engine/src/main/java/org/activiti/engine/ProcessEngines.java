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
package org.activiti.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.impl.ProcessEngineInfoImpl;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/** Helper for initializing and closing process engines in server environments.
 * <br>
 * All created {@link ProcessEngine}s will be registered with this class.
 * <br>
 * The activiti-webapp-init webapp will
 * call the {@link #init()} method when the webapp is deployed and it will call the 
 * {@link #destroy()} method when the webapp is destroyed, using a context-listener 
 * (<code>org.activiti.impl.servlet.listener.ProcessEnginesServletContextListener</code>).  That way, 
 * all applications can just use the {@link #getProcessEngines()} to 
 * obtain pre-initialized and cached process engines. <br>
 * <br>
 * Please note that there is <b>no lazy initialization</b> of process engines, so make sure the 
 * context-listener is configured or {@link ProcessEngine}s are already created so they were registered
 * on this class.<br>
 * <br>
 * The {@link #init()} method will try to build one {@link ProcessEngine} for 
 * each activiti.cfg.xml file found on the classpath.  If you have more then one,
 * make sure you specify different process.engine.name values.
 *  
 * @author Tom Baeyens
 * @author Joram Barrez
 * @desc  该类负责管理所有的流程引擎  ProcessEngine 集合 ,并负责流程引擎实例对象的注册 ,获取 注销等操作
 *
 */
public abstract class ProcessEngines  {
  
  private static Logger log = LoggerFactory.getLogger(ProcessEngines.class);
  
  public static final String NAME_DEFAULT = "default"; //流程引擎默认的名称
  
  protected static boolean isInitialized = false;  //判断 ProcessEngine对象是否已经被初始化
  protected static Map<String, ProcessEngine> processEngines = new HashMap<String, ProcessEngine>(); //存放processEngine 的map key为 流程引擎名称  value为 流程引擎对象
  protected static Map<String, ProcessEngineInfo> processEngineInfosByName = new HashMap<String, ProcessEngineInfo>();
  protected static Map<String, ProcessEngineInfo> processEngineInfosByResourceUrl = new HashMap<String, ProcessEngineInfo>();
  protected static List<ProcessEngineInfo> processEngineInfos = new ArrayList<ProcessEngineInfo>();
  
  /** Initializes all process engines that can be found on the classpath for 
   * resources <code>activiti.cfg.xml</code> (plain Activiti style configuration)
   * and for resources <code>activiti-context.xml</code> (Spring style configuration). */
  public synchronized static void init() {
    if (!isInitialized()) { //再次判断流程引擎是否初始化,如果确认没有初始化 执行后续操作 ,否则不予处理
      if(processEngines == null) {//如果集合为空
        // Create new map to store process-engines if current map is null
        processEngines = new HashMap<String, ProcessEngine>();         //开始初始化该集合
        /*该集合为Map数据结构,key为String类型, 用户存储流程引擎的名称 value为ProcessEngine对象
        * 也即 如果流程引擎名称相同 则只会存储一份ProcessEngines实例对象
        * */
      }

      ClassLoader classLoader = ReflectUtil.getClassLoader(); // 获取类加载器
      Enumeration<URL> resources = null;
      try {
        /*
        通过类加载器 定位 配置文件
        也即 该XML 必须位于 根目录的 classpath下
         */
        resources = classLoader.getResources("activiti.cfg.xml"); //定位activiti.cfg.xml 文件
      } catch (IOException e) {
        throw new ActivitiIllegalArgumentException("problem retrieving activiti.cfg.xml resources on the classpath: "+System.getProperty("java.class.path"), e);
      }
      
      // Remove duplicated configuration URL's using set. Some classloaders may return identical URL's twice, causing duplicate startups
      Set<URL> configUrls = new HashSet<URL>();
      while (resources.hasMoreElements()) { //
        configUrls.add( resources.nextElement() ); // 添加到configUrls集合
      }
      for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
        URL resource = iterator.next();//循环遍历
        log.info("Initializing process engine using configuration '{}'",  resource.toString());
        /*
        调用此() 构造ProcessEngine对象
        初始化流程引擎 Activiti配置风格
         */
        initProcessEnginFromResource(resource);
      }
      
      try {
        /*
        定位activiti-context.xml 文件的位置
         */
        resources = classLoader.getResources("activiti-context.xml"); //activiti-context.xml
      } catch (IOException e) {
        throw new ActivitiIllegalArgumentException("problem retrieving activiti-context.xml resources on the classpath: "+System.getProperty("java.class.path"), e);
      }
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        log.info("Initializing process engine using Spring configuration '{}'",  resource.toString());
        /*
        如果定位到那么直接 调用此() 构造ProcessEngine对象

         */
        initProcessEngineFromSpringResource(resource); //初始化流程引擎
      }
      /* 如果该()执行, 则表明ProcessEngine对象工作完毕*/
      setInitialized(true);
    } else {
      log.info("Process engines already initialized");
    }
  }
  /*
  初始化流程引擎之 Spring配置风格--->  主要是activiti-context.xml 构造ProcessEngine的整个过程
  入参是一个 URL 参数
   */
  protected static void initProcessEngineFromSpringResource(URL resource) {
    try {
      // 委托ReflectUtil 静态() loadClass 加载 SpringConfigurationHelper 类 该类位于 activitii-spring-5.21jar包中
      Class< ? > springConfigurationHelperClass = ReflectUtil.loadClass("org.activiti.spring.SpringConfigurationHelper");
      // 反射获取此类对象 然后进行  调用buildProcessEngine()
      Method method = springConfigurationHelperClass.getDeclaredMethod("buildProcessEngine", new Class<?>[]{URL.class});
      //使用反射调用  buildProcessEngine() 然后使用  processEngine变量 存储该() 返回值
      ProcessEngine processEngine = (ProcessEngine) method.invoke(null, new Object[]{resource});
      //获取流程引擎名称
      String processEngineName = processEngine.getName();
      //
      ProcessEngineInfo processEngineInfo = new ProcessEngineInfoImpl(processEngineName, resource.toString(), null);
      //上述操作完成之后 ,将流程引擎的详细信息添加到 这两个集合中
      processEngineInfosByName.put(processEngineName, processEngineInfo);
      processEngineInfosByResourceUrl.put(resource.toString(), processEngineInfo);
      
    } catch (Exception e) {
      throw new ActivitiException("couldn't initialize process engine from spring configuration resource "+resource.toString()+": "+e.getMessage(), e);
    } 
  }
 
  /**
   * Registers the given process engine. No {@link ProcessEngineInfo} will be 
   * available for this process engine. An engine that is registered will be closed
   * when the {@link ProcessEngines#destroy()} is called.
   *
   * 注册流程引擎的()
   * 从此() 中可以看出 相同名称的流程引擎只会存在一个实例, 因为 是Map结构
   *
   */
  public static void registerProcessEngine(ProcessEngine processEngine) {
    processEngines.put(processEngine.getName(), processEngine);
  }
  
  /**
   * Unregisters the given process engine.
   * 移除
   */
  public static void unregister(ProcessEngine processEngine) {
    processEngines.remove(processEngine.getName());
  }
  /*
  初始化流程引擎之Activiti 默认配置风格  ---> 此() 负责解析 activiti.cfg.xml文件中配置的bena信息

  进入此() 之后 Activiti并不着急构造 ProcessEngine对象 而是先做很多准备工作
   */
  private static ProcessEngineInfo initProcessEnginFromResource(URL resourceUrl) {
    /*
    根据resourceUrl 参数从ProcessEngineInfosByResourceUrl集合中获取元素值 ,
    如果获取到,则从processEngineInfos 集合中移除该元素         processEngineInfos.remove(processEngineInfo);
     */
    ProcessEngineInfo processEngineInfo = processEngineInfosByResourceUrl.get(resourceUrl.toString());
    if (processEngineInfo!=null) {
      //
      processEngineInfos.remove(processEngineInfo);
      //判断此对象是否存在异常信息,
      if (processEngineInfo.getException()==null) {
        //如果不存在异常信息, 从processEngines中移除 该元素
        String processEngineName = processEngineInfo.getName();
        //从此集合中移除该元素
        processEngines.remove(processEngineName);
        //从此集合中移除该元素
        processEngineInfosByName.remove(processEngineName);
      }
    //最后无论 有没有异常信息 都 从processEngineInfosByResourceUrl 中移除次元素
      processEngineInfosByResourceUrl.remove(processEngineInfo.getResourceUrl());
    }
    String resourceUrlString = resourceUrl.toString();
    try {
      log.info("initializing process engine for resource {}", resourceUrl);
      /*
      将构造ProcessEngine对象的工作交给buildProcessEngine() 执行
       */
      ProcessEngine processEngine = buildProcessEngine(resourceUrl);
      String processEngineName = processEngine.getName(); //首先获取流程引擎名称
      log.info("initialised process engine {}", processEngineName);
      //并实例化 ProcessEngineInfoImpl 类
      processEngineInfo = new ProcessEngineInfoImpl(processEngineName, resourceUrlString, null);
      //然后 分别对集合  进行添加操作  processEngineInfosByName,processEngineInfosByResourceUrl , processEngineInfos
      processEngines.put(processEngineName, processEngine);
      processEngineInfosByName.put(processEngineName, processEngineInfo);
    } catch (Throwable e) {
      log.error("Exception while initializing process engine: {}", e.getMessage(), e);
      processEngineInfo = new ProcessEngineInfoImpl(null, resourceUrlString, getExceptionString(e));
    }
    processEngineInfosByResourceUrl.put(resourceUrlString, processEngineInfo);
    processEngineInfos.add(processEngineInfo);
    return processEngineInfo;
  }

  private static String getExceptionString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }
  /*
    Activiti 构造流程引擎的()
   */
  private static  ProcessEngine buildProcessEngine(URL resource) {
    InputStream inputStream = null;
    try {
      //打开配置文件的数据流
      inputStream = resource.openStream();
      /*
      构造ProcessEngineConfiguration 对象 ,
       */
      ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromInputStream(inputStream);
      //用此对象的buildProcessEngine 构造 ProcessEngine 对象
      return processEngineConfiguration.buildProcessEngine();
    } catch (IOException e) {
      throw new ActivitiIllegalArgumentException("couldn't open resource stream: "+e.getMessage(), e);
    } finally {
      IoUtil.closeSilently(inputStream);
    }
  }
  
  /** Get initialization results. */
  public static List<ProcessEngineInfo> getProcessEngineInfos() {
    return processEngineInfos;
  }

  /** Get initialization results. Only info will we available for process engines
   * which were added in the {@link ProcessEngines#init()}. No {@link ProcessEngineInfo}
   * is available for engines which were registered programatically.
  */
  public static ProcessEngineInfo getProcessEngineInfo(String processEngineName) {
    return processEngineInfosByName.get(processEngineName);
  }
  /*
    也可以为 getDefaultProcessEngine() 传递参数,   参数的含义, 流程引擎的名称 默认为default

   */
  public static ProcessEngine getDefaultProcessEngine() {
    return getProcessEngine(NAME_DEFAULT);
  }

  /** obtain a process engine by name.  
   * @param processEngineName is the name of the process engine or null for the default process engine.
   *
   * 此() 需要显式的设置流程引擎的名称,因此开发者可以直接调用该() 构造流程引擎对象
   * */
  public static ProcessEngine getProcessEngine(String processEngineName) {
    if (!isInitialized()) {   //判断 ProcessEngine对象是否已经被初始化
      init(); //如果没有初始化  调用init 初始化 ProcessEngine类
    }
    return processEngines.get(processEngineName); //如果初始化了 直接 从 ProcessEngines集合中 查找ProcessEngine对象并作为该()返回值,通过该操作可以知道,Activiti可以支持多个ProcessEngine对象一起运行
//    只要流程引擎的名称不一样即可, 相同名称的流程引擎只会存在一个流程引擎实例,因为 processEngines 为Map数据结构   流程引擎的默认值为default

  }
  
  /** retries to initialize a process engine that previously failed.
   */
  public static ProcessEngineInfo retry(String resourceUrl) {
    log.debug("retying initializing of resource {}", resourceUrl);
    try {
      return initProcessEnginFromResource(new URL(resourceUrl));
    } catch (MalformedURLException e) {
      throw new ActivitiIllegalArgumentException("invalid url: "+resourceUrl, e);
    }
  }
  
  /** provides access to process engine to application clients in a 
   * managed server environment.  
   */
  public static Map<String, ProcessEngine> getProcessEngines() {
    return processEngines;
  }
  
  /** closes all process engines.  This method should be called when the server shuts down. */
  public synchronized static void destroy() {
    if (isInitialized()) {
      Map<String, ProcessEngine> engines = new HashMap<String, ProcessEngine>(processEngines);
      processEngines = new HashMap<String, ProcessEngine>();
      
      for (String processEngineName: engines.keySet()) {
        ProcessEngine processEngine = engines.get(processEngineName);
        try {
          processEngine.close();
        } catch (Exception e) {
          log.error("exception while closing {}", (processEngineName==null ? "the default process engine" : "process engine "+processEngineName), e);
        }
      }
      
      processEngineInfosByName.clear();
      processEngineInfosByResourceUrl.clear();
      processEngineInfos.clear();
      
      setInitialized(false);
    }
  }
  
  public static boolean isInitialized() {
    return isInitialized;
  }
  
  public static void setInitialized(boolean isInitialized) {
    ProcessEngines.isInitialized = isInitialized;
  }
}
