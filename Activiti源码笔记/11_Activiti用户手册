所有包名中包含 .impl. 的类都是内部实现类，都是不保证稳定的。
内部实现类
在jar包中，所有包名中包含.impl.（比如：org.activiti.engine.impl.pvm.delegate）的类都是实现类， 它们应该被视为流程引擎内部的类。对于这些类和接口都不能够保证其稳定性。

注意：只有使用了mail service task才必须引入mail依赖jar。



Activiti数据库类型	JDBC URL实例	备注
h2	jdbc:h2:tcp://localhost/activiti	默认配置的数据库
mysql	jdbc:mysql://localhost:3306/activiti?autoReconnect=true	使用mysql-connector-java驱动测试
oracle	jdbc:oracle:thin:@localhost:1521:xe
postgres	jdbc:postgresql://localhost:5432/activiti
db2	jdbc:db2://localhost:50000/activiti
mssql	jdbc:sqlserver://localhost:1433/activiti


不过，一般情况只有数据库管理员才能执行DDL语句。 在生产环境，这也是最明智的选择。 SQL DDL语句可以从Activiti下载页或Activiti发布目录里找到，在database子目录下。 脚本也包含在引擎的jar中(activiti-engine-x.jar)， 在org/activiti/db/create包下（drop目录里是删除语句）。 SQL文件的命名方式如下

activiti.{db}.{create|drop}.{type}.sql
其中 db 是 支持的数据库， type 是

engine: 引擎执行的表。必须。

identity: 包含用户，群组，用户与组之间的关系的表。 这些表是可选的，只有使用引擎自带的默认身份管理时才需要。

history: 包含历史和审计信息的表。可选的：历史级别设为none时不会使用。 注意这也会引用一些需要把数据保存到历史表中的功能（比如任务的评论）。

MySQL用户需要注意： 版本低于5.6.4的MySQL不支持毫秒精度的timstamp或date类型。 更严重的是，有些版本会在尝试创建这样一列时抛出异常，而有些版本则不会。 在执行自动创建/更新时，引擎会在执行过程中修改DDL。 当使用DDL时，可以选择通用版本和名为mysql55的文件。 （它适合所有版本低于5.6.4的情况）。 后一个文件会将列的类型设置为没有毫秒的情况。

总结一下，对于MySQL版本会执行如下操作

<5.6: 不支持毫秒精度。可以使用DDL文件（包含mysql55的文件）。可以实现自动创建/更新。

5.6.0 - 5.6.3: 不支持毫秒精度。无法自动创建/更新。建议更新到新的数据库版本。如果真的需要的话，也可以使用mysql 5.5。

5.6.4+:支持毫秒精度。可以使用DDL文件（默认包含mysql的文件）。可以实现自动创建、更新。

注意对于已经更新了MySQL数据库，而且Activiti表已经创建/更新的情况， 必须手工修改列的类型。


理解数据库表的命名
Activiti的表都以ACT_开头。 第二部分是表示表的用途的两个字母标识。 用途也和服务的API对应。

ACT_RE_*: 'RE'表示repository。 这个前缀的表包含了流程定义和流程静态资源 （图片，规则，等等）。

ACT_RU_*: 'RU'表示runtime。 这些运行时的表，包含流程实例，任务，变量，异步任务，等运行中的数据。 Activiti只在流程实例执行过程中保存这些数据， 在流程结束时就会删除这些记录。 这样运行时表可以一直很小速度很快。

ACT_ID_*: 'ID'表示identity。 这些表包含身份信息，比如用户，组等等。

ACT_HI_*: 'HI'表示history。 这些表包含历史数据，比如历史流程实例， 变量，任务等等。

ACT_GE_*: 通用数据， 用于不同场景下。



数据库升级
在执行更新之前要先备份数据库 （使用数据库的备份功能）

默认，每次构建流程引擎时都会进行版本检测。 这一切都在应用启动或Activiti webapp启动时发生。 如果Activiti发现数据库表的版本与依赖库的版本不同， 就会抛出异常。

