package net.xrftech.chain.data.controller.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.StatusDto;
import net.xrftech.chain.data.service.entity.StatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/statuses")
@RequiredArgsConstructor
@Validated
@Slf4j
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public ResponseEntity<List<StatusDto>> getAllStatuses() {
        log.info("GET /api/v1/statuses - Fetching all statuses");
        List<StatusDto> statuses = statusService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusDto> getStatusById(@PathVariable Long id) {
        log.info("GET /api/v1/statuses/{} - Fetching status by id", id);
        Optional<StatusDto> status = statusService.getStatusById(id);
        return status.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{statusCode}")
    public ResponseEntity<StatusDto> getStatusByCode(@PathVariable String statusCode) {
        log.info("GET /api/v1/statuses/code/{} - Fetching status by code", statusCode);
        Optional<StatusDto> status = statusService.getStatusByCode(statusCode);
        return status.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StatusDto> createStatus(@Valid @RequestBody StatusDto statusDto) {
        log.info("POST /api/v1/statuses - Creating new status");
        try {
            StatusDto createdStatus = statusService.createStatus(statusDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatusDto> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDto statusDto) {
        log.info("PUT /api/v1/statuses/{} - Updating status", id);
        try {
            StatusDto updatedStatus = statusService.updateStatus(id, statusDto);
            return ResponseEntity.ok(updatedStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id) {
        log.info("DELETE /api/v1/statuses/{} - Deleting status", id);
        try {
            statusService.deleteStatus(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}