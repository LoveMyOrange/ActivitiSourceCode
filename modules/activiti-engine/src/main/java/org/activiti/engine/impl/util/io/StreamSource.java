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
package org.activiti.engine.impl.util.io;

import java.io.InputStream;

import org.activiti.bpmn.converter.util.InputStreamProvider;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @desc
 * 该接口 继承 InputStreamProvider  该接口仅仅定义了 一个()  getInputStream()
 * 该方法 返回InputStream 对象
 * 不同来源的资源都有相应StreamSource 实现
 * 如 byte数组(ByteStreamSource
 *  Inputstream  (InputStreamSource)
 *  URL网络资源 (UrlStreamSource)
 *  classpath(ResourceStreamSource) 等
 */
public interface StreamSource extends InputStreamProvider {

  /**
   * Creates a <b>NEW</b> {@link InputStream} to the provided resource.
   * 因为 BPMNXMLConverter 类中的convertToBpmnModel() 需要 InputStreamProvider
   */
  InputStream getInputStream();
  
}
