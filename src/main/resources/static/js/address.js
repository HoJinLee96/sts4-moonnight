function validateAddress(address,errorHandler){
	if(address.postcode=="" || address.mainAddress==""){
		alert("주소를 입력해 주세요.");
		errorHandler({ type: "VALIDATION", status: 400, message: ""});
	}
}

export async function getMyAddress(addressId, successHandler, errorHandler){
	const response = await fetch("/api/address/private/"+addressId,{
		method: "GET"
	});
	
	if(response.ok){
		const json = await response.json(); 
		successHandler(json);
	}else{
		const json = await response.json(); 
		errorHandler(json);
	}
}

export async function getMyAddressList(successHandler, errorHandler){
	const response = await fetch("/api/address/private/getList",{
		method: "GET"
	});
	
	if(response.ok){
		const json = await response.json(); 
		successHandler(json);
	}else{
		const json = await response.json(); 
		errorHandler(json);
	}
}

export async function registerAddress(name, postcode, mainAddress, detailAddress, successHandler, errorHandler){
 	var address ={
			name : name,
			postcode : postcode,
			mainAddress : mainAddress,
			detailAddress : detailAddress,
			isPrimary : false
	};
	
	validateAddress(address,errorHandler());
	
	const response = await fetch("/api/address/private/register", {
	  method: "POST",
	  headers: {
	    "Content-Type": "application/json"
	  },
	  body: JSON.stringify(address)
	});
	
	if (response.ok) {
		const json = await response.json();
		alert(json);
		successHandler();
	} else {
		errorHandler({ type: "SERVER", status: response.status});
	}
    
}

export async function updateAddress(addressId, name, postcode, mainAddress, detailAddress, successHandler, errorHandler){
	var address ={
			name : name,
			postcode : postcode,
			mainAddress : mainAddress,
			detailAddress : detailAddress,
			isPrimary : false
	};
	
	validateAddress(address,errorHandler());
	
	const response = await fetch("/api/address/private/"+addressId, {
	  method: "PATCH",
	  headers: {
	    "Content-Type": "application/json"
	  },
	  body: JSON.stringify(address)
	});

	if (response.ok) {
		const json = await response.json();
		successHandler(json);
	} else {
		errorHandler({ type: "SERVER", status: response.status});
	}
}

export async function updatePrimary(addressId, successHandler, errorHandler){
	const response = await fetch("/api/address/private/primary/"+addressId, {
	  method: "PATCH",
	});

	if (response.ok) {
		successHandler();
	} else {
		errorHandler({ type: "SERVER", status: response.status});
	}
}

export async function deleteAddress(addressId, successHandler, errorHandler) {
    if(confirm("정말로 삭제하시겠습니까?")) {
		const response = await fetch("/api/address/private/"+addressId, {
		  method: "DELETE",
		});

		if (response.ok) {
			successHandler();
		} else {
			errorHandler({ type: "SERVER", status: response.status});
		}
    }
}