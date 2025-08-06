package net.xrftech.chain.data.controller.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.entity.ApiCallFailureLogDto;
import net.xrftech.chain.data.service.entity.ApiCallFailureLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/failure-logs")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ApiCallFailureLogController {

    private final ApiCallFailureLogService failureLogService;

    @GetMapping
    public ResponseEntity<List<ApiCallFailureLogDto>> getAllFailureLogs() {
        log.info("GET /api/v1/failure-logs - Fetching all failure logs");
        List<ApiCallFailureLogDto> failureLogs = failureLogService.getAllFailureLogs();
        return ResponseEntity.ok(failureLogs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiCallFailureLogDto> getFailureLogById(@PathVariable Long id) {
        log.info("GET /api/v1/failure-logs/{} - Fetching failure log by id", id);
        Optional<ApiCallFailureLogDto> failureLog = failureLogService.getFailureLogById(id);
        return failureLog.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/chain/{chainId}")
    public ResponseEntity<List<ApiCallFailureLogDto>> getFailureLogsByChainId(@PathVariable String chainId) {
        log.info("GET /api/v1/failure-logs/chain/{} - Fetching failure logs by chain ID", chainId);
        List<ApiCallFailureLogDto> failureLogs = failureLogService.getFailureLogsByChainId(chainId);
        return ResponseEntity.ok(failureLogs);
    }

    @GetMapping("/status/{statusCode}")
    public ResponseEntity<List<ApiCallFailureLogDto>> getFailureLogsByStatusCode(@PathVariable String statusCode) {
        log.info("GET /api/v1/failure-logs/status/{} - Fetching failure logs by status code", statusCode);
        List<ApiCallFailureLogDto> failureLogs = failureLogService.getFailureLogsByStatusCode(statusCode);
        return ResponseEntity.ok(failureLogs);
    }

    @PostMapping
    public ResponseEntity<ApiCallFailureLogDto> createFailureLog(@Valid @RequestBody ApiCallFailureLogDto failureLogDto) {
        log.info("POST /api/v1/failure-logs - Creating new failure log");
        try {
            ApiCallFailureLogDto createdFailureLog = failureLogService.createFailureLog(failureLogDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFailureLog);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create failure log: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFailureLog(@PathVariable Long id) {
        log.info("DELETE /api/v1/failure-logs/{} - Deleting failure log", id);
        try {
            failureLogService.deleteFailureLog(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete failure log: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/chain/{chainId}")
    public ResponseEntity<Void> deleteFailureLogsByChainId(@PathVariable String chainId) {
        log.info("DELETE /api/v1/failure-logs/chain/{} - Deleting failure logs by chain ID", chainId);
        failureLogService.deleteFailureLogsByChainId(chainId);
        return ResponseEntity.noContent().build();
    }
}