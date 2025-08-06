package net.xrftech.chain.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xrftech.chain.data.entity.Status;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatusMapper extends BaseMapper<Status> {
}