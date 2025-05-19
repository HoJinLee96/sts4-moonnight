export function formatEmail(email,message) {
	const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	const isValid = emailPattern.test(email);

	if (message) {
		message.style.color = isValid ? 'green' : 'red';
		message.innerText = isValid ? " " : "올바른 이메일 형식을 입력 해주세요.";
	}
	return isValid;
}

export function formatPasswords(password,message,confirmMessage) {
	var passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/;
	if(message){
		message.innerText = ' ';
	}
	if(confirmMessage){
		confirmMessage.innerText = ' ';
	}
	const isValid = passwordPattern.test(password);

	if (message) {
		message.style.color = isValid ? 'green' : 'red';
		message.innerText = isValid ? "유효한 비밀번호 입니다." : "최소 8자 이상이며, 영어, 숫자, 특수기호를 포함해야 합니다.";
	}
	return isValid;
}

export function validateConfirmPasswords(password, confirmPassword, confirmMessage) {
	const isValid = (password===confirmPassword);
	if (confirmMessage) {
		confirmMessage.style.color = isValid ? 'green' : 'red';
		confirmMessage.innerText = isValid ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
	}
	return isValid;
}

export function formatName(name, message) {
	
	// 정규 표현식: 공백 또는 특수기호
	var regex = /[!@#$%^&*(),.?":{}|<>\s]/;
	if(message){
		message.innerText="";
	}
	
	if (name === "" || regex.test(name)) {
		if (message) {
			message.style.color = 'red';
			message.innerText = "이름에 공백 또는 특수기호를 포함 시킬 수 없습니다.";
		}
		return false;
	} else {
		if(message){
			message.innerText="";
		}
		return true;
	}
	
}

export function formatBirth(input,message) {
	
	var regex = /[^0-9]/g; //숫자가 아닌 문자
	input.value = input.value.replace(regex, '');
	
	if (input.value.length > 8) {
	    input.value = input.value.substring(0, 8);
	}
	var birth = input.value;
	if (birth === "" || regex.test(birth) || birth.length !== 8 || !validDate(birth)) {
		if(message){
		message.style.color = 'red';
		message.innerText = "올바른 생년월일 입력 해주세요.";
		}
		return false;
	}else {
		if(message){
			message.innerText="";
		}
		return true;
	}
}

export function validDate(birth) {
	const year = parseInt(birth.substring(0,4),10);
	const month = parseInt(birth.substring(4,6),10);
	const day = parseInt(birth.substring(6,8),10);
	const date = new Date(year, month -1, day);
	
	if (date.getFullYear() !== year|| date.getMonth() !== month - 1 || date.getDate() !== day) {
  		return false;
	}
	
	const today = new Date();
    today.setHours(0, 0, 0, 0); // 시/분/초/밀리초를 0으로 설정하여 날짜만 비교
    if (date > today) {
        return false;
    }
	
	return true;
}

export function formatPhoneNumber(input,message) {
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
	if(message){
		if(value.length<8){
				message.style.color = 'red';
				message.innerText = "올바른 휴대폰 번호를 입력 해주세요.";
		}else{
			message.innerText = "";
		}
	}

	var regex = /^\d{3,4}-\d{3,4}-\d{4}$/;
	return regex.test(formattedValue);
}

export function formatVerifyCode(input) {
	input.value = input.value.replace(/[^0-9]/g, '');
	if (input.value.length > 6) {
	    input.value = input.value.substring(0, 6);
	}
}
