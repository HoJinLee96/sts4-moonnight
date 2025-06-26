import { validate, ValidationError } from '/js/validate.js';

/**
 * 견적서 폼의 유효성을 검사하는 함수
 * @param {object} estimateData - 견적서 데이터 객체
 * @param {File[]} imageFiles - 첨부된 이미지 파일 목록
 */
function validateEstimate(estimateData, imageFiles) {
	
	validate('name',estimateData.name);
	if (!estimateData.emailAgree && !estimateData.phoneAgree) {
		const error = new Error('최소 하나 이상의 수신 방법을 인증해주세요.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
	if(estimateData.phoneAgree){
		validate('phone',estimateData.phone);
	}else{
		validate('email',estimateData.email);
	}
	
	if (!estimateData.postcode || !estimateData.mainAddress || !estimateData.detailAddress) {
		const error = new Error('이름, 연락처, 주소를 입력해 주세요.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}

	if (imageFiles && imageFiles.length > 10) {
		const error = new Error('이미지는 최대 10장까지 업로드할 수 있습니다.'); 
		error.code = 400;
		error.type = "VALIDATION";
		throw error;
	}
}

/**
 * 견적서를 서버에 등록합니다. (파일 포함)
 * @param {object} estimateDto - 견적서의 텍스트 데이터 (name, phone, content 등)
 * @param {File[]} imageFiles - 사용자가 선택한 이미지 파일의 배열
 * @returns {Promise<object>} 성공 시 서버로부터 받은 데이터
 */
export async function registerEstimate(estimateDto, imageFiles) {
	validateEstimate(estimateDto, imageFiles);

	// 1. FormData 객체 생성
	const formData = new FormData();

	// 2. DTO 객체를 JSON 문자열로 변환 후 Blob으로 만들어 FormData에 추가
    //    @RequestPart("estimate")에 해당함
	formData.append(
		'estimateRequestDto', 
		new Blob([JSON.stringify(estimateDto)], { type: "application/json" })
	);

	// 3. 이미지 파일들을 FormData에 추가
    //    @RequestPart("images")에 해당함
	if (imageFiles && imageFiles.length > 0) {
		imageFiles.forEach(file => {
			formData.append('images', file);
		});
	}

	// 4. 요청 전송
	const response = await fetch("/api/estimate/public/register", {
		method: "POST",
		body: formData
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

/**
 * 나의 모든 견적서 목록을 가져옵니다.
 * @returns {Promise<object>} 견적서 목록 데이터
 */
export async function getMyAllEstimate() {
	const response = await fetch("/api/estimate/private/user", {
		method: "GET"
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

/**
 * 특정 견적서를 조회합니다.
* @param {number} estimateId - 조회할 견적서의 ID
 */
export async function getEstimate(estimateId) {
	const response = await fetch(`/api/estimate/private/user/${estimateId}`, {
		method: "GET"
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

/**
 * 견적서를 수정 합니다.
 * @param {number} estimateId - 수정할 견적서의 ID
 */
export async function updateEstimate(estimateId, estimateDto, imageFiles) {
	validateEstimate(estimateDto, imageFiles);

	// 1. FormData 객체 생성
	const formData = new FormData();

	// 2. DTO 객체를 JSON 문자열로 변환 후 Blob으로 만들어 FormData에 추가
	//    @RequestPart("estimate")에 해당함
	formData.append(
		'estimate', 
		new Blob([JSON.stringify(estimateDto)], { type: "application/json" })
	);

	// 3. 이미지 파일들을 FormData에 추가
	//    @RequestPart("images")에 해당함
	if (imageFiles && imageFiles.length > 0) {
		imageFiles.forEach(file => {
			formData.append('images', file);
		});
	}

	const response = await fetch(`/api/estimate/private/update/${estimateId}`, {
		method: "Patch", 
		body: formData
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


/**
 * 특정 견적서를 삭제합니다.
 * @param {number} estimateId - 삭제할 견적서의 ID
 */
export async function deleteEstimate(estimateId) {
	const response = await fetch(`/api/estimate/private/${estimateId}`, {
		method: "DELETE", 
	});

	if (!response.ok) {
		const json = await response.json();
		const error = new Error(json.message || '서버 요청에 실패했습니다.'); 
		error.code = json.code;
		error.type = "SERVER";
		throw error;
	}
}