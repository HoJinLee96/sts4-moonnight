package net.chamman.moonnight.domain.estimate;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.global.util.StringListConverter;

@Entity
@Table(name = "estimate")
@Builder
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Estimate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "estimate_id")
	private int estimateId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_user_TO_estimate_1"))
	private User user;
	
	@Column(name = "name", length = 20, nullable = false)
	private String name;
	
	@Column(name = "phone", length = 20, nullable = false)
	private String phone;
	
	@Column(name = "email", length = 50, nullable = false)
	private String email;
	
	@Column(name = "email_agree")
	private boolean emailAgree;
	
	@Column(name = "phone_agree")
	private boolean phoneAgree;
	
	@Column(name = "postcode", length = 10, nullable = false)
	private String postcode;
	
	@Column(name = "main_address", length = 255, nullable = false)
	private String mainAddress;
	
	@Column(name = "detail_address", length = 255, nullable = false)
	private String detailAddress;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "cleaning_service", nullable = false)
	private CleaningService cleaningService;
	
	@Column(name = "content", length = 5000)
	private String content;
	
	@Column(name = "images_path", length = 5000)
	@Convert(converter = StringListConverter.class) // List<String> 변환
	private List<String> imagesPath;
	
	@Enumerated(EnumType.STRING)
	@Basic(fetch = FetchType.EAGER)
	@Column(name = "estimate_status", nullable = false)
	private EstimateStatus estimateStatus;
	
	@Column(name = "request_ip", length = 50, nullable = false)
	private String requestIp;
	
	@Generated(event = EventType.INSERT)
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	
	@Generated(event = EventType.UPDATE)
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	public enum CleaningService {
		신축, 이사, 거주, 리모델링, 준공, 상가, 오피스, 기타
	}
	
	public enum EstimateStatus {
		RECEIVE, IN_PROGRESS, COMPLETE, DELETE
	}
}

