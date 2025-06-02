let timerInterval = null;
let smsId = 0; // sms 인증 요청 여부

// ======= 휴대폰 유효성 + 중복 검사 =======
export async function validatePhone(phone, successHandler, errorHandler) {
	var regex = /^\d{3,4}-\d{3,4}-\d{4}$/;

  if (!regex.test(phone)) {
    errorHandler(404);
    return;
  }

    const response = await fetch("/api/local/user/public/exist/phone", {
      method: "POST",
	  headers: { "Content-Type": "application/x-www-form-urlencoded" },
	  body: new URLSearchParams({ phone })
	});

	if (response.ok) {
	  successHandler();
	}  else {
	  errorHandler(response.status);
	}
 
}


// ======= 휴대폰 인증번호 발송 =======
export async function sendSms(phone, successHandler, errorHandler) {
	
    const response = await fetch("/api/verify/public/sms", {
      method: "POST",
	  headers: { "Content-Type": "application/x-www-form-urlencoded" },
	  body: new URLSearchParams({ phone })
    });

    if (response.ok) {
		smsId=1;
		successHandler();
    } else {
		errorHandler(response.status);
    }
}

// ======= 휴대폰 인증번호 검증 =======
export async function verifySmsCode(phone, code, successHandler, errorHandler) {

  if (smsId === 0 || code.length < 6) {
    errorHandler(400); // 인증번호 없음 또는 너무 짧음
    return;
  }

    const body = {
      phone: phone,
      verificationCode: code
    };

    const response = await fetch("/api/verify/public/compare/sms/uuid", {
      method: "POST",
	  headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    if (response.ok) {
      clearInterval(timerInterval);
      timerInterval = null;
      successHandler();
    } else {
      errorHandler(response.status);
    }
  
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

