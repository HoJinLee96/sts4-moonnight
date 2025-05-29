package net.chamman.moonnight.global.util;

public class LogMaskingUtil {
	
	public enum MaskLevel {
		STRONG,   // 운영용: 거의 안 보임
		MEDIUM,   // 개발용: 일부 보임
		NONE      // 디버깅용: 마스킹 없음
	}
	
	public static String maskEmail(String email, MaskLevel level) {
		if (email == null || !email.contains("@")) return "****";
		String[] parts = email.split("@");
		String local = parts[0];
		String domain = parts[1];
		
		return switch (level) {
		case STRONG -> local.charAt(0) + "****@" + domain;
		case MEDIUM -> local.substring(0, Math.min(4, local.length())) + "****@" + domain;
		case NONE -> email;
		};
	}
	
	public static String maskPhone(String phone, MaskLevel level) {
		if (phone == null || phone.length() < 7) return "****";
		
		return switch (level) {
		case STRONG -> phone.substring(0, 3) + "-****-" + phone.substring(phone.length() - 4);
		case MEDIUM -> phone.substring(0, 7) + "-****";
		case NONE -> phone;
		};
	}
	
	public static String maskName(String name, MaskLevel level) {
		if (name == null || name.isEmpty()) return "****";
		
		return switch (level) {
		case STRONG -> name.charAt(0) + "*".repeat(name.length() - 1);
		case MEDIUM -> name.substring(0, 2) + "*".repeat(name.length() - 2);
		case NONE -> name;
		};
	}
	
	public static String maskBirth(String birth, MaskLevel level) {
		if (birth == null || birth.length() < 4) return "****";
		
		return switch (level) {
		case STRONG -> birth.substring(0, 2) + "**-**";
		case MEDIUM -> birth.substring(0, 4) + "-**";
		case NONE -> birth;
		};
	}
	
	public static String maskJwt(String jwt, MaskLevel level) {
		if (jwt == null || jwt.length() < 10) return "****";
		
		return switch (level) {
		case STRONG -> jwt.substring(0, 5) + "..." + jwt.substring(jwt.length() - 5);
		case MEDIUM -> jwt.substring(0, 15) + "...";
		case NONE -> jwt;
		};
	}
	
	public static String maskVerificationCode(String code, MaskLevel level) {
		if (code == null) return "****";
		
		return switch (level) {
		case STRONG, MEDIUM -> "******";
		case NONE -> code;
		};
	}
	
	public static String maskAddress(String main, String detail, MaskLevel level) {
		String maskedMain = (main == null || main.length() < 6)
				? "주소비공개"
						: switch (level) {
						case STRONG -> main.substring(0, 6) + "****";
						case MEDIUM -> main.substring(0, Math.min(15, main.length())) + "...";
						case NONE -> main;
						};
						
						String maskedDetail = (detail == null || detail.isEmpty())
								? ""
										: switch (level) {
										case STRONG -> " ****";
										case MEDIUM -> " " + detail.charAt(0) + "****";
										case NONE -> " " + detail;
										};
										
										return maskedMain + maskedDetail;
	}
	
	public static String maskPostcode(String postcode, MaskLevel level) {
		if (postcode == null || postcode.length() < 3) return "****";
		
		return switch (level) {
		case STRONG -> postcode.substring(0, 3) + "**";
		case MEDIUM -> postcode.substring(0, 5) + "*";
		case NONE -> postcode;
		};
	}
	
	public static String maskIp(String ip, MaskLevel level) {
		if (ip == null || !ip.contains(".")) return "****";
		String[] parts = ip.split("\\.");
		if (parts.length < 4) return "****";
		
		return switch (level) {
		case STRONG -> parts[0] + "." + parts[1] + ".*.*";
		case MEDIUM -> parts[0] + "." + parts[1] + "." + parts[2] + ".*";
		case NONE -> ip;
		};
	}
}