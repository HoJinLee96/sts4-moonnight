import { formatEmail, formatPasswords, formatName, formatBirth, formatPhoneNumber } from '/js/format.js';

/**
 * 로그인
 */
export async function signIn(signInDto, rememberEmail, encodedRedirect) {

	if (!formatEmail(signInDto.email)) {
		const error = new Error('이메일 형식이 올바르지 않습니다.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
	if (!formatPasswords(signInDto.password)) {
		const error = new Error('비밀번호 형식이 올바르지 않습니다.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}

	let apiUrl = "/api/sign/public/in/local";

	if (encodedRedirect) {
		apiUrl += '?redirect=' + encodedRedirect;
	}

	const response = await fetch(apiUrl, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(signInDto)
	});

	if (response.ok) {
		const json = await response.json();
		if (rememberEmail) {
			localStorage.setItem("rememberEmail", signInDto.email);
		} else {
			localStorage.removeItem("rememberEmail");
		}
		window.location.href = json.data.redirect;
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 로그아웃
 */
export async function signOut() {
	const response = await fetch("/api/sign/private/out/local", {
		method: "POST"
	});
	if (response.ok){
		return;
	} else {
		const json = await response.json();
		console.log(json);
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

/**
 * 회원가입 1차
 */
export async function signUpStep1(email, password, confirmPassword) {

	if (!formatEmail(email)) {
		const error = new Error('이메일 형식이 올바르지 않습니다.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
	if (!formatPasswords(password)) {
		const error = new Error('비밀번호 형식이 올바르지 않습니다.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
	if (!validateConfirmPasswords(password, confirmPassword)) {
		const error = new Error('두 비밀번호가 일치하지 않습니다.');
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}

	const response = await fetch("/api/sign/public/up/first", {
		method: "POST",
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
		},
		body: new URLSearchParams({ email, password, confirmPassword })
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		console.log(json);
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}

}

/**
 * 회원가입 2차
 */
export async function signUpStep2(signUpStep2Dto,
	name,
	birthInput,
	phoneInput,
	postcode,
	mainAddress,
	detailAddress,
	agreeToTerms,
	marketingReceivedStatus) {

	if (!formatName(name)) {
		errorHandler({ type: "VALIDATION", message: "이름 형식이 올바르지 않습니다." });
		return;
	}
	if (!formatBirth(birthInput)) {
		errorHandler({ type: "VALIDATION", message: "생년월일 형식이 올바르지 않습니다." });
		return;
	}
	if (!formatPhoneNumber(phoneInput)) {
		errorHandler({ type: "VALIDATION", message: "휴대폰 형식이 올바르지 않습니다." });
		return;
	}
	if (postcode === "" || mainAddress === "") {
		errorHandler({ type: "VALIDATION", message: "주소 형식이 올바르지 않습니다." });
		return;
	}
	if (agreeToTerms === false) {
		errorHandler({ type: "VALIDATION", message: "개인정보 저장 동의는 필수 입니다." });
		return;
	}

	const body = {
		name: name,
		birth: birthInput.value,
		phone: phoneInput.value,
		postcode: postcode,
		mainAddress: mainAddress,
		detailAddress: detailAddress,
		marketingReceivedStatus: marketingReceivedStatus
	};


	const response = await fetch("/api/sign/public/up/second", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(body)
	});

	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		console.log(json);
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}

}

export function loginWithKakao(encodedRedirect) {
	let kakaoAuthUrl = `/oauth2/authorization/kakao`;
	if (encodedRedirect) {
		kakaoAuthUrl += '?redirect=' + encodedRedirect;
	}
	window.location.href = kakaoAuthUrl;
}

export  function loginWithNaver(encodedRedirect) {

	let naverAuthUrl = `/oauth2/authorization/naver`;
	if (encodedRedirect) {
		naverAuthUrl += '?redirect=' + encodedRedirect;
	}
	window.location.href = naverAuthUrl;
}