要升级，你要把下面的配置 放到activiti.cfg.xml配置文件里：

<beans ... >

  <bean id="processEngineConfiguration" class="org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration">
    <!-- ... -->
    <property name="databaseSchemaUpdate" value="true" />
    <!-- ... -->
  </bean>

</beans>
然后，把对应的数据库驱动放到classpath里。 升级应用的Activiti依赖。启动一个新版本的Activiti 指向包含旧版本的数据库。将databaseSchemaUpdate设置为true， Activiti会自动将数据库表升级到新版本， 当发现依赖和数据库表版本不通过时。

也可以执行更新升级DDL语句。 也可以执行数据库脚本，可以在Activiti下载页找到。


为表达式和脚本暴露配置
默认，activiti.cfg.xml和你自己的Spring配置文件中所有bean 都可以在表达式和脚本中使用。 如果你想限制配置文件中的bean的可见性， 可以配置流程引擎配置的beans配置。 ProcessEngineConfiguration的beans是一个map。当你指定了这个参数， 只有包含这个map中的bean可以在表达式和脚本中使用。 通过在map中指定的名称来决定暴露的bean。


从Activiti 5.12开始，SLF4J被用作日志框架，替换了之前使用java.util.logging。 所有日志（activiti, spring, mybatis等等）都转发给SLF4J 允许使用你选择的日志实现。

默认activiti-engine依赖中没有提供SLF4J绑定的jar， 需要根据你的实际需要使用日志框架。如果没有添加任何实现jar，SLF4J会使用NOP-logger，不使用任何日志，不会发出警告，而且什么日志都不会记录。 可以通过http://www.slf4j.org/codes.html#StaticLoggerBinder了解这些实现。

使用Maven，比如使用一个依赖（这里使用log4j），注意你还需要添加一个version：

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-log4j12</artifactId>
</dependency>
activiti-explorer和activiti-rest应用都使用了Log4j绑定。执行所有activiti-*模块的单元测试页使用了Log4j。

特别提醒如果容器classpath中存在commons-logging： 为了把spring日志转发给SLF4J，需要使用桥接（参考http://www.slf4j.org/legacy.html#jclOverSLF4J）。 如果你的容器提供了commons-logging实现，请参考下面网页：http://www.slf4j.org/codes.html#release来确保稳定性。

使用Maven的实例（忽略版本）：

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>jcl-over-slf4j</artifactId>
</dependency>

映射诊断上下文
在5.13中，activiti支持slf4j的MDC功能。 如下的基础信息会传递到日志中记录：

流程定义Id标记为mdcProcessDefinitionID

流程实例Id标记为mdcProcessInstanceID

分支Id标记为mdcexecutionId

默认不会记录这些信息。可以配置日志使用期望的格式来显示它们，扩展通常的日志信息。 比如，下面的log4j配置定义会让日志显示上面提及的信息：

 log4j.appender.consoleAppender.layout.ConversionPattern =ProcessDefinitionId=%X{mdcProcessDefinitionID}
executionId=%X{mdcExecutionId} mdcProcessInstanceID=%X{mdcProcessInstanceID} mdcBusinessKey=%X{mdcBusinessKey} %m%n"

当系统进行高风险任务，日志必须严格检查时，这个功能就非常有用，比如要使用日志分析的情况。



把事件监听器配置到流程引擎配置中时，会在流程引擎启动时激活，并在引擎启动启动中持续工作着。

eventListeners属性需要org.activiti.engine.delegate.event.ActivitiEventListener的队列。 通常，我们可以声明一个内部的bean定义，或使用ref引用已定义的bean。 下面的代码，向配置添加了一个事件监听器，任何事件触发时都会提醒它，无论事件是什么类型：

<bean id="processEngineConfiguration" class="org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration">
    ...
    <property name="eventListeners">
      <list>
         <bean class="org.activiti.engine.example.MyEventListener" />
      </list>
    </property>
