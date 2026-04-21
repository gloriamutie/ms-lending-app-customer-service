package com.glo.lending.customer.model.dto;

import com.glo.lending.customer.model.enums.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
@Builder
@Data
public class UpdateCustomerRequest{
    @Size(max = 50) String firstName;
    @Size(max = 50) String lastName;
    @Email String email;
    @Size(max = 20) String phoneNumber;
    CustomerStatus status;
}

