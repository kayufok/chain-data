package net.xrftech.chain.data.service.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.StatusDto;
import net.xrftech.chain.data.entity.Status;
import net.xrftech.chain.data.mapper.StatusMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusService {

    private final StatusMapper statusMapper;

    public List<StatusDto> getAllStatuses() {
        log.info("Fetching all statuses");
        List<Status> statuses = statusMapper.selectList(null);
        return statuses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<StatusDto> getStatusById(Long id) {
        log.info("Fetching status by id: {}", id);
        Status status = statusMapper.selectById(id);
        return status != null ? Optional.of(convertToDto(status)) : Optional.empty();
    }

    public Optional<StatusDto> getStatusByCode(String statusCode) {
        log.info("Fetching status by code: {}", statusCode);
        QueryWrapper<Status> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status_code", statusCode);
        Status status = statusMapper.selectOne(queryWrapper);
        return status != null ? Optional.of(convertToDto(status)) : Optional.empty();
    }

    @Transactional
    public StatusDto createStatus(StatusDto statusDto) {
        log.info("Creating new status: {}", statusDto.getStatusCode());
        
        // Check if status code already exists
        QueryWrapper<Status> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status_code", statusDto.getStatusCode());
        Status existing = statusMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new IllegalArgumentException("Status with code " + statusDto.getStatusCode() + " already exists");
        }
        
        Status status = convertToEntity(statusDto);
        statusMapper.insert(status);
        log.info("Created status with id: {}", status.getId());
        return convertToDto(status);
    }

    @Transactional
    public StatusDto updateStatus(Long id, StatusDto statusDto) {
        log.info("Updating status with id: {}", id);
        
        Status existingStatus = statusMapper.selectById(id);
        if (existingStatus == null) {
            throw new IllegalArgumentException("Status with id " + id + " not found");
        }
        
        // Check for status code conflicts
        if (!existingStatus.getStatusCode().equals(statusDto.getStatusCode())) {
            QueryWrapper<Status> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status_code", statusDto.getStatusCode());
            Status conflicting = statusMapper.selectOne(queryWrapper);
            if (conflicting != null && !conflicting.getId().equals(id)) {
                throw new IllegalArgumentException("Status with code " + statusDto.getStatusCode() + " already exists");
            }
        }
        
        existingStatus.setStatusType(statusDto.getStatusType());
        existingStatus.setStatusCode(statusDto.getStatusCode());
        existingStatus.setStatusDescription(statusDto.getStatusDescription());
        statusMapper.updateById(existingStatus);
        log.info("Updated status with id: {}", id);
        return convertToDto(existingStatus);
    }

    @Transactional
    public void deleteStatus(Long id) {
        log.info("Deleting status with id: {}", id);
        
        Status existingStatus = statusMapper.selectById(id);
        if (existingStatus == null) {
            throw new IllegalArgumentException("Status with id " + id + " not found");
        }
        
        statusMapper.deleteById(id);
        log.info("Deleted status with id: {}", id);
    }

    private StatusDto convertToDto(Status status) {
        return StatusDto.builder()
                .id(status.getId())
                .statusType(status.getStatusType())
                .statusCode(status.getStatusCode())
                .statusDescription(status.getStatusDescription())
                .createdAt(status.getCreatedAt())
                .build();
    }

    private Status convertToEntity(StatusDto dto) {
        return Status.builder()
                .id(dto.getId())
                .statusType(dto.getStatusType())
                .statusCode(dto.getStatusCode())
                .statusDescription(dto.getStatusDescription())
                .build();
    }
}