</bean>
为了监听特定类型的事件，可以使用typedEventListeners属性，它需要一个map参数。 map的key是逗号分隔的事件名（或单独的事件名）。 map的value是org.activiti.engine.delegate.event.ActivitiEventListener队列。 下面的代码演示了向配置中添加一个事件监听器，可以监听job执行成功或失败：

<bean id="processEngineConfiguration" class="org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration">
    ...
    <property name="typedEventListeners">
      <map>
        <entry key="JOB_EXECUTION_SUCCESS,JOB_EXECUTION_FAILURE" >
          <list>
            <bean class="org.activiti.engine.example.MyJobEventListener" />
          </list>
        </entry>
      </map>
    </property>
</bean>
分发事件的顺序是由监听器添加时的顺序决定的。首先，会调用所有普通的事件监听器（eventListeners属性），按照它们在list中的次序。 然后，会调用所有对应类型的监听器（typedEventListeners属性），如果对应类型的事件被触发了。


在运行阶段添加监听器
可以通过API（RuntimeService）在运行阶段添加或删除额外的事件监听器：

/**
 * Adds an event-listener which will be notified of ALL events by the dispatcher.
 * @param listenerToAdd the listener to add
 */
void addEventListener(ActivitiEventListener listenerToAdd);

/**
 * Adds an event-listener which will only be notified when an event occurs, which type is in the given types.
 * @param listenerToAdd the listener to add
 * @param types types of events the listener should be notified for
 */
void addEventListener(ActivitiEventListener listenerToAdd, ActivitiEventType... types);

/**
 * Removes the given listener from this dispatcher. The listener will no longer be notified,
 * regardless of the type(s) it was registered for in the first place.
 * @param listenerToRemove listener to remove
 */
 void removeEventListener(ActivitiEventListener listenerToRemove);
注意运行期添加的监听器引擎重启后就消失了。

为流程定义添加监听器
可以为特定流程定义添加监听器。监听器只会监听与这个流程定义相关的事件，以及这个流程定义上发起的所有流程实例的事件。 监听器实现可以使用，全类名定义，引用实现了监听器接口的表达式，或配置为抛出一个message/signal/error的BPMN事件。

让监听器执行用户定义的逻辑
下面代码为一个流程定义添加了两个监听器。第一个监听器会接收所有类型的事件，它是通过全类名定义的。 第二个监听器只接收作业成功或失败的事件，它使用了定义在流程引擎配置中的beans属性中的一个bean。

<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener class="org.activiti.engine.test.MyEventListener" />
    <activiti:eventListener delegateExpression="${testEventListener}" events="JOB_EXECUTION_SUCCESS,JOB_EXECUTION_FAILURE" />
  </extensionElements>

  ...

</process>
对于实体相关的事件，也可以设置为针对某个流程定义的监听器，实现只监听发生在某个流程定义上的某个类型实体事件。 下面的代码演示了如何实现这种功能。可以用于所有实体事件（第一个例子），也可以只监听特定类型的事件（第二个例子）。

<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener class="org.activiti.engine.test.MyEventListener" entityType="task" />
    <activiti:eventListener delegateExpression="${testEventListener}" events="ENTITY_CREATED" entityType="task" />
  </extensionElements>

  ...

</process>
entityType支持的值有：attachment, comment, execution,identity-link, job, process-instance, process-definition, task。



监听抛出BPMN事件
[试验阶段]

另一种处理事件的方法是抛出一个BPMN事件。请注意它只针对与抛出一个activiti事件类型的BPMN事件。 比如，抛出一个BPMN事件，在流程实例删除时，会导致一个错误。 下面的代码演示了如何在流程实例中抛出一个signal，把signal抛出到外部流程（全局），在流程实例中抛出一个消息事件， 在流程实例中抛出一个错误事件。除了使用class或delegateExpression， 还使用了throwEvent属性，通过额外属性，指定了抛出事件的类型。

<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener throwEvent="signal" signalName="My signal" events="TASK_ASSIGNED" />
  </extensionElements>
</process>
<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener throwEvent="globalSignal" signalName="My signal" events="TASK_ASSIGNED" />
  </extensionElements>
