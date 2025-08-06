package net.xrftech.chain.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xrftech.chain.data.entity.AddressChain;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AddressChainMapper extends BaseMapper<AddressChain> {
}