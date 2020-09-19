org.activiti.bpmn.exceptions.XMLException: cvc-datatype-valid.1.2.1: '0785bf15-8d5d-11e7-8f8f-5254004a5833_39abc790-91fa-11e7-9e50-5254004a5833_loan' is not a valid value for 'NCName'.

报错原因: 流程key使用了 数字开头

解决办法:
为了证实想法，借助谷歌终于弄清楚了。NCName只能以下划线(_)或字母开头，只能包含中划线(-)、下划线、
字母和数字，我们的流程ID是按单位ID+'_'+部门ID规则拼接的，而单位ID是随机生成的字符串，
所以有的是以数字开头的就无法部署。最终，我们更改了单位ID的 生成规则，
保证是字母开头(多么简单粗暴的solution...)。