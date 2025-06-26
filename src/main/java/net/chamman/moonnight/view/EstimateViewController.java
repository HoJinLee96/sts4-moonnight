package net.chamman.moonnight.view;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.domain.address.AddressResponseDto;
import net.chamman.moonnight.domain.address.AddressService;
import net.chamman.moonnight.domain.estimate.EstimateResponseDto;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserResponseDto;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Controller
@Slf4j
@RequiredArgsConstructor
public class EstimateViewController {

	private final UserService userService;
	private final AddressService addressService;

	@GetMapping("/estimate")
	public String showEstimate(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {

		if (customUserDetails != null) {
			User user = userService.getUserByUserId(customUserDetails.getUserId());
			List<AddressResponseDto> addressList = addressService.getAddressList(customUserDetails.getUserId());
			model.addAttribute("user", UserResponseDto.fromEntity(user));
			model.addAttribute("addressList", addressList);
			log.debug("addressList: {}", addressList.toArray().toString());
			if (addressList != null && !addressList.isEmpty()) {
				AddressResponseDto primaryAddress = addressList.stream().filter(AddressResponseDto::isPrimary)
						.findFirst().orElse(addressList.get(0));
				model.addAttribute("primaryAddress", primaryAddress);
				log.debug("primaryAddress: {}", primaryAddress);
			}
		}

		return "estimate/estimate";
	}

	/*
	 * 직접 URL 치고 들어간다면?
	 * 
	 * 이전 사용될 견적 등록 api요청에서 FlashAttribute를 써서 막음.
	 * POST 요청 처리 후에 RedirectAttributes에 DTO를 담고,
	 * GET 요청 컨트롤러에서는 @ModelAttribute로 받아서 null이면 MethodArgumentNotValidException 발생 및 홈으로 이동
	 * 
	 * 성공 페이지에서 보여줄 견적 정보를 다시 DB에서 조회할 필요 없이 효율적으로 전달하고, 
	 * 직접적인 URL 접근도 막을 수 있는 좋은 방법.
	 * 하지만 이 방식은 API 컨트롤러와 뷰 컨트롤러 사이에
	 * 'FlashAttribute'라는 숨겨진 암묵적 의존성이 생기는 단점 존재.
	 * API의 순수성 훼손. 강력한 의존성.
	 * 
	 * 우선 FlashAttribute 방식사용, DB 조회를 줄일 수 있고 구현이 간단.
	 * 이후 '일회용 토큰 + 데이터 재조회'로 리팩토링. 이 방식은 DB 조회가 한 번 더 일어나지만, 규모가 커진다면 시스템 전체의 결합도를 낮추기에 더 적합.
	 */
	@GetMapping("/estimate/success")
	public String showEstimateSuccess(@ModelAttribute("estimate") EstimateResponseDto estimateResponseDto,
			Model model) {

		model.addAttribute("estimate", estimateResponseDto);
		return "estimate/successReceive";
	}

}
