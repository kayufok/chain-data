package net.xrftech.chain.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc;
    
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("result")
    private EthBlock result;
    
    @JsonProperty("error")
    private RpcError error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RpcError {
        
        @JsonProperty("code")
        private Integer code;
        
        @JsonProperty("message")
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EthBlock {
        
        @JsonProperty("number")
        private String number;
        
        @JsonProperty("hash")
        private String hash;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("transactions")
        private List<EthTransaction> transactions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EthTransaction {
        
        @JsonProperty("hash")
        private String hash;
        
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("to")
        private String to;
        
        @JsonProperty("value")
        private String value;
    }
}