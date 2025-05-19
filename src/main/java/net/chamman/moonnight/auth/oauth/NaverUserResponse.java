package net.chamman.moonnight.auth.oauth;


import lombok.Data;

@Data
public class NaverUserResponse {
  private String resultcode;
  private String message;
  private NaverUser response;

  @Data
  public static class NaverUser {
    private String id;
    private String nickname;
    private String name;
    private String email;
    private String gender;
    private String age;
    private String birthday;
    private String profile_image;
    private String birthyear;
    private String mobile;
  }
}
