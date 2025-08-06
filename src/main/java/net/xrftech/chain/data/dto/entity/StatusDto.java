package net.xrftech.chain.data.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("statusType")
    @NotBlank(message = "Status type is required")
    private String statusType;
    
    @JsonProperty("statusCode")
    @NotBlank(message = "Status code is required")
    private String statusCode;
    
    @JsonProperty("statusDescription")
    private String statusDescription;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}