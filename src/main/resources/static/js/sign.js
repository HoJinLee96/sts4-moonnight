import { validate, ValidationError } from '/js/validate.js';

/**
 * 로그인
 */
export async function signIn(signInDto, rememberEmail, encodedRedirect) {

	validate('email', signInDto.email);
	validate('password', signInDto.password);

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
	if (response.ok) {
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

	validate('email', email);
	validate('password', password);
	if (!password.equals(confirmPassword)) {
		const error = new ValidationError('두 비밀번호가 일치하지 않습니다.');
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
export async function signUpStep2(signUpRequestDto) {

	validate('name', signUpRequestDto.name);
	validate('birth', signUpRequestDto.birth);
	validate('phone', signUpRequestDto.phone);
	validate('address', signUpRequestDto.postcode);
	
	const response = await fetch("/api/sign/public/up/second", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify(signUpRequestDto)
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

export function loginWithNaver(encodedRedirect) {

	let naverAuthUrl = `/oauth2/authorization/naver`;
	if (encodedRedirect) {
		naverAuthUrl += '?redirect=' + encodedRedirect;
	}
	window.location.href = naverAuthUrl;
}

export async function signInAuthPhone() {
	const response = await fetch("/api/sign/public/in/auth/sms", {
		method: "POST"
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}

export async function signInAuthEmail() {
	const response = await fetch("/api/sign/public/in/auth/email", {
		method: "POST"
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}


export async function signOutAuth() {
	const response = await fetch("/api/sign/public/out/auth", {
		method: "POST"
	});
	if (response.ok) {
		return await response.json();
	} else {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.');
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}