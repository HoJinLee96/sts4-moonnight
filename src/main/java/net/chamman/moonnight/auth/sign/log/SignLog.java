package net.chamman.moonnight.auth.sign.log;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chamman.moonnight.domain.user.User.UserProvider;

@Entity
@Table(name="sign_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SignLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name ="sign_log_id")
  private int signLogId;

  @Enumerated(EnumType.STRING)
  @Basic(fetch = FetchType.EAGER)
  @Column(name ="user_provider")
  private UserProvider userProvider;
  
  @Column(name ="email", length=100)
  private String email;
  
  @Column(name = "request_ip", length = 50, nullable = false)
  private String requestIp;
  
  @Enumerated(EnumType.STRING)
  @Basic(fetch = FetchType.EAGER)
  @Column(name = "sign_result", nullable = false)
  private SignResult signResult;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolve_by", referencedColumnName = "sign_log_id", foreignKey = @ForeignKey(name = "sign_log_resolve_by"))
  private SignLog resolveBy;
  
  @Generated(event = EventType.INSERT)
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Generated(event = EventType.UPDATE)
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  public enum SignResult {
    //로그인 성공
    LOCAL_SUCCESS,
    
    OAUTH_SUCCESS,
    
    AUTH_SUCCESS,
    
    REFRESH,
    
    //일치하는 이메일 없음
    INVALID_EMAIL,
    
    //비밀번호 불일치
    INVALID_PASSWORD,
    
    //비밀번호 업데이트
    UPDATE_PASSWORD,
    
    LOCAL_FAIL,
    
    OAUTH_FAIL,
    
    AUTH_FAIL,
    
    //IP 차단
    IP_BLOCKED,
    
    //서버 에러
//    SERVER_ERROR,
    
    BLACKLIST_TOKEN,
    
    REFRESH_FAIL,
    
    // 계정이 STAY 상태 (이메일/휴대폰 인증 필요)
    ACCOUNT_STAY,
    
    // 계정이 STOP 상태 (정지됨)
    ACCOUNT_STOP,
    
    // 계정이 DELETE 상태 (탈퇴됨)
    ACCOUNT_DELETE;
    
  }
  
}