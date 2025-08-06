package net.xrftech.chain.data.service.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.AddressChainDto;
import net.xrftech.chain.data.entity.AddressChain;
import net.xrftech.chain.data.mapper.AddressChainMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressChainService {

    private final AddressChainMapper addressChainMapper;

    public List<AddressChainDto> getAllAddressChains() {
        log.info("Fetching all address-chain relationships");
        List<AddressChain> addressChains = addressChainMapper.selectList(null);
        return addressChains.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<AddressChainDto> getAddressChainById(Long id) {
        log.info("Fetching address-chain relationship by id: {}", id);
        AddressChain addressChain = addressChainMapper.selectById(id);
        return addressChain != null ? Optional.of(convertToDto(addressChain)) : Optional.empty();
    }

    @Transactional
    public AddressChainDto createAddressChain(AddressChainDto addressChainDto) {
        log.info("Creating new address-chain relationship: walletAddressId={}, chainId={}", 
                addressChainDto.getWalletAddressId(), addressChainDto.getChainId());
        
        // Check if relationship already exists
        QueryWrapper<AddressChain> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wallet_address_id", addressChainDto.getWalletAddressId())
                   .eq("chain_id", addressChainDto.getChainId());
        AddressChain existing = addressChainMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new IllegalArgumentException("Address-chain relationship already exists");
        }
        
        AddressChain addressChain = convertToEntity(addressChainDto);
        addressChainMapper.insert(addressChain);
        log.info("Created address-chain relationship with id: {}", addressChain.getId());
        return convertToDto(addressChain);
    }

    @Transactional
    public void deleteAddressChain(Long id) {
        log.info("Deleting address-chain relationship with id: {}", id);
        
        AddressChain existingAddressChain = addressChainMapper.selectById(id);
        if (existingAddressChain == null) {
            throw new IllegalArgumentException("Address-chain relationship with id " + id + " not found");
        }
        
        addressChainMapper.deleteById(id);
        log.info("Deleted address-chain relationship with id: {}", id);
    }

    private AddressChainDto convertToDto(AddressChain addressChain) {
        return AddressChainDto.builder()
                .id(addressChain.getId())
                .walletAddressId(addressChain.getWalletAddressId())
                .chainId(addressChain.getChainId())
                .createdAt(addressChain.getCreatedAt())
                .build();
    }

    private AddressChain convertToEntity(AddressChainDto dto) {
        return AddressChain.builder()
                .id(dto.getId())
                .walletAddressId(dto.getWalletAddressId())
                .chainId(dto.getChainId())
                .build();
    }
}