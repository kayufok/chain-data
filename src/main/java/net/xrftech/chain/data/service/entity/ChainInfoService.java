package net.xrftech.chain.data.service.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.ChainInfoDto;
import net.xrftech.chain.data.entity.ChainInfo;
import net.xrftech.chain.data.mapper.ChainInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChainInfoService {

    private final ChainInfoMapper chainInfoMapper;

    public List<ChainInfoDto> getAllChains() {
        log.info("Fetching all chains");
        List<ChainInfo> chains = chainInfoMapper.selectList(null);
        return chains.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ChainInfoDto> getChainById(Long id) {
        log.info("Fetching chain by id: {}", id);
        ChainInfo chain = chainInfoMapper.selectById(id);
        return chain != null ? Optional.of(convertToDto(chain)) : Optional.empty();
    }

    public Optional<ChainInfoDto> getChainByChainId(String chainId) {
        log.info("Fetching chain by chain id: {}", chainId);
        QueryWrapper<ChainInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chain_id", chainId);
        ChainInfo chainInfo = chainInfoMapper.selectOne(queryWrapper);
        return chainInfo != null ? Optional.of(convertToDto(chainInfo)) : Optional.empty();
    }

    @Transactional
    public ChainInfoDto createChain(ChainInfoDto chainInfoDto) {
        log.info("Creating new chain: {}", chainInfoDto.getChainName());
        
        // Check if chain ID already exists
        QueryWrapper<ChainInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chain_id", chainInfoDto.getChainId());
        ChainInfo existing = chainInfoMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new IllegalArgumentException("Chain with chain ID " + chainInfoDto.getChainId() + " already exists");
        }
        
        ChainInfo chainInfo = convertToEntity(chainInfoDto);
        chainInfoMapper.insert(chainInfo);
        log.info("Created chain with id: {}", chainInfo.getId());
        return convertToDto(chainInfo);
    }

    @Transactional
    public ChainInfoDto updateChain(Long id, ChainInfoDto chainInfoDto) {
        log.info("Updating chain with id: {}", id);
        
        ChainInfo existingChain = chainInfoMapper.selectById(id);
        if (existingChain == null) {
            throw new IllegalArgumentException("Chain with id " + id + " not found");
        }
        
        // Check for chain ID conflicts
        if (!existingChain.getChainId().equals(chainInfoDto.getChainId())) {
            QueryWrapper<ChainInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("chain_id", chainInfoDto.getChainId());
            ChainInfo conflicting = chainInfoMapper.selectOne(queryWrapper);
            if (conflicting != null && !conflicting.getId().equals(id)) {
                throw new IllegalArgumentException("Chain with chain ID " + chainInfoDto.getChainId() + " already exists");
            }
        }
        
        existingChain.setChainName(chainInfoDto.getChainName());
        existingChain.setChainId(chainInfoDto.getChainId());
        existingChain.setNextBlockNumber(chainInfoDto.getNextBlockNumber());
        
        chainInfoMapper.updateById(existingChain);
        log.info("Updated chain with id: {}", id);
        return convertToDto(existingChain);
    }

    @Transactional
    public void deleteChain(Long id) {
        log.info("Deleting chain with id: {}", id);
        
        ChainInfo existingChain = chainInfoMapper.selectById(id);
        if (existingChain == null) {
            throw new IllegalArgumentException("Chain with id " + id + " not found");
        }
        
        chainInfoMapper.deleteById(id);
        log.info("Deleted chain with id: {}", id);
    }

    private ChainInfoDto convertToDto(ChainInfo chainInfo) {
        return ChainInfoDto.builder()
                .id(chainInfo.getId())
                .chainName(chainInfo.getChainName())
                .chainId(chainInfo.getChainId())
                .nextBlockNumber(chainInfo.getNextBlockNumber())
                .createdAt(chainInfo.getCreatedAt())
                .updatedAt(chainInfo.getUpdatedAt())
                .build();
    }

    private ChainInfo convertToEntity(ChainInfoDto dto) {
        return ChainInfo.builder()
                .id(dto.getId())
                .chainName(dto.getChainName())
                .chainId(dto.getChainId())
                .nextBlockNumber(dto.getNextBlockNumber())
                .build();
    }
}