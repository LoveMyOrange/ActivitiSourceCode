<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:activiti="http://activiti.org/bpmn" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" typeLanguage="http://www.w3.org/2001/XMLSchema" expressionLanguage="http://www.w3.org/1999/XPath" targetNamespace="http://www.activiti.org/processdef">
  <process id="ownChannelThreeShopMainProcess" name="自有建店-三专店主流程" isExecutable="true">
    <startEvent id="start" name="开始"></startEvent>
    <userTask id="sid-3FD87B95-DDC9-43E0-9A46-96F10154595C" name="平面图上传|广告公司" activiti:assignee="${restrictDim.advertisingAgency}"></userTask>
    <userTask id="sid-B953A106-1A2C-47C7-AD4D-DB65C0FEA30E" name="平面图审核|网络总监" activiti:assignee="wlzjyubei_new,ZX_WLZJ_new|mkt:${restrictDim.mkt}" activiti:formKey="{&quot;pc&quot;:&quot;/hnm/todoTransfer&quot;,&quot;appId&quot;:&quot;1583982098180735&quot;,&quot;app&quot;:&quot;/pages/checkDetail/index&quot;}"></userTask>
    <userTask id="sid-40E81439-12A0-4933-A29D-6922B06BD156" name="效果图报价单上传|广告公司" activiti:assignee="${restrictDim.advertisingAgency}"></userTask>
    <userTask id="sid-23A70891-D910-47C5-8B38-9245A6E46AA3" name="效果图报价单审核|营销总监" activiti:assignee="zx_yxzj_new|mkt:${restrictDim.mkt}" activiti:formKey="{&quot;pc&quot;:&quot;/hnm/todoTransfer&quot;,&quot;appId&quot;:&quot;1583996294852048&quot;,&quot;app&quot;:&quot;/pages/zyCheckDetails/index&quot;}"></userTask>
    <userTask id="sid-603F8DB1-508B-4D15-91CD-0D85AB5A437C" name="三专店主流程完成" activiti:assignee="hnm"></userTask>
    <sequenceFlow id="sid-CD0A4BDF-FC54-443C-9216-B1DDB2F08868" sourceRef="start" targetRef="sid-3FD87B95-DDC9-43E0-9A46-96F10154595C"></sequenceFlow>
    <sequenceFlow id="sid-430783A6-9F2A-4005-99C2-3CEBE7868DAE" sourceRef="sid-3FD87B95-DDC9-43E0-9A46-96F10154595C" targetRef="sid-B953A106-1A2C-47C7-AD4D-DB65C0FEA30E"></sequenceFlow>
    <sequenceFlow id="sid-3B6863B1-C87F-44BC-A0E1-0EEAB46BA059" sourceRef="sid-B953A106-1A2C-47C7-AD4D-DB65C0FEA30E" targetRef="sid-40E81439-12A0-4933-A29D-6922B06BD156"></sequenceFlow>
    <sequenceFlow id="sid-1455C06A-186C-4B3D-8A70-FEE6059F610C" sourceRef="sid-40E81439-12A0-4933-A29D-6922B06BD156" targetRef="sid-23A70891-D910-47C5-8B38-9245A6E46AA3"></sequenceFlow>
    <sequenceFlow id="sid-131B3DD4-DC26-45D0-AC06-69BFE867E7CF" sourceRef="sid-23A70891-D910-47C5-8B38-9245A6E46AA3" targetRef="sid-603F8DB1-508B-4D15-91CD-0D85AB5A437C"></sequenceFlow>
    <endEvent id="end" name="结束"></endEvent>
    <sequenceFlow id="sid-DCB6A17B-CAA9-4986-B3AC-81AF75387BF7" sourceRef="sid-603F8DB1-508B-4D15-91CD-0D85AB5A437C" targetRef="end"></sequenceFlow>
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_ownChannelThreeShopMainProcess">
    <bpmndi:BPMNPlane bpmnElement="ownChannelThreeShopMainProcess" id="BPMNPlane_ownChannelThreeShopMainProcess">
      <bpmndi:BPMNShape bpmnElement="start" id="BPMNShape_start">
        <omgdc:Bounds height="35.0" width="35.0" x="30.0" y="45.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="sid-3FD87B95-DDC9-43E0-9A46-96F10154595C" id="BPMNShape_sid-3FD87B95-DDC9-43E0-9A46-96F10154595C">
        <omgdc:Bounds height="80.0" width="100.0" x="186.0" y="230.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="sid-B953A106-1A2C-47C7-AD4D-DB65C0FEA30E" id="BPMNShape_sid-B953A106-1A2C-47C7-AD4D-DB65C0FEA30E">
        <omgdc:Bounds height="80.0" width="100.0" x="410.0" y="230.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="sid-40E81439-12A0-4933-A29D-6922B06BD156" id="BPMNShape_sid-40E81439-12A0-4933-A29D-6922B06BD156">
        <omgdc:Bounds height="80.0" width="100.0" x="617.0" y="240.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="sid-23A70891-D910-47C5-8B38-9245A6E46AA3" id="BPMNShape_sid-23A70891-D910-47C5-8B38-9245A6E46AA3">
        <omgdc:Bounds height="80.0" width="100.0" x="850.0" y="220.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="sid-603F8DB1-508B-4D15-91CD-0D85AB5A437C" id="BPMNShape_sid-603F8DB1-508B-4D15-91CD-0D85AB5A437C">
        <omgdc:Bounds height="80.0" width="100.0" x="1250.0" y="139.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="end" id="BPMNShape_end">
        <omgdc:Bounds height="35.0" width="35.0" x="1440.0" y="63.0"></omgdc:Bounds>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge bpmnElement="sid-CD0A4BDF-FC54-443C-9216-B1DDB2F08868" id="BPMNEdge_sid-CD0A4BDF-FC54-443C-9216-B1DDB2F08868">
        <omgdi:waypoint x="47.0" y="80.0"></omgdi:waypoint>
        <omgdi:waypoint x="271.0" y="91.0"></omgdi:waypoint>
        <omgdi:waypoint x="236.0" y="230.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="sid-430783A6-9F2A-4005-99C2-3CEBE7868DAE" id="BPMNEdge_sid-430783A6-9F2A-4005-99C2-3CEBE7868DAE">
        <omgdi:waypoint x="236.0" y="230.0"></omgdi:waypoint>
        <omgdi:waypoint x="352.0" y="131.0"></omgdi:waypoint>
        <omgdi:waypoint x="460.0" y="230.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="sid-3B6863B1-C87F-44BC-A0E1-0EEAB46BA059" id="BPMNEdge_sid-3B6863B1-C87F-44BC-A0E1-0EEAB46BA059">
        <omgdi:waypoint x="460.0" y="230.0"></omgdi:waypoint>
        <omgdi:waypoint x="574.0" y="115.0"></omgdi:waypoint>
        <omgdi:waypoint x="667.0" y="240.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="sid-1455C06A-186C-4B3D-8A70-FEE6059F610C" id="BPMNEdge_sid-1455C06A-186C-4B3D-8A70-FEE6059F610C">
        <omgdi:waypoint x="667.0" y="240.0"></omgdi:waypoint>
        <omgdi:waypoint x="782.0" y="106.0"></omgdi:waypoint>
        <omgdi:waypoint x="900.0" y="220.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="sid-131B3DD4-DC26-45D0-AC06-69BFE867E7CF" id="BPMNEdge_sid-131B3DD4-DC26-45D0-AC06-69BFE867E7CF">
        <omgdi:waypoint x="900.0" y="220.0"></omgdi:waypoint>
        <omgdi:waypoint x="1029.0" y="46.0"></omgdi:waypoint>
        <omgdi:waypoint x="1300.0" y="139.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="sid-DCB6A17B-CAA9-4986-B3AC-81AF75387BF7" id="BPMNEdge_sid-DCB6A17B-CAA9-4986-B3AC-81AF75387BF7">
        <omgdi:waypoint x="1300.0" y="219.0"></omgdi:waypoint>
        <omgdi:waypoint x="1530.0" y="302.0"></omgdi:waypoint>
        <omgdi:waypoint x="1457.0" y="98.0"></omgdi:waypoint>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>