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

package org.activiti.engine.impl.pvm.process;


/**
 * Marks implementing class as having DI-information bounded by a rectangle
 * at a certain location.
 * 
 * @author Frederik Heremans
 * 提供了获取和设置元素的长度,宽度,以及X,Y 坐标信息的()
 * 元素的坐标以及长宽值, 都存储在流程文档中
 * 如果使用图形化工具设计流程文档, 则绘制流程文档时 ,会自动生成元素相应的坐标值以及长宽值
 */
public interface HasDIBounds {

  int getWidth();
  int getHeight();
  int getX();
  int getY();
  
  void setWidth(int width);
  void setHeight(int height);
  void setX(int x);
  void setY(int y);
}
