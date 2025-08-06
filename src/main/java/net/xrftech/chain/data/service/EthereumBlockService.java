package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.client.EthereumRpcClient;
import net.xrftech.chain.data.dto.BlockAddressResponse;
import net.xrftech.chain.data.dto.RpcResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EthereumBlockService {

    private final EthereumRpcClient rpcClient;

    public BlockAddressResponse getBlockAddresses(String blockHeight) {
        log.info("Processing request for block height: {}", blockHeight);
        
        try {
            // Validate block height
            validateBlockHeight(blockHeight);
            
            // Fetch block data from RPC
            RpcResponse.EthBlock block = rpcClient.getBlockByNumber(blockHeight, true);
            
            // Extract addresses
            BlockAddressResponse response = extractAddressesFromBlock(block, blockHeight);
            
            log.info("Successfully processed block {}: {} unique addresses", 
                    blockHeight, response.getData().getUniqueAddressCount());
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to process block {}: {}", blockHeight, e.getMessage());
            throw mapErrorToApiException(e);
        }
    }

    private void validateBlockHeight(String blockHeight) {
        if (blockHeight == null || blockHeight.trim().isEmpty()) {
            throw new IllegalArgumentException("Block height cannot be null or empty");
        }

        try {
            // Try parsing as decimal
            if (!blockHeight.startsWith("0x")) {
                long blockNumber = Long.parseLong(blockHeight);
                if (blockNumber < 0) {
                    throw new IllegalArgumentException("Block height must be a positive number");
                }
            } else {
                // Try parsing as hexadecimal
                Long.parseLong(blockHeight.substring(2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Block height must be a valid decimal or hexadecimal number");
        }
    }

    private BlockAddressResponse extractAddressesFromBlock(RpcResponse.EthBlock block, String requestedBlockHeight) {
        if (block == null) {
            throw new RuntimeException("Block data is null");
        }

        List<RpcResponse.EthTransaction> transactions = block.getTransactions();
        if (transactions == null) {
            transactions = List.of();
        }

        // Extract unique addresses from 'from' and 'to' fields
        Set<String> uniqueAddresses = new LinkedHashSet<>();
        
        for (RpcResponse.EthTransaction transaction : transactions) {
            if (transaction.getFrom() != null && !transaction.getFrom().trim().isEmpty()) {
                uniqueAddresses.add(transaction.getFrom());
            }
            if (transaction.getTo() != null && !transaction.getTo().trim().isEmpty()) {
                uniqueAddresses.add(transaction.getTo());
            }
        }

        // Convert timestamp from hex to Instant
        Instant blockTimestamp = parseBlockTimestamp(block.getTimestamp());

        // Convert block number from hex to decimal string
        String blockHeightDecimal = parseBlockNumber(block.getNumber(), requestedBlockHeight);

        BlockAddressResponse.BlockAddressData data = BlockAddressResponse.BlockAddressData.builder()
                .blockHeight(blockHeightDecimal)
                .blockHash(block.getHash())
                .addresses(uniqueAddresses.stream().collect(Collectors.toList()))
                .transactionCount(transactions.size())
                .uniqueAddressCount(uniqueAddresses.size())
                .timestamp(blockTimestamp)
                .build();

        return BlockAddressResponse.builder()
                .status("success")
                .data(data)
                .build();
    }

    private Instant parseBlockTimestamp(String timestamp) {
        if (timestamp == null) {
            return Instant.now();
        }
        
        try {
            long timestampSeconds = Long.parseLong(timestamp.startsWith("0x") 
                    ? timestamp.substring(2) : timestamp, 16);
            return Instant.ofEpochSecond(timestampSeconds);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse block timestamp: {}", timestamp);
            return Instant.now();
        }
    }

    private String parseBlockNumber(String blockNumber, String fallback) {
        if (blockNumber != null && blockNumber.startsWith("0x")) {
            try {
                long blockNum = Long.parseLong(blockNumber.substring(2), 16);
                return String.valueOf(blockNum);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse block number: {}", blockNumber);
            }
        }
        return fallback;
    }

    private RuntimeException mapErrorToApiException(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            return new IllegalArgumentException(error.getMessage());
        }
        
        if (error.getMessage() != null && error.getMessage().contains("Block not found")) {
            return new RuntimeException("Block not found");
        }
        
        if (error.getMessage() != null && error.getMessage().contains("RPC API Error")) {
            return new RuntimeException("Failed to retrieve block data from RPC provider");
        }
        
        if (error.getMessage() != null && error.getMessage().contains("timeout")) {
            return new RuntimeException("RPC request timed out");
        }
        
        log.error("Unexpected error occurred", error);
        return new RuntimeException("Internal server error");
    }
}