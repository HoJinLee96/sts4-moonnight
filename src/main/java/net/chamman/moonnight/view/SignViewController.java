package net.chamman.moonnight.view;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.global.annotation.ClientSpecific;

@Controller
@Slf4j
public class SignViewController {
	
	@GetMapping("/signin")
	public String showLogin(HttpServletRequest req, HttpServletResponse res, Model model) {
		
		String redirect = req.getParameter("redirect");
		log.debug("*req.getParameter(\"redirect\"): {}", redirect);
		if(redirect==null) {
			String referer = req.getHeader("Referer");
			if(referer != null) {
				if (isSameDomain(referer, req)) {
					redirect = Base64.getEncoder().encodeToString(referer.getBytes(StandardCharsets.UTF_8));
				}
			}
		}
		model.addAttribute("redirect", redirect);
		log.debug("*model.addAttribute(\"redirect\": {})", redirect);
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
	@GetMapping("/sign/stay")
	public String showUserStatusStay(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showJoinSnsConfirm() 실행----------");
		return "sign/signStay";
	}
	@GetMapping("/sign/stop")
	public String showUserStatusStop(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showJoinSnsConfirm() 실행----------");
		return "sign/signStop";
	}
	@GetMapping("/sign/delete")
	public String showUserStatusDelete(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("----------WebMainController.showJoinSnsConfirm() 실행----------");
		return "sign/signDelete";
	}
	
    private boolean isSameDomain(String referer, HttpServletRequest request) {
        try {
            URI refererUri = new URI(referer);
            String refererDomain = refererUri.getHost();
            String serverDomain = request.getServerName();
            return refererDomain != null && refererDomain.equalsIgnoreCase(serverDomain);
        } catch (URISyntaxException e) {
            return false;
        }
    }
	
}
