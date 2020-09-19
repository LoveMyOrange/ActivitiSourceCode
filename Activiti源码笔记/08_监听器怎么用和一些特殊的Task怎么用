1)ServiceTask 怎么用???
ServiceTask Description 描述
Java 服务任务用来调用外部 Java 类

XML representation 内容
有4钟方法来声明 java 调用逻辑：

实现 JavaDelegate 或 ActivityBehavior
执行解析代理对象的表达式
调用一个方法表达式
调用一直值表达式
执行一个在流程执行中调用的类， 需要在'activiti:class'属性中设置全类名，这个类设置后，流程执行到serviceTask这一步时就会自动调用这个java类。

<serviceTask id="javaService"  name="My Java Service Task"
    activiti:class="com.yang.activiti.demo.service.ServiceTaskService" />
注意，如果使用了spring mvc框架，默认配置的 activiti:class 是不能通过注解注入类的，如果想要通过注解注入java类，需要使用如下语法

<serviceTask id="serviceTask" activiti:delegateExpression="${serviceTaskService}" />
serviceTaskService是一个实现了 JavaDelegate 接口的bean， 它定义在实例的 spring 容器中

serviceTask还可以注入多个参数

<serviceTask id="ServiceTaskService"
    name="ServiceTaskService"
    activiti:class="com.yang.activiti.demo.service.ServiceTaskService">
  <extensionElements>
    <activiti:field name="param1">
        <activiti:string> Hello World</activiti:string>
    </activiti:field>
  <activiti:field name="param2">
        <activiti:string> Hello World</activiti:string>
    </activiti:field>
  </extensionElements>
</serviceTask>
java后台com.yang.activiti.demo.service.ServiceTaskService

public class ServiceTaskService implements JavaDelegate {

  private Expression param1;
  private Expression param2;

  public void execute(DelegateExecution execution) {
    String value1 = (String) param1.getValue(execution);
    execution.setVariable("var1", new StringBuffer(value1).reverse().toString());

    String value2 = (String) param2.getValue(execution);
    execution.setVariable("var2", new StringBuffer(value2).reverse().toString());
  }
}
还有其他的参数属性去参照ServiceTask参考不得不说这个节点真的很灵活。

至此我发现了ServiceTask的两个重要特性：

1. 可以绑定自定义的java class（这不就是我想要的扩展自定义任务么）
2. 可以传递无限多个参数（这不就是我想要的扩展属性么）

基于ServiceTask可以有无限多的可能。
这样基本解决了以下两个大问题

作者：程序鱼
链接：https://www.jianshu.com/p/68c0034f8e56
来源：简书
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

2) 监听器

(1)Listener的生命周期是由servlet容器（例如tomcat）管理的，项目启动时上例中的ConfigListener是由servlet容器实例化并调用其contextInitialized方法，而servlet容器并不认得@Autowired注解，因此导致ConfigService实例注入失败。

(2)而spring容器中的bean的生命周期是由spring容器管理的。

注解的方式执行的位置，spring的注入是在filter和listener之后的，（顺序:listener >> filter >> servlet >> spring ）。

如果在监听器中有需要对容器中bean进行引用，就不能采用注解的方式了。只能手动的进行配置文件的读取

个人理解，上述过程中MyTaskListenre虽然存在于Spring容器中，但未被Spring容器所管理，所以@Autowired失效

作者：廖小明的赖胖子
链接：https://www.jianshu.com/p/56d5be0dab91
来源：简书
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。


    监听器常用的有3种
    如果用javaclass   BPMN上配置的是   全类名
        那么在这种监听器类中要获取Spring中的bean
            需要通过 SpringApplicationContext来获取 ,因为这种类的初始化底层通过反射,所以不能直接@AutoWired

    如果通过 delegateExpression
    1) 监听器类上 @Component("xxxListenr")  注入到SPring容器中
     BPMN上使用 delegateExpression  value使用   ${xxxListenr}    (使用的是Spring的beanNname)


expression：表达式创建。在表达式中指明需要调用的类及方法，且可以为需要调用的方法传人参数。
            形如${bean.doSomething(execution)}
















