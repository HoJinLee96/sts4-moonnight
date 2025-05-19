<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>비밀번호 관리</title>
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
#loginForm label[for="phone"] {
	text-align: left;
	display: block;
	margin: 0px;
	padding: 0px;
	margin-top: 15px; /* input과의 간격을 조정 */
	font-size: 14px;
}

#phone, #verificationSmsCode, #email, #password, #confirmPassword{
	width: 250px;
	height: 40px;
	border: none;
	border-bottom: 2px solid #ccc;
	outline: none;
	transition: border-bottom-color 0.3s;
	margin-right: 20px;
}

#phone:focus {
	border-bottom: 2px solid #20367a;
}

#input-wrapper {
	position: relative;
	display: none; /* none <-> inline-block */
}

#verificationTimeMessage {
	position: absolute;
	top: 60%;
	right: 40%;
	transform: translateY(-50%);
	font-size: 14px;
	color: red;
	pointer-events: none; /* 타이머가 클릭되지 않도록 설정 */
}

#sendSmsButton, #verifySmsCodeButton {
	border: none;
	background-color: #20367a;
	color: white;
	width: 130px;
	height: 40px;
	margin: 0px 3px;
	border-radius: 8px;
}

#sendSmsButton:hover, #verifySmsCodeButton:hover, #updatePasswordButton:hover, #confirmButton:hover {
	border: 1px solid #20367a;
	background-color: white;
	color: #20367a;
	cursor: pointer;
}

#confirmDiv,#newPasswordDiv {
	display: none; /* none <-> block */
}

#confirmButton,#updatePasswordButton {
	border: none;
	background-color: #20367a;
	color: white;
	width: 130px;
	height: 40px;
	border-radius: 8px;
	margin-left: 35%
}

#confirmEmail {
	padding: 10px 20px;
	background-color: #ededed;
}

/* 닫기 버튼 스타일 */
.close {
	position: absolute;
	top: 10px;
	left: 10px; /* 왼쪽 위에 위치 */
	color: #aaa;
	font-size: 28px;
	font-weight: bold;
	cursor: pointer;
}

.close:hover, .close:focus {
	color: #20367a;
	text-decoration: none;
	cursor: pointer;
}
</style>
</head>

<body>
	<div class="container">
		<div id="formDiv">
			<span class="close" onclick="window.close()">&times;</span>
			<h2>비밀번호 변경</h2>
			<input type="hidden" id="email" val>
			<label for="phone">휴대폰 번호</label>
			<br>
			<input type="text" id="phone" name="phone" required oninput="formatPhoneNumber(this)"maxlength="13" value="010-" >
			<button class="sendSmsButton" id="sendSmsButton" type="button">인증번호 발송</button>
			<br> 
			<span id="sendSmsMessage"></span> 
			<br>
			<div id="input-wrapper">
				<label for="verificationCode">인증번호</label> 
				<br> 
				<input type="text" id="verificationSmsCode" name="verificationSmsCode" required oninput="formatCode(this)" maxlength="5" readonly disabled>
				<div id="verificationTimeMessage"></div>
				<button class="verifySmsCodeButton" id="verifySmsCodeButton" type="button" disabled>인증번호 확인</button>
				<br> 
				<span id="verificationSmsMessage"></span>
			</div>
			<br> <br> 
			<div id="newPasswordDiv">
				<span>비밀번호 변경</span>
				<br> <br>
				<label for="password">새로운 비밀번호</label>
				<br>
				<input type="password" id="password" name="password" required oninput="formatPasswords();">
				<br>
				<span id="passwordMessage"></span>
				<br><br>
				<label for="confirmPassword">새로운 비밀번호 확인</label>
				<br>
				<input type="password" id="confirmPassword" name="confirmPassword" required oninput="validateConfirmPasswords()">
				<br>
				<span id="confirmPasswordMessage"></span>
				<br><br><br>
				<button id="updatePasswordButton" type="button">비밀번호 변경</button>
			</div>
			<div id="confirmDiv">
				<br> <br> 
				<span id="confirmMessage"></span>
				<br> <br> <br> <br> 
				<button id="confirmButton" type="button" onclick="window.close()">확인</button>
			</div>


		</div>
	</div>

</body>

