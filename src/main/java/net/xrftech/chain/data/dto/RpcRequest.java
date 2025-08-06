package net.xrftech.chain.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest {
    
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private List<Object> params;
    
    @JsonProperty("id")
    @Builder.Default
    private Integer id = 1;
}