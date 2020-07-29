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

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.interceptor.AbstractCommandInterceptor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.swing.*;

/**
 * @author Dave Syer
 * @author Tom Baeyens
 * 该类 用于接管Activiti事务传播行为  负责将Activit的事务传播行为转化为Spring中的  这样Spring框架就可以管理Activiti中的事务
 * 该类是一个Spring 事务拦截器
 *
 *
 */
public class SpringTransactionInterceptor extends AbstractCommandInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringTransactionInterceptor.class);
    /*
    *
    * */
    protected PlatformTransactionManager transactionManager;

    public SpringTransactionInterceptor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
/*
*
* */
    public <T> T execute(final CommandConfig config, final Command<T> command) {
        LOGGER.debug("Running command with propagation {}", config.getTransactionPropagation());
        //实例化事务模板类
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        //调用 () 将Activiti的事务传播行为转化为Spring的事务传播行为, 并且将转化之后的事务传播行为类型 设置到 事务模板类中
        transactionTemplate.setPropagationBehavior(getPropagation(config));
        //开始执行当前命令拦截器中的下一个命令拦截器,
        T result = transactionTemplate.execute(new TransactionCallback<T>() {
            public T doInTransaction(TransactionStatus status) {
                return next.execute(config, command);
            }
        });

        return result;
    }

    private int getPropagation(CommandConfig config) {
        /*
        Spring 框架 会获取 CommandConfig 类中定义的所有事务传播行为
        然后转化为SPring框架支持的行为
        Activiti 只支持这3种事务传播行为
         */
        switch (config.getTransactionPropagation()) {
            case NOT_SUPPORTED:
                return TransactionTemplate.PROPAGATION_NOT_SUPPORTED;
            case REQUIRED:
                return TransactionTemplate.PROPAGATION_REQUIRED;
            case REQUIRES_NEW:
                return TransactionTemplate.PROPAGATION_REQUIRES_NEW;
            default:
                throw new ActivitiIllegalArgumentException("Unsupported transaction propagation: " + config.getTransactionPropagation());
        }
    }
}














