1)在实际项目开发中如果开发人员觉得ProcessEngineConfigurationImpl类的初始化
例如initBeans() 不满足业务需求,则可以自定义一个类 继承SpringProcessEngineConfiguration
然后重写initBeans()
2) 自定义配置器,   继承 AbstractProcessEngineConfigurator  然后注入配置类即可 配置器是升序排序的
3)   如果打算在引擎创建 ,删除 ,更新表的时候 执行项目中的DDL 脚本
     可以通过扩展DbSqlSession 进行实现
4) 自定义 流程引擎生命周期监听器 (ProcessEngineLifecycleListener)
5) 自定义模型校验器   extends ProcessLevelValidator
    自定义完了之后 还要 重写 模型校验器工厂的 createDefaultProcessValidator
    将我们自定义的校验器  添加进去
     ValidatorSet validatorSet = new validatorset("validate usertask assigne");
     validatorset.addvalidator(new usertaskvalidator());
6) 自定义元素解析器   例如startevnet     重写它的   starteventxmlconverter(开始节点的默认解析器)
7) 自定义一个日志监听器 并且继承eventlogger类
   		然后通过 addeventhandler() 将 期望处理的事件 以及该事件对应的日志处理器, 添加到eventhandlers 集合中 即可
    自定义日志清洗器 将 act_evt_log的数据  入到es中  继承 abstracteventflusher类
8) 自定义部署器 (前后置)   bpmndeployer,  或者  deployer
9) 自定义流程定义缓存处理类 实现 deploymentcache<processdefinitionentity>
10) 自定义节点缓存类
    继承 processdefinitioninfocache
11) 自定义作业处理器  比方对 TimerStartEventJobHandler进行扩展 就继承它
12) 自定义 对象解析器   比方对 UserTaskParseHandler
13) 自定义代理类  控制监听器是否可以执行,  实际项目中可以通过该特性对废弃的监听器进行屏蔽
14)自定义全局任务监听器  比方对 UserTaskParseHandler  只是重写 userTask的 executeParse()















