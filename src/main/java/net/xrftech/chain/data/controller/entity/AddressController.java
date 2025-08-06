package net.xrftech.chain.data.controller.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.AddressDto;
import net.xrftech.chain.data.service.entity.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> getAllAddresses() {
        log.info("GET /api/v1/addresses - Fetching all addresses");
        List<AddressDto> addresses = addressService.getAllAddresses();
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable Long id) {
        log.info("GET /api/v1/addresses/{} - Fetching address by id", id);
        Optional<AddressDto> address = addressService.getAddressById(id);
        return address.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto addressDto) {
        log.info("POST /api/v1/addresses - Creating new address");
        try {
            AddressDto createdAddress = addressService.createAddress(addressDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create address: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto addressDto) {
        log.info("PUT /api/v1/addresses/{} - Updating address", id);
        try {
            AddressDto updatedAddress = addressService.updateAddress(id, addressDto);
            return ResponseEntity.ok(updatedAddress);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update address: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        log.info("DELETE /api/v1/addresses/{} - Deleting address", id);
        try {
            addressService.deleteAddress(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete address: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}