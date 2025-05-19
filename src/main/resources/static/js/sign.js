import { formatEmail,formatPasswords,validateConfirmPasswords,formatName,formatBirth,formatPhoneNumber }
 from '/js/format.js';
 
 export async function signIn(email, password, rememberEmail, successHandler, errorHandler){

 	if (!formatEmail(email)) {
 	errorHandler({ type: "VALIDATION", message: "이메일 형식이 올바르지 않습니다." });
 	  return;
 	}
 	if (!formatPasswords(password)) {
 	errorHandler({ type: "VALIDATION", message: "비밀번호 형식이 올바르지 않습니다." });
 	  return;
 	}
 	const data = {
 		email:email,
 		password:password
 	};
 	
 	const response = await fetch("/api/sign/public/in/local", {
 	  method: "POST",
 	  headers: {
 	    "Content-Type": "application/json"
 	  },
 	  body: JSON.stringify(data)
 	});

 	if (response.ok) {
 		if (rememberEmail) {
 		  localStorage.setItem("rememberEmail", email);
 		} else {
 		  localStorage.removeItem("rememberEmail");
 		}
		const json = await response.json();
 		successHandler(json);
 	} else {
 		errorHandler({ type: "SERVER", status: response.status});
 	}
 }

export async function signOut(successHandler, errorHandler) {
    const response = await fetch("/api/sign/public/out", {
        method: "POST"
    });
    if (response.ok) {
		successHandler();
    } else {
        errorHandler(response.status);
    }
}

export async function signUpStep1(email, password, confirmPassword, successHandler, errorHandler) {
  if (!formatEmail(email)) {
	errorHandler({ type: "VALIDATION", message: "이메일 형식이 올바르지 않습니다." });
    return;
  }
  if (!formatPasswords(password)) {
	errorHandler({ type: "VALIDATION", message: "비밀번호 형식이 올바르지 않습니다." });
    return;
  }
  if (!validateConfirmPasswords(password, confirmPassword)) {
	errorHandler({ type: "VALIDATION", message: "비밀번호가 일치하지 않습니다." });
    return;
  }

    const response = await fetch("/api/sign/public/up/first", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
	  body: new URLSearchParams({ email, password, confirmPassword })
    });

    if (response.ok) {
      const json = await response.json();
      successHandler(json);
    } else {
	  errorHandler({ type: "SERVER", status: response.status, message:  response.message});
    }
 
}


export async function signUpStep2(
	name,
	birthInput,
	phoneInput,
	postcode,
	mainAddress,
	detailAddress,
	agreeToTerms,
	marketingReceivedStatus,
	successHandler, errorHandler) {
		
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
		if (postcode==="" || mainAddress==="") {
			errorHandler({ type: "VALIDATION", message: "주소 형식이 올바르지 않습니다." });
			return;
		}
		if (agreeToTerms===false) {
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
      const json = await response.json();
      successHandler(json);
    } else {
		errorHandler({ type: "SERVER", status: response.status, message:  response.message});
	}
	
}
