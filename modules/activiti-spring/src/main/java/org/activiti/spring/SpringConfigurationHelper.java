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

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.UrlResource;

import java.net.URL;
import java.util.Map;


/**
 * @author Tom Baeyens
 * @
 */
public class SpringConfigurationHelper {

    private static Logger log = LoggerFactory.getLogger(SpringConfigurationHelper.class);
    /*
        反射构造ProcessEngine
     */
    public static ProcessEngine buildProcessEngine(URL resource) {
        log.debug("==== BUILDING SPRING APPLICATION CONTEXT AND PROCESS ENGINE =========================================");
        //加载activiti-context.xml
        ApplicationContext applicationContext = new GenericXmlApplicationContext(new UrlResource(resource));
        /*
        获取类型为ProcessEngine的对象,

        如果activiti-context.xml 文件中没有定义类型为ProcessEngine的bean 则报错

        如果在activiti0context.xml中 配置了多个类型为ProcessEngine的bean
        则从        beansOfType  中 取出第一个ProcessEngine 作为()的返回值

        也就是不管流程配置文件中 定义了多少个,ProcessEngine类 程序只会使用一个而且是第一个

         */
        Map<String, ProcessEngine> beansOfType = applicationContext.getBeansOfType(ProcessEngine.class);
        if ((beansOfType == null)
                || (beansOfType.isEmpty())
                ) {
            throw new ActivitiException("no " + ProcessEngine.class.getName() + " defined in the application context " + resource.toString());
        }

        ProcessEngine processEngine = beansOfType.values().iterator().next();

        log.debug("==== SPRING PROCESS ENGINE CREATED ==================================================================");
        return processEngine;































    }


}