</process>
<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener throwEvent="message" messageName="My message" events="TASK_ASSIGNED" />
  </extensionElements>
</process>
<process id="testEventListeners">
  <extensionElements>
    <activiti:eventListener throwEvent="error" errorCode="123" events="TASK_ASSIGNED" />
  </extensionElements>
</process>
如果需要声明额外的逻辑，是否抛出BPMN事件，可以扩展activiti提供的监听器类。在子类中重写isValidEvent(ActivitiEvent event)， 可以防止抛出BPMN事件。对应的类是org.activiti.engine.test.api.event.SignalThrowingEventListenerTest, org.activiti.engine.impl.bpmn.helper.MessageThrowingEventListener 和 org.activiti.engine.impl.bpmn.helper.ErrorThrowingEventListener.

流程定义中监听器的注意事项
事件监听器只能声明在process元素中，作为extensionElements的子元素。 监听器不能定义在流程的单个activity下。

delegateExpression中的表达式无法访问execution上下文，这与其他表达式不同（比如gateway）。 它只能引用定义在流程引擎配置的beans属性中声明的bean，或者使用spring（未使用beans属性）中所有实现了监听器接口的spring-bean。

在使用监听器的 class 属性时，只会创建一个实例。记住监听器实现不会依赖成员变量， 确认是多线程安全的。

当一个非法的事件类型用在events属性或throwEvent中时，流程定义发布时就会抛出异常。（会导致部署失败）。如果class或delegateExecution由问题（类不存在，不存在的bean引用，或代理类没有实现监听器接口），会在流程启动时抛出异常（或在第一个有效的流程定义事件被监听器接收时）。所以要保证引用的类正确的放在classpath下，表达式也要引用一个有效的实例。


通过API分发事件
我们提供了通过API使用事件机制的方法，允许大家触发定义在引擎中的任何自定义事件。 建议（不强制）只触发类型为CUSTOM的ActivitiEvents。可以通过RuntimeService触发事件：

/**
 * Dispatches the given event to any listeners that are registered.
 * @param event event to dispatch.
 *
 * @throws ActivitiException if an exception occurs when dispatching the event or when the {@link ActivitiEventDispatcher}
 * is disabled.
 * @throws ActivitiIllegalArgumentException when the given event is not suitable for dispatching.
 */
 void dispatchEvent(ActivitiEvent event);






 //////////////////
 有两种方法可以从引擎中查询数据：查询API和原生查询。查询API提供了完全类型安全的API。 你可以为自己的查询条件添加很多条件 （所以条件都以AND组合）和精确的排序条件。下面的代码展示了一个例子：

       List<Task> tasks = taskService.createTaskQuery()
          .taskAssignee("kermit")
          .processVariableValueEquals("orderId", "0815")
          .orderByDueDate().asc()
          .list();

 有时，你需要更强大的查询，比如使用OR条件或不能使用查询API实现的条件。 这时，我们推荐原生查询，
 它让你可以编写自己的SQL查询。 返回类型由你使用的查询对象决定，数据会映射到正确的对象上。
 比如，任务，流程实例，，执行，等等。 因为查询会作用在数据库上，你必须使用数据库中定义的表名和列名；
 这要求了解内部数据结构， 因此使用原生查询时一定要注意。表名可以通过API获得，可以尽量减少对数据库的依赖。

       List<Task> tasks = taskService.createNativeTaskQuery()
         .sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = #{taskName}")
         .parameter("taskName", "gonzoTask")
         .list();

       long count = taskService.createNativeTaskQuery()
         .sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T1, "
                + managementService.getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_")
         .count();





表达式
Activiti使用UEL处理表达式。UEL即统一表达式语言，它时EE6规范的一部分（参考 EE6规范）。为了在所有运行环境都支持最新UEL的所有功能，我们使用了一个JUEL的修改版本。

表达式可以用在很多场景下，比如Java服务任务，执行监听器，任务监听器和条件流。 虽然有两重表达式，值表达式和方法表达式，Activiti进行了抽象，所以两者可以同样使用在需要表达式的场景中。

