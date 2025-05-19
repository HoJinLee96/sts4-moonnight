package net.chamman.moonnight.global.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.chamman.moonnight.global.annotation.ValidId;

public class IdValidator implements ConstraintValidator<ValidId, String> {

  private static final String ID_REGEX = "^\\{1,10}$";

  @Override
  public void initialize(ValidId constraintAnnotation) {
    // 초기화 로직 필요하면 여기에 (지금은 없음)
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) return false;
    return value.matches(ID_REGEX);
  }
  
}