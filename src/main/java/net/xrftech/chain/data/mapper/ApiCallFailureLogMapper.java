package net.xrftech.chain.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xrftech.chain.data.entity.ApiCallFailureLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiCallFailureLogMapper extends BaseMapper<ApiCallFailureLog> {
}