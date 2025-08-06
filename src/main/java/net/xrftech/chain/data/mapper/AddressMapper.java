package net.xrftech.chain.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xrftech.chain.data.entity.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}