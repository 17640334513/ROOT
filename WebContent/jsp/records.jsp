<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>记录战绩</title>
<%@include file="uf-mobile.jsp"%>
<style type="text/css">

</style>
</head>
<body>
<br>
<input type="text" id="name" class="uf-input" placeholder="NAME">
<br>
<input type="number" id="profit" class="uf-input" placeholder="PROFIT">
<br>
<button id="add">添加</button>
<script type="text/javascript">
$(function() {
	$('#add').click(function(){
		if(confirm('sure?')){
			ajax({
				url: 'Chart/addRecords.o',
				data: {
					NAME: $('#name').val(),
					PROFIT: $('#profit').val()
				},
				right: function(json){
					alert(json);
				}
			});
		}
	});
});
</script>
</body>
</html>