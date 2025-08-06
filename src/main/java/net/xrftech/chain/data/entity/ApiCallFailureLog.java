package net.xrftech.chain.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("api_call_failure_log")
public class ApiCallFailureLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("chain_id")
    private String chainId;
    
    @TableField("block_number")
    private Long blockNumber;
    
    @TableField("status_code")
    private String statusCode;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}