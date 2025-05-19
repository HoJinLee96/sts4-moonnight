package net.chamman.moonnight.global.security.principal;


import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.chamman.moonnight.domain.user.User.UserProvider;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final int userId;
  private final UserProvider userProvider; 
  private final String email; 
  private final String name; 
  private final List<GrantedAuthority> authorities; 
 
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return authorities;
  }

  @Override
  public String getPassword() {
      return null; 
  }

  @Override
  public String getUsername() {
      return email; 
  }

  @Override
  public boolean isAccountNonExpired() {
      return true;
  }

  @Override
  public boolean isAccountNonLocked() {
      return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
      return true;
  }

  @Override
  public boolean isEnabled() {
      return true;
  }

}
