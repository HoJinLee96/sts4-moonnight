<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>프로필 관리</title>
<style type="text/css">
.subContent {
	margin-top: 30px;
}
.contentTitle{
padding-bottom : 15px;
border-bottom: 4px solid #20367a;
}
#overlay {
	position: flex;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	background-color: rgba(128, 128, 128, 0.7);
	display: none;
	justify-content: center;
	align-items: center;
	z-index: 1000;
}
.switchDiv{
position: absolute;
right: 0px;
bottom: 20px;
}
.label{
display: block;
margin: 20px 0px 10px 0px;
font-size: 14px;
color: #acacac;
}
.valueDiv{
    border-bottom: 2px solid #20367a;
max-width: 550px;
position: relative;
padding-bottom: 10px;
}
.value{
width:450px;
white-space: nowrap;/* 줄바꿈없음 */
overflow:hidden;
text-overflow: ellipsis;/*...으로표시  */
margin: 15px 0px 10px 0px;
}
.changeButton{
z-index:1;
color: #20367a;
background: white;
border: 1px solid #20367a;
border-radius: 10px;
cursor: pointer;
padding: 6px 8px;
position: absolute;
right: 0;
bottom:15px;
}
.changeBox{
position: absolute;
right: 0;
bottom:15px;
}
button:hover{
color: white;
background: #20367a;
border: 1px solid white;
}
</style>
<!-- 스위치 -->
<style type="text/css">
/* 스위치 전체 컨테이너 */
.switch {
  position: relative;
  width: 50px;
  height: 26px;
}

/* 체크박스는 숨김 */
.toggle-checkbox {
	display: none;
}

/* 라벨: 배경과 껐다 켜졌을 때의 스타일 */
.toggle-label {
  display: block;
  width: 100%;
  height: 100%;
  background-color: #ccc; /* 꺼졌을 때 배경색 */
  border-radius: 30px;
  position: relative;
  cursor: pointer;
  transition: background-color 0.3s;
}

/* 스위치 버튼 */
.toggle-button {
  content: '';
  position: absolute;
  top: 1px;
  left: 2px;
  width: 24px;
  height: 24px;
  background-color: white;
  border-radius: 50%;
  transition: transform 0.3s ease; /* 부드러운 애니메이션 */
}

/* 체크박스가 체크되었을 때의 스타일 */
.toggle-checkbox:checked + .toggle-label {
  background-color: #20367a; /* 켜졌을 때 배경색 */
}

/* 스위치 버튼이 오른쪽으로 이동하는 효과 */
.toggle-checkbox:checked + .toggle-label .toggle-button {
  transform: translateX(22px); /* 부드럽게 이동 */
}
</style>
</head>
<script type="text/javascript">
document.addEventListener("DOMContentLoaded", function() {

	var name ="";
	var phone ="";
	var sortedAddress ="";
	var marketingReceived ="";
	
    if (${sessionScope.userDto != null}) {
    	name = "${sessionScope.userDto.name}";
		phone = "${sessionScope.userDto.phone}";
		sortedAddress = "(${sessionScope.addressList[0].postcode}) ${sessionScope.addressList[0].mainAddress} ${sessionScope.addressList[0].detailAddress}";
		marketingReceived = ${sessionScope.userDto.marketingReceivedStatus ? 'true' : 'false'};
    }else if (${sessionScope.oAuthDto != null}) {
    	name = "${sessionScope.oAuthDto.name}";
		phone = "${sessionScope.oAuthDto.phone}";
    }
    
	document.getElementById("name").textContent=name;
	document.getElementById("phone").textContent=phone;
	document.getElementById("sortedAddress").textContent=sortedAddress;
	document.getElementById('marketingReceived').checked = marketingReceived;
});
</script>
<body>
	<%@ include file="/WEB-INF/view/main/main_header.jsp"%>
	<div class="container">
		<%@ include file="mypageSidebar.jsp"%>

		<div class="content">
			<p class="headerFont contentTitle">개인 정보</p>
			<div class="subContent">
				
				<div class="valueDiv">
					<p class="label">이름</p>
					<p class="value" id="name"></p>
					<button class="changeButton" id="changeName">이름 변경</button>
				</div>

				<div class="valueDiv">
					<p class="label">휴대폰 번호</p>
					<p class="value" id="phone"></p>
					<button class="changeButton" id="changePhone">휴대폰 번호 변경</button>
				</div>

				<div class="valueDiv">
					<p class="label">주소록</p>
					<p class="value" id="sortedAddress"></p>
					<button class="changeButton" id="changeAddress">주소록 변경</button>
				</div>
			</div>
			<div class="subContent">
				<p class="subHeaderFont">광고성 정보 수신</p>
				<div class="valueDiv">
					<p class="value" id="phone">개인정보 수집 및 이용 내역</p>
					<div class="switchDiv" id="switchDiv">
						<div class="switch" id="switch">
							<input type="checkbox" id="marketingReceived"
								class="toggle-checkbox"<%-- <%= marketingReceived ? "checked" : "" %> --%>>
							<label for="marketingReceived" class="toggle-label"> <span
								class="toggle-button"></span>
							</label>
						</div>
					</div>
				</div>


			</div>

		</div>

	</div>

	<div id ="overlay"></div>
	<%@ include file="/WEB-INF/view/main/main_footer.jsp"%>
</body>
<script type="text/javascript">
document.getElementById('changeName').addEventListener('click',function(event){
	alert("현재 이름 변경은 지원하지 않습니다.");
});

document.getElementById('changePhone').addEventListener('click',function(event){
	openFindWindow('/verify/phone/blank');
});

document.getElementById('changeAddress').addEventListener('click',function(event){
	window.location.href='/my/addressBook';
});

document.getElementById('marketingReceived').addEventListener('change',function(event){
    var userDto = JSON.parse('${userJson}');
	var checked = document.getElementById('marketingReceived').checked;
	userDto.marketingReceivedStatus = checked;
	userUpdateInfo(userDto);
});

function userUpdateInfo(userDto){
	var xhr = new XMLHttpRequest();
	xhr.open('POST','/user/update/info');
	xhr.setRequestHeader('Content-Type','application/json; charset=UTF-8');
    xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
			if (xhr.status === 200) {
				alert("수정 완료");
			}else if (xhr.status === 500) {
				alert("서버 오류");
			}
        }
	}
	xhr.send(JSON.stringify(userDto));
}
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
    if (event.data.verifyPhoneStatus === 200) {
		var userDto = JSON.parse('${userJson}');
		userDto.phone = event.data.reqPhone;
		
		var xhr = new XMLHttpRequest();
		xhr.open('POST','/user/update/info');
		xhr.setRequestHeader('Content-Type','application/json; charset=UTF-8');
	    xhr.onreadystatechange = function() {
	        if (xhr.readyState === 4) {
				if (xhr.status === 200) {
					alert("휴대폰 변경 완료");
					location.href = "/my";
				}else if (xhr.status === 500) {
					alert("죄송합니다. 잠시 후 다시 시도해 주세요.");
				}else{
					alert("서버 오류.")
					location.href = "/logout";
				}
	        }
		}
		xhr.send(JSON.stringify(userDto));
    }

});
</script>
</html>