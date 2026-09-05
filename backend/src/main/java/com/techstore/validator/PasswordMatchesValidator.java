package com.techstore.validator;

import com.techstore.dto.request.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }
        return request.getPassword().equals(request.getConfirmPassword());
    }
}
