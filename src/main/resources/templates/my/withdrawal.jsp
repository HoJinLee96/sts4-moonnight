<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>회원 탈퇴</title>
<link rel="stylesheet" type="text/css" href="../../static/mypageContainer.css">
<style type="text/css">
.stopBtn{
color: #20367a;
background: white;
border: 1px solid #20367a;
border-radius: 120px;
cursor: pointer;
padding: 5px 15px;
margin-top: 50px; 
margin-right: 3px;
}
.cancelBtn{
color: white;
background: #20367a;
border: 1px solid #20367a;
border-radius: 120px;
cursor: pointer;
padding: 5px 15px;
margin-top: 50px; 
}
</style>
</head>
<body>
<%@ include file = "/WEB-INF/view/main/main_header.jsp" %>
<div class="container">
	<%@ include file="/WEB-INF/view/my/mypageSidebar.jsp"%>
	<div class="content">
		<p class="headerFont contentTitle">회원 탈퇴</p>
		<p class="contentTitle">회원탈퇴에 앞서 아래 내용을 반드시 확인해 주세요.</p>
		<div class="subContent">
			<div>
				
				<div class="switch">
				  <input type="checkbox" id="checkbox1">
				  <label for="toggle" class="toggle-label">
				    <span class="toggle-button"></span>
				  </label>
				</div>
			
				<input type="checkbox" id="title0">
				<label for="title0">탈퇴하면 회원 정보 및 서비스 이용 기록이 삭제됩니다.</label>
			</div>
		</div>
		<div class="subContent">
			<button class="stopBtn">탈퇴</button>
			<button class="cancelBtn">취소</button>
		</div>
	</div>
</div>

<%@ include file = "/WEB-INF/view/main/main_footer.jsp" %>
</body>

<!-- 회원탈퇴 -->
<script type="text/javascript">
publicStopBtn = document.getElementById('stopBtn').addEventListener('click', function(event) {
   event.preventDefault(); // 링크 기본 동작 방지
   
   var email = $('#email').val();
   var password = $('#password').val();
   
   console.log(email, password);
   
   var xhr = new XMLHttpRequest();
   xhr.open('POST', '/user/stop', true);
   xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
   var data = 'email=' + encodeURIComponent(email) +
   '&password=' + encodeURIComponent(password);


   xhr.onreadystatechange = function() { // onreadyState 변경
   	if(xhr.readyState === 4){
       if (xhr.status === 200) { // 성공적인 응답
       	alert("성공적으로 회원탈퇴 됐습니다.\n다음에 또 이용해 주세요.");
           window.location.href = '/logout';
       } else { // 실패한 응답
           alert('오류 발생 : ' + xhr.responseText);
       }
   	}
   };
   xhr.send(data);
});
</script>
</html>