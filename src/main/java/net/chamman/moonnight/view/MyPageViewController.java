package net.chamman.moonnight.view;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.address.AddressResponseDto;
import net.chamman.moonnight.domain.address.AddressService;
import net.chamman.moonnight.domain.estimate.EstimateResponseDto;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserResponseDto;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;

@Controller
@RequiredArgsConstructor
public class MyPageViewController {
	
	private final AddressService addressService;
	private final EstimateService estimateService; 
	private final UserService userService; 
	
	
	@GetMapping("/my")
	public String showMy(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		return "my/my";
	}
	
    @GetMapping("/my/addressBook")
    public String showMyAddressBook(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        List<AddressResponseDto> addressList = addressService.getAddressList(userDetails.getUserId());
        model.addAttribute("addressList", addressList);

        return "my/myAddressBook";
    }
	
	@GetMapping("/my/addressBook/Blank")
	public String showMyAddressBookBlank(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
        List<AddressResponseDto> addressList = addressService.getAddressList(userDetails.getUserId());
        model.addAttribute("addressList", addressList);
        
		return "my/myAddressBookBlank";
	}
	
	@GetMapping("/my/estimate")
	public String showMyEstimate(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
		List<EstimateResponseDto> estimateList = estimateService.getMyAllEstimate(userDetails.getUserId());
        model.addAttribute("estimateList", estimateList);

		return "my/myEstimate";
	}
	
	@GetMapping("/my/loginInfo")
	public String showMyLoginInfo(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
		User user = userService.getUserByUserId(userDetails.getUserId());
        model.addAttribute("user", UserResponseDto.fromEntity(user));

		return "my/myLoginInfo";
	}
	
	@GetMapping("/my/profile")
	public String showMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
		
		User user = userService.getUserByUserId(userDetails.getUserId());
        model.addAttribute("user", UserResponseDto.fromEntity(user));
        
		return "my/myProfile";
	}
	
	@GetMapping("/my/withdrawal")
	public String showWithdrawal() {
		
		return "my/withdrawal";
	}
	
}
