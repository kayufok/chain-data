package net.xrftech.chain.data.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainInfoDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("chainName")
    @NotBlank(message = "Chain name is required")
    private String chainName;
    
    @JsonProperty("chainId")
    @NotBlank(message = "Chain ID is required")
    private String chainId;
    
    @JsonProperty("nextBlockNumber")
    @Min(value = 0, message = "Next block number must be non-negative")
    private Long nextBlockNumber;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}