package net.chamman.moonnight.domain.estimate.simple;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.chamman.moonnight.domain.estimate.Estimate.CleaningService;
import net.chamman.moonnight.domain.estimate.Estimate.EstimateStatus;
import net.chamman.moonnight.domain.estimate.simple.SimpleEstimate.Region;

public record SimpleEstimateRequestDto(
    Integer estimateId,
    
    @NotBlank(message = "{validation.user.phone.required}")
    @Pattern(regexp = "^\\d{3,4}-\\d{3,4}-\\d{4}$", message = "{validation.user.phone.invalid}")
    @Size(max = 20, message = "{validation.user.phone.length}")
    String phone,
    
    @NotBlank(message = "{validation.estimate.cleaning_service.required}")
    CleaningService cleaningService,
    
    @NotBlank(message = "{validation.estimate.region.required}")
    Region region
    
    ) {
  
  public SimpleEstimate toEntity(String clientIp) {
    return SimpleEstimate.builder()
        .phone(phone)
        .cleaningService(cleaningService)
        .region(region)
        .estimateStatus(EstimateStatus.RECEIVE)
        .requestIp(clientIp)
        .build();
  }

}
