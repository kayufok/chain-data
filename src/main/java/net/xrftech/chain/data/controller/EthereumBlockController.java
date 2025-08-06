package net.xrftech.chain.data.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.BlockAddressResponse;
import net.xrftech.chain.data.service.EthereumBlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class EthereumBlockController {

    private final EthereumBlockService blockService;

    @GetMapping("/blocks/{blockHeight}/addresses")
    public ResponseEntity<BlockAddressResponse> getBlockAddresses(
            @PathVariable String blockHeight,
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        log.info("Received request for block {} from IP: {}", blockHeight, clientIp);
        
        try {
            BlockAddressResponse response = blockService.getBlockAddresses(blockHeight);
            log.info("Successfully completed request for block {}", blockHeight);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid block height error for block {}: {}", blockHeight, e.getMessage());
            BlockAddressResponse errorResponse = createErrorResponse("INVALID_BLOCK_HEIGHT", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (RuntimeException e) {
            log.error("Error processing block {}: {}", blockHeight, e.getMessage());
            
            if (e.getMessage().contains("Block not found")) {
                BlockAddressResponse errorResponse = createErrorResponse("BLOCK_NOT_FOUND", 
                        "Block with height " + blockHeight + " not found");
                return ResponseEntity.notFound().build();
            }
            
            if (e.getMessage().contains("timed out")) {
                BlockAddressResponse errorResponse = createErrorResponse("RPC_TIMEOUT", "RPC request timed out");
                return ResponseEntity.status(503).body(errorResponse);
            }
            
            if (e.getMessage().contains("RPC provider")) {
                BlockAddressResponse errorResponse = createErrorResponse("RPC_API_ERROR", 
                        "Failed to retrieve block data from RPC provider");
                return ResponseEntity.status(502).body(errorResponse);
            }
            
            BlockAddressResponse errorResponse = createErrorResponse("INTERNAL_SERVER_ERROR", 
                    "An unexpected error occurred");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private BlockAddressResponse createErrorResponse(String errorCode, String errorMessage) {
        return BlockAddressResponse.builder()
                .status("error")
                .error(BlockAddressResponse.ErrorDetails.builder()
                        .code(errorCode)
                        .message(errorMessage)
                        .build())
                .build();
    }
}