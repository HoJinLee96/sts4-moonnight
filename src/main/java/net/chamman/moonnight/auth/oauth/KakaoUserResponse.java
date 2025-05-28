package net.chamman.moonnight.auth.oauth;

import lombok.Data;

@Data
public class KakaoUserResponse {
	private Long id;
	private Boolean has_signed_up;
	private String connected_at;
	private String synched_at;
	private KakaoProperties properties;
	private KakaoAccount kakao_account;

	@Data
	public static class KakaoProperties {
		private String nickname;
		private String profile_image;
		private String thumbnail_image;
	}

	@Data
	public static class KakaoAccount {
		private Boolean profile_nickname_needs_agreement;
		private Boolean profile_image_needs_agreement;
		private KakaoProfile profile;
		private Boolean has_email;
		private Boolean email_needs_agreement;
		private Boolean is_email_valid;
		private Boolean is_email_verified;
		private String email;
		private String gender;
		private String age_range;
		private String birthday;
		private String birthyear;
		private String phone_number;
	}

	@Data
	public static class KakaoProfile {
		private String nickname;
		private String thumbnail_image_url;
		private String profile_image_url;
	}
}
