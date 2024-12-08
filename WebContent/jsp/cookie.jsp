<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>更新cookie</title>
<%@include file="uf-mobile.jsp"%>
<style type="text/css">

</style>
</head>
<body>
<br>
<input type="text" id="cookie" class="uf-input" placeholder="COOKIE">
<br>
<button id="update">更新cookie</button>
<script type="text/javascript">
$(function() {
	$('#update').click(function(){
		if(confirm('sure?')){
			ajax({
				url: 'Chart/updateCookie.o',
				data: {
					cookie: $('#cookie').val()
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