<!-- 휴대폰 번호와 이메일 조회 -->
<script type="text/javascript">
function isEmailPhoneExist() {
	var email = document.getElementById("email").value;
	var phone = document.getElementById("phone").value;
	var confirmMessage = document.getElementById("confirmMessage");

	var xhr = new XMLHttpRequest();
	xhr.open('POST', '/user/exist/emailPhone', false); 
	xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');

	var data = 'email=' + encodeURIComponent(email) + '&phone=' + encodeURIComponent(phone);
    
	xhr.onload = function() {
	    if (xhr.status === 200) {
			document.getElementById("newPasswordDiv").style.display = "block";
	    } else if (xhr.status === 404) {
	    	alert("가입시 입력한 정보와 일치하지 않습니다.");
	    	confirmMessage.innerText = "가입시 입력한 정보와 일치하지 않습니다.";
			document.getElementById("confirmDiv").style.display = "block";
	    } else if (xhr.status === 500) {
	    	alert("서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
	    	confirmMessage.innerText = "서버 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.";
			document.getElementById("confirmDiv").style.display = "block";
	    } else {
	    	alert("알 수 없는 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.");
	    	confirmMessage.innerText = "알 수 없는 오류가 발생했습니다. \n 잠시 후 다시 시도해주세요.";
			document.getElementById("confirmDiv").style.display = "block";
	    }
	};
	 xhr.send(data);
}

function updatePassword() {
	var email = document.getElementById("email").value;
	var password = document.getElementById("password").value;
	var confirmPassword = document.getElementById("confirmPassword").value;
	
	var xhr = new XMLHttpRequest();
	xhr.open('POST', '/user/update/password', false); 
	xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
	
    var data = 'email=' + encodeURIComponent(email) +
    '&password=' + encodeURIComponent(password) +
    '&confirmPassword=' + encodeURIComponent(confirmPassword);
    
    xhr.onload = function() {
	    if (xhr.status === 200) {
	        alert("비밀번호 변경 완료.");
	    } else if (xhr.status === 401) {
	    	alert("오류가 발생했습니다. 다시 시도해 주세요.");
	    } else if (xhr.status === 500) {
	    	alert("오류가 발생했습니다. 잠시후 다시 시도해주세요.");
	    } else {
	    	alert("오류가 발생했습니다.");
	    }
        window.opener.postMessage({ verifyStatus: xhr.status }, "*");
        window.close();
    };
	 xhr.send(data);
}
</script>

<!-- sms 인증 api
<script type="text/javascript">
	var timerInterval; // 타이머 인터벌을 저장할 변수
	
	var reqPhone = document.getElementById("phone").value.replace(/[^0-9]/g, ''); // 인증할 휴대폰 번호
	
	var reqCode = document.getElementById("verificationSmsCode").value; // 인증할 인증코드 번호
	var message = document.getElementById("verificationSmsMessage"); // 인증 결과 메시지
	
	function sendSms() {
		
		if (reqPhone.length !== 11){
			alert("휴대폰 번호를 확인해 주세요.");
		} 
		else {
			var xhr = new XMLHttpRequest();
			xhr.open('POST', '/api/verify/sendsms', false);
			xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');
			
		    xhr.onreadystatechange = function() {
		        if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						alert("인증번호 발송 완료");
						document.getElementById("phone").setAttribute("readonly", true);
						document.getElementById("phone").setAttribute("disabled", true);
						document.getElementById("sendSmsButton").innerText = "인증번호 재발송";
						document.getElementById("input-wrapper").style.display = "inline-block";
						document.getElementById("verificationSmsCode").removeAttribute("readonly");
						document.getElementById("verificationSmsCode").removeAttribute("disabled");
						document.getElementById("verifySmsCodeButton").removeAttribute("disabled");
						onVerificationCodeSent();
					} else {
						message.style.color = 'red';
						if (xhr.status === 429) {
							message.innerText = "시도 초과. 잠시 후 다시 시도 해주세요.";
						}else if(xhr.status === 500){
							message.innerText = "서버 오류가 발생했습니다. 다시 시도해주세요.";
						} else {
							message.innerText = "알 수 없는 오류가 발생했습니다. \n 재발송 시도 해주세요.";
						}
					}
		        }
		    };
	    xhr.send('reqPhone=' + encodeURIComponent(reqPhone));
		}
	}

	function verifySmsCode() {
		
		if (reqCode.length < 5) {
			alert("인증번호를 다시 확인해주세요.");
			message.style.color = 'red';
			message.innerText = "인증번호를 다시 확인해주세요.";
		} else {
			var xhr = new XMLHttpRequest();
			xhr.open('POST', '/api/verify/comparecode', false); 
			xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded; charset=UTF-8');

		    xhr.onload = function() {
				if (xhr.status === 200) {
					isEmailPhoneExist();
					message.style.color = 'green';
					message.innerText = "인증 성공";
					document.getElementById("phone").setAttribute("readonly", true);
					document.getElementById("phone").setAttribute("disabled", true);
					document.getElementById("sendSmsButton").setAttribute("disabled", true);
					document.getElementById("verificationSmsCode").setAttribute("readonly", true);
					document.getElementById("verificationSmsCode").setAttribute("disabled", true);
					document.getElementById("verifySmsCodeButton").setAttribute("disabled", true);
		            clearInterval(timerInterval); // 타이머 중지
		            timerInterval = null; // 타이머 초기화
				} else {
					message.style.color = 'red';
					if (xhr.status === 408) {
						alert(xhr.responseText);
						message.innerText = xhr.responseText;
					}else if( xhr.status === 401){
						alert(xhr.responseText);
						message.innerText = xhr.responseText;
					}else if( xhr.status === 500){
						alert(xhr.responseText);
						message.innerText = xhr.responseText;
					}else {
						alert("알 수 없는 장애 발생. \n 잠시 후 다시 시도 해주세요.");
						message.innerText = "알 수 없는 장애 발생. 잠시 후 다시 시도 해주세요.";
					}
				}
			};
		xhr.send('reqCode=' + encodeURIComponent(reqCode));
		}
	}
	
	function onVerificationCodeSent() {
	    // 3분 타이머 시작
	    let timeLeft = 180; // 3분 = 180초
	    const timerElement = document.getElementById('verificationTimeMessage');
	    var message = document.getElementById("verificationSmsMessage");
	    timerElement.textContent = formatTime(timeLeft);

	    timerInterval = setInterval(() => {
	        timeLeft--;
	        timerElement.textContent = formatTime(timeLeft);

	        if (timeLeft <= 0) {
	            message.style.color = 'red';
	            message.innerText = '인증 시간이 초과되었습니다. \n 재발송 시도 해주세요.' ;
	            timerElement.textContent = '00:00' ;
	            document.getElementById("verificationSmsCode").setAttribute("readonly", true);
	            document.getElementById("verificationSmsCode").setAttribute("disabled", true);
	            document.getElementById("verifySmsCodeButton").setAttribute("disabled", true);
	            clearInterval(timerInterval); // 타이머 중지
	            timerInterval = null; // 타이머 초기화
	        }
	    }, 1000);
	}

    function formatTime(seconds) {
        var minutes = Math.floor(seconds / 60);
        var remainingSeconds = seconds % 60;
        
        function pad(number) {
            return (number < 10 ? '0' : '') + number;
        }

        return pad(minutes) + ':' + pad(remainingSeconds);
    }
