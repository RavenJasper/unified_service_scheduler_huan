package com.unified.scheduler.dto;

import com.unified.scheduler.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Customer profile representation")
public record CustomerDto(
    @Schema(description = "Customer unique ID", example = "1")
    Long id,

    @Schema(description = "Natural customer business code", example = "CUST-001")
    String customerCode,

    @Schema(description = "First name", example = "John")
    String firstName,

    @Schema(description = "Last name", example = "Doe")
    String lastName,

    @Schema(description = "Full name", example = "John Doe")
    String fullName,

    @Schema(description = "Email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Phone number", example = "+1-555-0101")
    String phone,

    @Schema(description = "Whether the customer account is active")
    boolean isActive,

    @Schema(description = "Creation timestamp")
    OffsetDateTime createdAt,

    @Schema(description = "Last update timestamp")
    OffsetDateTime updatedAt
) {
    public static CustomerDto fromEntity(Customer customer) {
        return new CustomerDto(
            customer.getId(),
            customer.getCustomerCode(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.isActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
