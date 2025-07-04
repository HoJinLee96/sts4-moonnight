import { validate } from '/js/validate.js';

// ======= 이메일 유효성 + 중복 검사 =======
export async function checkEmailDuplication(email) {

	validate('email', email);

	const response = await fetch("/api/local/user/public/exist/email", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
	});

	if (response.ok) {
	} else {
		const json = await response.json();
		if (json.code == '4531') {
			const error = new Error('이미 가입되어 있는 이메일 입니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		} else {
			const error = new Error(json.message || '서버 요청에 실패했습니다.');
			error.code = json.code;
			error.type = "SERVER";
			throw error;
		}
	}

}

// ======= 이메일 인증번호 발송 =======
export async function sendEmail(email) {

	validate('email', email);

	// [POST] /api/public/verify/email  
	const response = await fetch("/api/verify/public/email", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
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

// ======= 이메일 인증번호 검증 =======
export async function verifyMailCode(email, code) {

	validate('email', email);
	validate('code', code);

	const body = {
		email: email,
		verificationCode: code
	};

	const response = await fetch("/api/verify/public/compare/email/uuid", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(body)
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