</script>
-->
<script type="text/javascript">
function formatPhoneNumber(input) {
	let value = input.value.replace(/[^0-9]/g, ''); // 숫자 이외의 문자를 제거합니다.
	let formattedValue = value;

	// 앞 세 자리를 "010"으로 고정합니다.
	if (value.startsWith('010')) {
		value = value.slice(3); // 앞 세 자리("010")를 잘라냅니다.
	}

	if (value.length <= 4) {
		formattedValue = '010-' + value; // 4자리 이하의 숫자만 있을 경우
	} else if (value.length <= 7) {
		formattedValue = '010-' + value.slice(0, 4) + '-' + value.slice(4); // 5~7자리의 경우
	} else {
		formattedValue = '010-' + value.slice(0, 4) + '-'
				+ value.slice(4, 8); // 8자리 이상의 경우
	}

	input.value = formattedValue;
}
function formatCode(input) {
	input.value = input.value.replace(/[^0-9]/g, '');
}
function formatPasswords() {
	var password = document.getElementById("password").value;
	var message = document.getElementById("passwordMessage");
	var confirmPasswordMessage = document.getElementById("confirmPasswordMessage");
	confirmPasswordMessage.innerText = ' ';

	var passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;

	if (!passwordPattern.test(password)) {
		message.style.color = 'red';
		message.innerText = "최소 8자 이상이며, 영어, 숫자, 특수기호를 포함해야 합니다.";
		return false;
	} else {
		message.style.color = 'green';
		message.innerText = "유효한 비밀번호 입니다.";
		return true;
	}
}
function validateConfirmPasswords() {
	var password = document.getElementById("password").value;
	var confirmPassword = document.getElementById("confirmPassword").value;
	var confirmPasswordMessage = document.getElementById("confirmPasswordMessage");

	if (formatPasswords()) {
		if (password !== confirmPassword) {
			confirmPasswordMessage.style.color = 'red';
			confirmPasswordMessage.innerText = "비밀번호가 일치하지 않습니다.";
			return false;
		} else {
			confirmPasswordMessage.style.color = 'green';
			confirmPasswordMessage.innerText = "비밀번호가 일치합니다.";
			return true;
		}
	}
}

</script>
<script type="text/javascript">
document.getElementById('sendSmsButton').addEventListener('click', function(event) {
	sendSms();	
});
document.getElementById('verifySmsCodeButton').addEventListener('click', function(event) {
	verifySmsCode(function(result){
		if(result){
			isEmailPhoneExist();
		}
	});
});
document.getElementById('updatePasswordButton').addEventListener('click', function(event) {
	updatePassword();	
});
</script>
</html>