<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>添加数据</title>
<%@include file="uf-mobile.jsp"%>
<style type="text/css">

</style>
</head>
<body>
<span>想想工资取没取</span>
<br>
<input type="number" id="yhRmb" class="uf-input" placeholder="银河资产">
<input type="number" id="yhRevenue" class="uf-input" placeholder="银河累计收益">
<br>
<input type="number" id="htRmb" class="uf-input" placeholder="华泰资产">
<input type="number" id="htRevenue" class="uf-input" placeholder="华泰累计收益">
<br>
<input type="number" id="zfbRmb" class="uf-input" placeholder="支付宝资产">
<input type="number" id="zfbRevenue" class="uf-input" placeholder="支付宝累计收益">
<br>
<input type="number" id="wxRmb" class="uf-input" placeholder="微信资产">
<input type="number" id="otherRmb" class="uf-input" placeholder="其他资产" value="0">
<br>
<button id="add">添加</button>
<script type="text/javascript">
$(function() {
	$('#add').click(function(){
		var yhRmb = $('#yhRmb').val();
		var yhRevenue = $('#yhRevenue').val();
		var htRmb = $('#htRmb').val();
		var htRevenue = $('#htRevenue').val();
		var zfbRmb = $('#zfbRmb').val();
		var zfbRevenue = $('#zfbRevenue').val();
		var wxRmb = $('#wxRmb').val();
		var otherRmb = $('#otherRmb').val();
		if(confirm('sure?')){
			ajax({
				url: 'Chart/addAsset.o',
				data: {
					rmb: Number(yhRmb) + Number(htRmb) + Number(zfbRmb) + Number(wxRmb) + Number(otherRmb),
					revenue: Number(yhRevenue) + Number(htRevenue) + Number(zfbRevenue)
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