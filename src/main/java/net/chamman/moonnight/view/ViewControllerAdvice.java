package net.chamman.moonnight.view;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.security.principal.SilentUserDetails;

//@ControllerAdvice
//@RequiredArgsConstructor
//public class ViewControllerAdvice {
//	
////	private final UserService userService;
////  private final AddressService addressService;
//	
//	@ModelAttribute
//	public void addUserDetailsToModel(Model model, @AuthenticationPrincipal SilentUserDetails userDetails) {
//		if (userDetails != null) {
//			// 로그인 되어 있으면 사용자 이름(이나 필요한 다른 정보)을 모델에 추가
////			int userId = userDetails.getUserId();
//			model.addAttribute("userName", userDetails.getName()); // 모델 속성 이름은 원하는 대로 (예: userName)
//			model.addAttribute("isAuthenticated", true);
//		} else {
//			// 로그인 안 되어 있으면 해당 속성 없거나, 로그인 안됐다는 플래그 추가
//			model.addAttribute("isAuthenticated", false);
//		}
//		
//		// String 같은 걸 반환하면 그 값이 특정 이름의 모델 속성으로 들어감.
//	}
//}
