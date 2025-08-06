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
@TableName("status")
public class Status {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("status_type")
    private String statusType;
    
    @TableField("status_code")
    private String statusCode;
    
    @TableField("status_description")
    private String statusDescription;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}