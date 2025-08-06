package net.xrftech.chain.data.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallFailureLogDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("chainId")
    @NotBlank(message = "Chain ID is required")
    private String chainId;
    
    @JsonProperty("blockNumber")
    @NotNull(message = "Block number is required")
    @Min(value = 0, message = "Block number must be non-negative")
    private Long blockNumber;
    
    @JsonProperty("statusCode")
    @NotBlank(message = "Status code is required")
    private String statusCode;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}