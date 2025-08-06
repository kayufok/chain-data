package net.xrftech.chain.data.service.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.AddressDto;
import net.xrftech.chain.data.entity.Address;
import net.xrftech.chain.data.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressMapper addressMapper;

    public List<AddressDto> getAllAddresses() {
        log.info("Fetching all addresses");
        List<Address> addresses = addressMapper.selectList(null);
        return addresses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Page<AddressDto> getAddressesPaged(int page, int size) {
        log.info("Fetching addresses page {} with size {}", page, size);
        Page<Address> addressPage = addressMapper.selectPage(new Page<>(page, size), null);
        Page<AddressDto> dtoPage = new Page<>(page, size, addressPage.getTotal());
        dtoPage.setRecords(addressPage.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    public Optional<AddressDto> getAddressById(Long id) {
        log.info("Fetching address by id: {}", id);
        Address address = addressMapper.selectById(id);
        return address != null ? Optional.of(convertToDto(address)) : Optional.empty();
    }

    public Optional<AddressDto> getAddressByWalletAddress(String walletAddress) {
        log.info("Fetching address by wallet address: {}", walletAddress);
        QueryWrapper<Address> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wallet_address", walletAddress);
        Address address = addressMapper.selectOne(queryWrapper);
        return address != null ? Optional.of(convertToDto(address)) : Optional.empty();
    }

    @Transactional
    public AddressDto createAddress(AddressDto addressDto) {
        log.info("Creating new address: {}", addressDto.getWalletAddress());
        
        // Check if address already exists
        QueryWrapper<Address> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wallet_address", addressDto.getWalletAddress());
        Address existing = addressMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new IllegalArgumentException("Address with wallet address " + addressDto.getWalletAddress() + " already exists");
        }
        
        Address address = convertToEntity(addressDto);
        addressMapper.insert(address);
        log.info("Created address with id: {}", address.getId());
        return convertToDto(address);
    }

    @Transactional
    public AddressDto updateAddress(Long id, AddressDto addressDto) {
        log.info("Updating address with id: {}", id);
        
        Address existingAddress = addressMapper.selectById(id);
        if (existingAddress == null) {
            throw new IllegalArgumentException("Address with id " + id + " not found");
        }
        
        // Check for wallet address conflicts
        if (!existingAddress.getWalletAddress().equals(addressDto.getWalletAddress())) {
            QueryWrapper<Address> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("wallet_address", addressDto.getWalletAddress());
            Address conflicting = addressMapper.selectOne(queryWrapper);
            if (conflicting != null && !conflicting.getId().equals(id)) {
                throw new IllegalArgumentException("Address with wallet address " + addressDto.getWalletAddress() + " already exists");
            }
        }
        
        existingAddress.setWalletAddress(addressDto.getWalletAddress());
        addressMapper.updateById(existingAddress);
        log.info("Updated address with id: {}", id);
        return convertToDto(existingAddress);
    }

    @Transactional
    public void deleteAddress(Long id) {
        log.info("Deleting address with id: {}", id);
        
        Address existingAddress = addressMapper.selectById(id);
        if (existingAddress == null) {
            throw new IllegalArgumentException("Address with id " + id + " not found");
        }
        
        addressMapper.deleteById(id);
        log.info("Deleted address with id: {}", id);
    }

    private AddressDto convertToDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .walletAddress(address.getWalletAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    private Address convertToEntity(AddressDto dto) {
        return Address.builder()
                .id(dto.getId())
                .walletAddress(dto.getWalletAddress())
                .build();
    }
}