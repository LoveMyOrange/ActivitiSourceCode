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

package org.activiti.spring;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Dave Syer
 * @author Christian Stettler
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Josh Long
 *  ProcessEngineFactoryBean 此类是什么时候被Spring初始化的呢???
 *  此工厂类负责生成 ProcessEngine 对象
 *  看此类的 getObject() 中
 *
 */
public class ProcessEngineFactoryBean implements FactoryBean<ProcessEngine>, DisposableBean, ApplicationContextAware {


    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    protected ApplicationContext applicationContext;
    protected ProcessEngine processEngine;


    public void destroy() throws Exception {
        if (processEngine != null) {
            processEngine.close();
        }
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    /*

     */
    public ProcessEngine getObject() throws Exception {
        configureExpressionManager();//设置表达式管理器
        configureExternallyManagedTransactions();//设置事务管理器

        if (processEngineConfiguration.getBeans() == null) {//设置bean
            processEngineConfiguration.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        }
        //开始构造ProcessEngine对象  .实现类是 SpringProcessEngineConfiguration
        this.processEngine = processEngineConfiguration.buildProcessEngine();
        return this.processEngine;
    }

    protected void configureExpressionManager() {
        if (processEngineConfiguration.getExpressionManager() == null && applicationContext != null) {
            processEngineConfiguration.setExpressionManager(
                    new SpringExpressionManager(applicationContext, processEngineConfiguration.getBeans()));
        }
    }
    /*

     */
    protected void configureExternallyManagedTransactions() {
        /*
        如果processEngineConfiguration 对象为 SpringProcessEngineConfiguration 实例 对象
        则进行如下处理,否则不予理会
        将processEngineConfiguration对象转化为SpringProcessEngineConfiguration
        如果engineConfiguration 中getTransactionManger() 不为空
        则将事务交给Spring管理.
        也就是说 如果要使用Spring 管理事务, 则流程引擎配置类 必须为SpringProcessEngineConfiguration
         */
        if (processEngineConfiguration instanceof SpringProcessEngineConfiguration) { // remark: any config can be injected, so we cannot have SpringConfiguration as member

            SpringProcessEngineConfiguration engineConfiguration = (SpringProcessEngineConfiguration) processEngineConfiguration;
            if (engineConfiguration.getTransactionManager() != null) {
                processEngineConfiguration.setTransactionsExternallyManaged(true);//交给Spring管理事务
            }
        }
    }

    public Class<ProcessEngine> getObjectType() {
        return ProcessEngine.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
}
