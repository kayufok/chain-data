package net.xrftech.chain.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xrftech.chain.data.entity.ApiCallFailureLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApiCallFailureLogMapper extends BaseMapper<ApiCallFailureLog> {
    
    List<ApiCallFailureLog> findByChainId(String chainId);
    
    List<ApiCallFailureLog> findByBlockNumber(Long blockNumber);
}