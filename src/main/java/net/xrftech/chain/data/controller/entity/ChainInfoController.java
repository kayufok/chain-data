package net.xrftech.chain.data.controller.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.ChainInfoDto;
import net.xrftech.chain.data.service.entity.ChainInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/chains")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ChainInfoController {

    private final ChainInfoService chainInfoService;

    @GetMapping
    public ResponseEntity<List<ChainInfoDto>> getAllChains() {
        log.info("GET /api/v1/chains - Fetching all chains");
        List<ChainInfoDto> chains = chainInfoService.getAllChains();
        return ResponseEntity.ok(chains);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChainInfoDto> getChainById(@PathVariable Long id) {
        log.info("GET /api/v1/chains/{} - Fetching chain by id", id);
        Optional<ChainInfoDto> chain = chainInfoService.getChainById(id);
        return chain.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ChainInfoDto> createChain(@Valid @RequestBody ChainInfoDto chainInfoDto) {
        log.info("POST /api/v1/chains - Creating new chain");
        try {
            ChainInfoDto createdChain = chainInfoService.createChain(chainInfoDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdChain);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create chain: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChainInfoDto> updateChain(@PathVariable Long id, @Valid @RequestBody ChainInfoDto chainInfoDto) {
        log.info("PUT /api/v1/chains/{} - Updating chain", id);
        try {
            ChainInfoDto updatedChain = chainInfoService.updateChain(id, chainInfoDto);
            return ResponseEntity.ok(updatedChain);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update chain: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChain(@PathVariable Long id) {
        log.info("DELETE /api/v1/chains/{} - Deleting chain", id);
        try {
            chainInfoService.deleteChain(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete chain: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}