Value expression：解析为值。默认，所有流程变量都可以使用。所有spring bean（spring环境中）也可以使用在表达式中。 一些实例：

${myVar}
${myBean.myProperty}
Method expression：调用一个方法，使用或不使用参数。当调用一个无参数的方法时，记得在方法名后添加空的括号（以区分值表达式）。 传递的参数可以是字符串也可以是表达式，它们会被自动解析。例子：

${printer.print()}
${myBean.addNewOrder('orderName')}
${myBean.doSomething(myVar, execution)}
注意这些表达式支持解析原始类型（包括比较），bean，list，数组和map。

在所有流程实例中，表达式中还可以使用一些默认对象：

execution：DelegateExecution提供外出执行的额外信息。

task：DelegateTask提供当前任务的额外信息。注意，只对任务监听器的表达式有效。

authenticatedUserId：当前登录的用户id。如果没有用户登录，这个变量就不可用。




ProcessEngine是线程安全的， 可以在多线程下共享。在web应用中， 意味着可以在容器启动时创建流程引擎， 在容器关闭时关闭流程引擎。

下面代码演示了如何编写一个ServletContextListener 在普通的Servlet环境下初始化和销毁流程引擎：

public class ProcessEnginesServletContextListener implements ServletContextListener {

  public void contextInitialized(ServletContextEvent servletContextEvent) {
    ProcessEngines.init();
  }

  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    ProcessEngines.destroy();
  }

}
contextInitialized方法会执行ProcessEngines.init()。 这会查找classpath下的activiti.cfg.xml文件， 根据配置文件创建一个ProcessEngine（比如，多个jar中都包含配置文件）。 如果classpath中包含多个配置文件，确认它们有不同的名字。 当需要使用流程引擎时，可以通过

ProcessEngines.getDefaultProcessEngine()
或

ProcessEngines.getProcessEngine("myName");
。 当然，也可以使用其他方式创建流程引擎， 可以参考配置章节中的描述。

ContextListener中的contextDestroyed方法会执行ProcessEngines.destroy(). 这会关闭所有初始化的流程引擎。




关于Activiti 接管的 SpringBean
表达式
当使用ProcessEngineFactoryBean时候，默认情况下，在BPMN流程中的所有表达式都将会'看见'所有的Spring beans。
 它可以限制你在表达式中暴露出的beans或者甚至可以在你的配置中使用一个Map不暴露任何beans。下面的例子暴露了一个单例bean（printer），可以把"printer"当作关键字使用. 想要不暴露任何beans，仅仅只需要在SpringProcessEngineConfiguration中传递一个空的list作为'beans'的属性。当不设置'beans'的属性时，在应用上下文中Spring beans都是可以使用的。

<bean id="processEngineConfiguration" class="org.activiti.spring.SpringProcessEngineConfiguration">
  ...
  <property name="beans">
    <map>
      <entry key="printer" value-ref="printer" />
    </map>
  </property>
</bean>

  <bean id="printer" class="org.activiti.examples.spring.Printer" />

现在暴露出来的beans就可以在表达式中使用：例如，在SpringTransactionIntegrationTest中的 hello.bpmn20.xml
展示的是如何使用UEL方法表达式去调用Spring bean的方法：

<definitions id="definitions" ...>

  <process id="helloProcess">

    <startEvent id="start" />
    <sequenceFlow id="flow1" sourceRef="start" targetRef="print" />

    <serviceTask id="print" activiti:expression="#{printer.printMessage()}" />
    <sequenceFlow id="flow2" sourceRef="print" targetRef="end" />

    <endEvent id="end" />

  </process>

</definitions>
这里的 Printer 看起来像这样：

public class Printer {

  public void printMessage() {
    System.out.println("hello world");
  }
}
并且Spring bean的配置（如上文所示）看起来像这样：

<beans ...>
  ...

  <bean id="printer" class="org.activiti.examples.spring.Printer" />

</beans>










