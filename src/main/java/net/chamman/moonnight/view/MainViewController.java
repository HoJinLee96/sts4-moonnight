package net.chamman.moonnight.view;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class MainViewController {
	
	@GetMapping("/swagger-ui/")
	public String redirectToSwagger() {
		return "forward:/swagger-ui/index.html";
	}
	
	@GetMapping("/error")
	public String showError() {
		System.out.println("---------- WebMainController.error() 실행 ----------");
		return "error";
	}
	
	
	@GetMapping({"/", "/home"})
	public String showHome() {
		return "home";
	}
	
	@GetMapping("/estimate")
	public String showEstimate() {
		return "estimate/estimate";
	}
	
	@GetMapping("/review")
	public String showReview(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showReview() 실행----------");
		return "review";
	}
	
	@GetMapping("/find/email")
	public String showFindEmail(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showFindEmail() 실행----------");
		return "findEmailBlank";
	}
	
	@GetMapping("/find/password")
	public String showFindPassword(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showFindId() 실행----------");
		return "findPasswordBlank";
	}
	
	@GetMapping("/update/password/blank")
	public String showUpdatePasswordBlank(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.verifyPhoneBlank() 실행----------");
		return "updatePasswordBlank";
	}
   
}
