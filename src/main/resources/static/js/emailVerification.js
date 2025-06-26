import { validate } from '/js/validate.js';

let timerInterval = null;
let mailId = 0; // 이메일 인증 요청 여부

// ======= 이메일 유효성 + 중복 검사 =======
export async function validateEmail(successHandler, errorHandler) {
	const email = document.getElementById("userEmail").value;

	// 1차 형식 검증
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		errorHandler(404);
		return;
	}

	const response = await fetch("/api/local/user/public/exist/email", {
		method: "POST",
		headers: { "Content-Type": "application/x-www-form-urlencoded" },
		body: new URLSearchParams({ email })
	});


	if (response.ok) {
		// 200 → 중복 없음
		successHandler();
	} else {
		errorHandler(response.status);
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
	validate('code',code);

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
		throw error;	}

}

export function startVerificationTimer(onTimeout) {
	let timeLeft = 180;
	const timerElement = document.getElementById("verificationTimeMessage");

	timerElement.textContent = formatTime(timeLeft);

	if (timerInterval) clearInterval(timerInterval); // 중복 방지

	timerInterval = setInterval(() => {
		timeLeft--;
		timerElement.textContent = formatTime(timeLeft);

		if (timeLeft <= 0) {
			clearInterval(timerInterval);
			timerInterval = null;
			onTimeout();
		}
	}, 1000);
}

function formatTime(seconds) {
	const m = String(Math.floor(seconds / 60)).padStart(2, "0");
	const s = String(seconds % 60).padStart(2, "0");
	return `${m}:${s}`;
}

