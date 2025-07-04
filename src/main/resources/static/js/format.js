// /js/format.js (최종 업데이트 버전)

/**
 * input 요소에 실시간으로 전화번호 형식(010-1234-5678)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initPhoneFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		const rawValue = e.target.value.replace(/\D/g, '').substring(0, 12);
		const length = rawValue.length;

		let formattedValue = '';

		// 길이에 따라 하이픈(-)을 추가합니다.
		if (length < 4) {
			formattedValue = rawValue;
		} else if (length < 8) {
			formattedValue = `${rawValue.substring(0, 3)}-${rawValue.substring(3)}`;
		} else if (length < 12) {
			formattedValue = `${rawValue.substring(0, 3)}-${rawValue.substring(3, 7)}-${rawValue.substring(7)}`;
		} else {
			formattedValue = `${rawValue.substring(0, 4)}-${rawValue.substring(4, 8)}-${rawValue.substring(8)}`;
		}

		if (rawValue.startsWith('02')) {
			if (length < 3) {
				formattedValue = rawValue;
			} else if (length < 7) { 
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2)}`;
			} else if (length < 10) {
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2, 5)}-${rawValue.substring(5)}`;
			} else if (length < 11) {
				formattedValue = `${rawValue.substring(0, 2)}-${rawValue.substring(2, 6)}-${rawValue.substring(6)}`;
			}
		}

		e.target.value = formattedValue;
	});
}

/**
 * input 요소에 실시간으로 생년월일 형식(YYYYMMDD)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initBirthFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		e.target.value = e.target.value.replace(/\D/g, '').substring(0, 8);
	});
}

/**
 * input 요소에 실시간으로 인증번호 형식(6자리 숫자)을 적용합니다.
 * @param {HTMLInputElement} inputElement - 형식을 적용할 input 요소
 */
export function initVerificationCodeFormatting(inputElement) {
	if (!inputElement) return;

	inputElement.addEventListener('input', (e) => {
		e.target.value = e.target.value.replace(/\D/g, '').substring(0, 6);
	});
}