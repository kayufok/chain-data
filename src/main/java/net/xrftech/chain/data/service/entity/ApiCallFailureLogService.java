package net.xrftech.chain.data.service.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.ApiCallFailureLogDto;
import net.xrftech.chain.data.entity.ApiCallFailureLog;
import net.xrftech.chain.data.mapper.ApiCallFailureLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiCallFailureLogService {

    private final ApiCallFailureLogMapper failureLogMapper;

    public List<ApiCallFailureLogDto> getAllFailureLogs() {
        log.info("Fetching all failure logs");
        List<ApiCallFailureLog> failureLogs = failureLogMapper.selectList(null);
        return failureLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ApiCallFailureLogDto> getFailureLogById(Long id) {
        log.info("Fetching failure log by id: {}", id);
        ApiCallFailureLog failureLog = failureLogMapper.selectById(id);
        return failureLog != null ? Optional.of(convertToDto(failureLog)) : Optional.empty();
    }

    public List<ApiCallFailureLogDto> getFailureLogsByChainId(String chainId) {
        log.info("Fetching failure logs by chain ID: {}", chainId);
        QueryWrapper<ApiCallFailureLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chain_id", chainId);
        List<ApiCallFailureLog> failureLogs = failureLogMapper.selectList(queryWrapper);
        return failureLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ApiCallFailureLogDto> getFailureLogsByStatusCode(String statusCode) {
        log.info("Fetching failure logs by status code: {}", statusCode);
        QueryWrapper<ApiCallFailureLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status_code", statusCode);
        List<ApiCallFailureLog> failureLogs = failureLogMapper.selectList(queryWrapper);
        return failureLogs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiCallFailureLogDto createFailureLog(ApiCallFailureLogDto failureLogDto) {
        log.info("Creating new failure log for chain: {}, block: {}", 
                failureLogDto.getChainId(), failureLogDto.getBlockNumber());
        
        ApiCallFailureLog failureLog = convertToEntity(failureLogDto);
        failureLogMapper.insert(failureLog);
        log.info("Created failure log with id: {}", failureLog.getId());
        return convertToDto(failureLog);
    }

    @Transactional
    public void deleteFailureLog(Long id) {
        log.info("Deleting failure log with id: {}", id);
        
        ApiCallFailureLog existingFailureLog = failureLogMapper.selectById(id);
        if (existingFailureLog == null) {
            throw new IllegalArgumentException("Failure log with id " + id + " not found");
        }
        
        failureLogMapper.deleteById(id);
        log.info("Deleted failure log with id: {}", id);
    }

    @Transactional
    public void deleteFailureLogsByChainId(String chainId) {
        log.info("Deleting failure logs for chain ID: {}", chainId);
        QueryWrapper<ApiCallFailureLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chain_id", chainId);
        int deletedCount = failureLogMapper.delete(queryWrapper);
        log.info("Deleted {} failure logs for chain ID: {}", deletedCount, chainId);
    }

    private ApiCallFailureLogDto convertToDto(ApiCallFailureLog failureLog) {
        return ApiCallFailureLogDto.builder()
                .id(failureLog.getId())
                .chainId(failureLog.getChainId())
                .blockNumber(failureLog.getBlockNumber())
                .statusCode(failureLog.getStatusCode())
                .errorMessage(failureLog.getErrorMessage())
                .createdAt(failureLog.getCreatedAt())
                .build();
    }

    private ApiCallFailureLog convertToEntity(ApiCallFailureLogDto dto) {
        return ApiCallFailureLog.builder()
                .id(dto.getId())
                .chainId(dto.getChainId())
                .blockNumber(dto.getBlockNumber())
                .statusCode(dto.getStatusCode())
                .errorMessage(dto.getErrorMessage())
                .build();
    }
}