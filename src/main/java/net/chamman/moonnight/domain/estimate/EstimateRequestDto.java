package net.chamman.moonnight.domain.estimate;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.user.User;

public record EstimateRequestDto(
    
    @NotBlank(message = "{validation.user.name.required}")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]$", message = "{validation.user.name.invalid}")
    @Size(min = 2, max = 20, message = "{validation.user.name.length}")
    String name,
    
    @NotBlank(message = "{validation.user.phone.required}")
    @Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "{validation.user.phone.invalid}")
    @Size(max = 20, message = "{validation.user.phone.length}")
    String phone,
    
    @NotBlank(message = "{validation.user.email.required}")
    @Pattern(regexp = "^[a-zA-Z0-9]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]$", message = "{validation.user.email.invalid}")
    @Size(min = 5, max = 50, message = "{validation.user.email.length}")
    String email,
    
    boolean emailAgree,
    boolean smsAgree,
    boolean callAgree,
    
    @NotBlank(message = "{validation.address.postcode.required}")
    @Pattern(regexp = "^\\d{10}$", message = "{validation.address.postcode.invalid}")
    @Size(max = 10, message = "{validation.address.postcode.length}")
    String postcode,
    
    @NotBlank(message = "{validation.address.main_address.required}")
    @Size(max = 250, message = "{validation.address.main_address.length}")
    String mainAddress,
    
    @NotBlank(message = "{validation.address.detail_address.required}")
    @Size(max = 250, message = "{validation.address.detail_address.length}")
    String detailAddress,
    
    @NotBlank(message = "{validation.estimate.cleaning_service.required}")
    CleaningService cleaningService,
    
    @Size(max = 5000, message = "{validation.estimate.content.length}")
    String content,
    
    List<String> imagesPath
    ) {
  
  public Estimate toEntity(User user, List<String> imagesPath) {
    return Estimate.builder()
        .user(user)
        .name(name)
        .phone(phone)
        .email(email)
        .emailAgree(emailAgree)
        .smsAgree(smsAgree)
        .callAgree(callAgree)
        .postcode(postcode)
        .mainAddress(mainAddress)
        .detailAddress(detailAddress)
        .cleaningService(cleaningService)
        .content(content)
        .imagesPath(imagesPath)
        .estimateStatus(EstimateStatus.RECEIVE)
        .build();
  }

}
