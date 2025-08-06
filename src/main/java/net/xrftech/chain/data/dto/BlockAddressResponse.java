package net.xrftech.chain.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockAddressResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("data")
    private BlockAddressData data;
    
    @JsonProperty("error")
    private ErrorDetails error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockAddressData {
        
        @JsonProperty("blockHeight")
        private String blockHeight;
        
        @JsonProperty("blockHash")
        private String blockHash;
        
        @JsonProperty("addresses")
        private List<String> addresses;
        
        @JsonProperty("transactionCount")
        private Integer transactionCount;
        
        @JsonProperty("uniqueAddressCount")
        private Integer uniqueAddressCount;
        
        @JsonProperty("timestamp")
        private Instant timestamp;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
    }
}