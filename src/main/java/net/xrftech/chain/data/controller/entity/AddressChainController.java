package net.xrftech.chain.data.controller.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.AddressChainDto;
import net.xrftech.chain.data.service.entity.AddressChainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/address-chains")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AddressChainController {

    private final AddressChainService addressChainService;

    @GetMapping
    public ResponseEntity<List<AddressChainDto>> getAllAddressChains() {
        log.info("GET /api/v1/address-chains - Fetching all address-chain relationships");
        List<AddressChainDto> addressChains = addressChainService.getAllAddressChains();
        return ResponseEntity.ok(addressChains);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressChainDto> getAddressChainById(@PathVariable Long id) {
        log.info("GET /api/v1/address-chains/{} - Fetching address-chain relationship by id", id);
        Optional<AddressChainDto> addressChain = addressChainService.getAddressChainById(id);
        return addressChain.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AddressChainDto> createAddressChain(@Valid @RequestBody AddressChainDto addressChainDto) {
        log.info("POST /api/v1/address-chains - Creating new address-chain relationship");
        try {
            AddressChainDto createdAddressChain = addressChainService.createAddressChain(addressChainDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAddressChain);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create address-chain relationship: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddressChain(@PathVariable Long id) {
        log.info("DELETE /api/v1/address-chains/{} - Deleting address-chain relationship", id);
        try {
            addressChainService.deleteAddressChain(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete address-chain relationship: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}