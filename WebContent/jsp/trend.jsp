<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>趋势轮动法</title>
<%@include file="uf-mobile.jsp"%>
<style type="text/css">
#chart{width: 28rem;height: 12rem}
</style>
</head>
<body>
<input type="date" id="today">
<select id="interval">
	<option value="1">近一月</option>
	<option value="3">近三月</option>
	<option value="6">近六月</option>
	<option value="12">近一年</option>
	<option value="36">近三年</option>
	<option value="60">近五年</option>
	<option value="">成立来</option>
	<option value="hm">选择开始日期:</option>
</select>
<input type="date" id="start">
<div id="chart"></div>
<script type="text/javascript">
var myChart = echarts.init(document.getElementById('chart'));
var option = {
	title: {
	    text: ''
	},
	tooltip: {},//空着也必须得有
	legend: {
		type: 'scroll'
	},
	xAxis: {},
	yAxis: {}//空着也必须得有
	//series: []
};
function select(){
	var today = $('#today').val();
	var interval = $('#interval').val();
	var start = '';
	if(interval == 'hm'){
		start = $('#start').val();
	}
	if(!today){
		today = now10();
		$('#today').val(today);
	}
	ajax({
		url: 'Chart/trend.o',
		data: {
			today: today,
			interval: interval,
			start: start
		},
		right: function(json){
			option.title.text = json.titleText;
			option.legend.data = json.legendDataList;//必须得有
			option.xAxis.data = json.xAxisDataList;
			option.series = json.seriesList;
			myChart.setOption(option, true);
		}
	});
}
$(function(){
	$('#today,#interval,#start').change(function(){
		if($('#interval').val() == 'hm'){
			if($('#start').css('display') == 'none'){
				$('#start').val('');
				$('#start').css('display', '');
				return;
			}
		}else{
			$('#start').css('display', 'none');
		}
		select();
	});
	$('#today').val(now10());
	$('#start').css('display', 'none');
});
</script>
</body>
</html>