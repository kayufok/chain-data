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
@TableName("chain_info")
public class ChainInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("chain_name")
    private String chainName;
    
    @TableField("chain_id")
    private String chainId;
    
    @TableField("next_block_number")
    private Long nextBlockNumber;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}