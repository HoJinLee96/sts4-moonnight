package net.chamman.moonnight.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.chamman.moonnight.global.annotation.ClientSpecific;

@Controller
public class SignViewController {
	
	@GetMapping("/signin")
	public String showLogin() {
		return "signin/signin";
	}
	
	@GetMapping("/signinBlank")
	public String showLoginBlank() {
		return "signin/signinBlank";
	}
	
	@GetMapping("/signup1")
	public String showJoin() {
		return "signup/signup1";
	}
	
	@GetMapping("/signup2")
	public String showJoin2(@ClientSpecific(required = false, value = "X-Access-SignUp-Token") String accessSignUpToken) {
		if (accessSignUpToken == null || accessSignUpToken.isBlank()) {
			return "redirect:/signup1";
		}
		return "signup/signup2";
	}
	
	@GetMapping("/signup/sns/confirm")
	public String showJoinSnsConfirm(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showJoinSnsConfirm() 실행----------");
		return "joinSnsConfirm";
	}
	
}
