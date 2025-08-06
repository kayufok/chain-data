package net.xrftech.chain.data.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressChainDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("walletAddressId")
    @NotNull(message = "Wallet address ID is required")
    private Long walletAddressId;
    
    @JsonProperty("chainId")
    @NotNull(message = "Chain ID is required")
    private Long chainId;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}