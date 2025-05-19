<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>로그인</title>
<style type="text/css">
@font-face {
	font-family: 'SF_HambakSnow';
	src:
		url('https://fastly.jsdelivr.net/gh/projectnoonnu/noonfonts_2106@1.1/SF_HambakSnow.woff')
		format('woff');
	font-weight: normal;
	font-style: normal;
}

* {
	font-family: 'SF_HambakSnow', sans-serif;
}
</style>
<style type="text/css">
.container {
	max-width: 1200px;
	margin: 0 auto;
	padding-top: 20px;
	min-height: 700px;
	display: flex;
	justify-content: center; /* 수평 중앙 정렬 */
}
.loginform{
	max-width: 400px;
	min-width: 400px;
	margin: 0 auto;
	border: 2px solid #20367a;
	padding : 0px 100px;
	border-radius: 10px;
	text-align: center;
}
.title{
text-align: left;
padding : 20px 0px;
margin-bottom: 30px;
}
#loginForm label[for="email"],
#loginForm label[for="password"] {
    text-align: left;
    display: block;
    margin: 0px;
    padding: 0px;
    margin-top: 15px; /* input과의 간격을 조정 */
    font-size: 14px;
}
#email, #password{
    width: 400px;
    height: 40px;
    border: none;
    border-bottom: 2px solid #ccc;
    outline: none;
    transition: border-bottom-color 0.3s;
}
#email:focus, #password:focus{
    border-bottom: 2px solid #20367a;
}
#loginForm button{
    width: 400px;
    height: 52px;
    border: none;
    border-radius: 10px;
    background-color: #20367a;
    color: white;
    font-size: 18px;
    margin: 25px 0px 10px 0px;
    cursor: pointer;
}
#loginForm button:hover{
    background-color: white;
    border: 2px solid #20367a;
    color:#20367a;
}
#OAutoLoginBlcok{
padding:10px 0px 30px 0px;
    display: flex;
    justify-content: center; /* 아이템 사이에 여백을 넣어 균등 배치 */
    align-items: center; /* 수직 정렬 */
}
#OAutoLoginBlcok a{
	text-decoration: none;
	display: block;
    width: 60px;
    height: 60px;
    margin: 0px 5px;
    
}
#OAutoLoginBlcok img {
	cursor: pointer;
    width: 60px;
    height: 60px;
}
#rememmberIdCheckbox{
    appearance: none; /* 기본 스타일 제거 */
    -webkit-appearance: none;
    -moz-appearance: none;
    background-color: #fff;
    border: 2px solid #ccc;
    border-radius: 50%; /* 동그랗게 만들기 */
    width: 15px;
    height: 15px;
    cursor: pointer;
    position: relative;
    outline: none;
    transition: background-color 0.2s, border-color 0.2s;
}
#rememmberIdCheckbox:checked {
    background-color: #20367a; /* 체크된 배경색 */
    border-color: #20367a;
}
#rememmberIdCheckbox:checked::after {
    content: '';
    position: absolute;
    top: 40%;
    left: 50%;
    width: 4px;
    height: 8px;
    border: solid white;
    border-width: 0 2px 2px 0;
    transform: translate(-50%, -50%) rotate(45deg);
}
label[for="rememmberIdCheckbox"]{
font-size: 14px;
margin-right: 140px;
color: #666;
}
#findEmail, #findPassword{
text-decoration: none;
color: #b1b1b1;
font-size: 14px;
margin-left: 5px;
}
#findEmail::after{
content: '';
border-right: 1px solid #e1e1e1;
padding-left: 5px;
}
#findEmail:hover, #findPassword:hover{
color: #20367a;
cursor:  
}
#etcActionDiv{
    display: flex;
    align-items: center; 
margin-top: 20px;
line-height:normal; 
}
#underline-text{
width: 400px;
position: relative;
display: inline-block;
margin: 10px 0px;
}
#underline-text::after {
content:"다른 방법 로그인";
    color: #b1b1b1;
    background-color: white;
    padding: 0px 15px;
    font-size: 15px;
}
#underline-text::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 1px;
    background-color: #e1e1e1;
    top:10px;
    left: 0px;
    z-index: -1;
}
</style>
</head>

<body>
	<div class="container">
		<div id="formDiv">
		<h2 class = "title">로그인</h2>
		<form id="loginForm">
	        <label for="email">이메일</label>
	        <input type="email" id=email name="email" required autofocus placeholder="example@example.com">
	        <br>
	        <label for="password">비밀번호</label>
	        <input type="password" id="password" name="password" required placeholder="password">
	        <br>
	        <!-- <div id="error"></div> -->
	        <div id ="etcActionDiv">
	        <input type="checkbox" id="rememmberIdCheckbox" name="rememmberIdCheckbox">
	        <label for="rememmberIdCheckbox">이메일 저장</label>
			<a href="" id="findEmail" onclick="openFindWindow('/find/email')">이메일 찾기</a>
			<a href="" id="findPassword" onclick="openFindWindow('/find/password')">비밀번호 찾기</a>
	        </div>
	        <button type="submit">로그인</button>
	    </form>

		</div>
	</div>

</body>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
function openFindWindow(url) {
    // 새 창의 크기 지정
    const width = 500;
    const height = 600;

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
</html>