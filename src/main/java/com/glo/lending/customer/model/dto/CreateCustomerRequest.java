package com.glo.lending.customer.model.dto;

import com.glo.lending.customer.model.enums.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class CreateCustomerRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email;

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber;

        @NotBlank(message = "ID number is required")
        @Size(max = 30, message = "ID number must not exceed 30 characters")
        String idNumber;

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth;

        CustomerStatus status;
}

