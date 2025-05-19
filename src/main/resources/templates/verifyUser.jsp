<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>계정 복구</title>
<style type="text/css">
.container {
	/* display: flex; */
	max-width: 1200px;
	margin: 0 auto;
	padding-top: 50px;
	min-height: 1080px;
}
.updatePasswordBlank{
color: #20367a;
background: white;
border: 1px solid #20367a;
border-radius: 120px;
cursor: pointer;
padding: 5px 15px;
}
.updatePasswordBlank:hover{
color: white;
background: #20367a;
border: 1px solid white;
}
</style>
</head>
<body>
	<%@ include file="/WEB-INF/view/main/main_header.jsp"%>
	<div class="container">
		<div>
		<h3>계정 복구</h3>
		</div>
		<div>
			<button class="updatePasswordBlank" id="updatePasswordBlank" type="button" onclick="openFindWindow('/update/password/blank')">휴대폰 인증</button>
		</div>
	
	</div>
	<%@ include file="/WEB-INF/view/main/main_footer.jsp"%>
</body>
<script type="text/javascript">
function openFindWindow(url) {
    // 새 창의 크기 지정
    const width = 500;
    const height = 800;

    // 창의 중앙 위치 계산
    const left = window.screenX + (window.outerWidth / 2) - (width / 2);
    const top = window.screenY + (window.outerHeight / 2) - (height / 2);

    // 새로운 창 열기
    window.open(
        url,  // 열고자 하는 URL
        '_blank',  // 새 창의 이름 또는 _blank (새 탭)
        'width=' + width + ',height=' + height + ',top=' + top + ',left=' + left  // 창의 크기와 위치
    );
    return false; // 기본 링크 동작을 막기 위해 false를 반환
}
</script>
<script type="text/javascript">
  window.addEventListener('message', function(event) {
      if (event.data.updatePasswordStatus === 200) {
          location.href = "/clearLogin";
      }else {
          location.href = "/logout";
      }

  });

</script>
</html>