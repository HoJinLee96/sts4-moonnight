package net.chamman.moonnight.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class MyPageViewController {
	
	@GetMapping("/my")
	public String showMy() {
		return "my/my";
	}
	
	@GetMapping("/my/loginInfo")
	public String showMyLoginInfo(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showMyLoginInfo() 실행----------");
		return "my/myLoginInfo";
	}
	
	@GetMapping("/my/withdrawal")
	public String showWithdrawal(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showWithdrawal() 실행----------");
		return "my/withdrawal";
	}
	@GetMapping("/my/withdrawalOAuth")
	public String showWithdrawalOAuth(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showWithdrawalOAuth() 실행----------");
		return "my/withdrawalOAuth";
	}
	@GetMapping("/my/addressBook")
	public String showMyAddressBook(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showMyAddress() 실행----------");
		return "my/myAddressBook";
	}
	@GetMapping("/my/addressBook/Blank")
	public String showMyAddressBookBlank(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showMyAddress() 실행----------");
		return "my/myAddressBookBlank";
	}
	@GetMapping("/my/profile")
	public String showMyProfile(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showMyProfile() 실행----------");
		return "my/myProfile";
	}
	@GetMapping("/my/estimate")
	public String showMyEstimate(HttpServletRequest req, HttpServletResponse res,HttpSession session) {
		System.out.println("----------WebMainController.showMyProfile() 실행----------");
		return "my/myEstimate";
	}
	
}
