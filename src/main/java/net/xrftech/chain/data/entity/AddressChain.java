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
@TableName("address_chain")
public class AddressChain {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("wallet_address_id")
    private Long walletAddressId;
    
    @TableField("chain_id")
    private Long chainId;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}