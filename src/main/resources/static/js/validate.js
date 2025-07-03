export class ValidationError extends Error {
	constructor(message) {
		super(message); // 부모 Error 클래스의 생성자 호출
		this.code = 400; // HTTP 상태 코드
		this.type = 'VALIDATION'; // 에러 타입
	}
}

const validationRules = {
	email: {
		pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
		emptyMessage: '이메일을 입력해주세요.',
		invalidMessage: '올바른 이메일 형식이 아닙니다.'
	},
	password: {
		pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/,
		emptyMessage: '비밀번호를 입력해주세요.',
		invalidMessage: '비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.'
	},
	phone: {
		pattern: /^\d{3,4}-\d{3,4}-\d{4}$/,
		emptyMessage: '휴대폰 번호를 입력해주세요.',
		invalidMessage: '올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)'
	},
	code: {
		pattern: /^\d{6}$/,
		emptyMessage: '인증코드를 입력해주세요.',
		invalidMessage: '올바른 인증코드 형식이 아닙니다. (예: 012345)'
	},
	name: {
		pattern: /^[가-힣a-zA-Z0-9 ]+$/,
		emptyMessage: '이름을 입력해주세요.',
		invalidMessage: '이름에는 특수기호 또는 줄바꿈을 포함할 수 없습니다.'
	},
	birth: {
		validator: (value) => isValidDate(value),
		emptyMessage: '생년월일을 입력해주세요.',
		invalidMessage: '올바른 생년월일 8자리를 입력해주세요. (예: 19960101)'
	},
	address: {
		pattern: /.+/, // 한 글자 이상 아무 문자나 있으면 통과
		emptyMessage: '주소를 입력해주세요.',
		invalidMessage: '주소를 올바르게 입력해주세요.' // 사실상 호출될 일 없음
	}
};

/**
 * 범용 유효성 검사 함수
 * @param {('email'|'password'|'phone')} type - 유효성 검사 타입
 * @param {string} value - 검사할 값
 */
export function validate(type, value) {
	const rule = validationRules[type];

	if (!rule) {
		throw new ValidationError(`'${type}'에 대한 유효성 검사 규칙이 정의되지 않았습니다.`);
	}
	if(value==null){
		throw new ValidationError(`'${type}'검사 값이 비어있습니다.`);
	}
	value = value.trim(); 

	// 1. 빈 값 검사
	if (value === '') {
		throw new ValidationError(rule.emptyMessage);
	}

	// 2. 정규식 패턴 검사
	if (!rule.pattern.test(value)) {
		console.log("검증값: " + value);
		throw new ValidationError(rule.invalidMessage);
	}
}

function isValidDate(dateString) {
	if (dateString.length !== 8) return false;
	const year = parseInt(dateString.substring(0, 4), 10);
	const month = parseInt(dateString.substring(4, 6), 10);
	const day = parseInt(dateString.substring(6, 8), 10);

	const date = new Date(year, month - 1, day);
	return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}