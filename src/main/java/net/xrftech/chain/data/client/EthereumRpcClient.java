package net.xrftech.chain.data.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.dto.RpcRequest;
import net.xrftech.chain.data.dto.RpcResponse;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EthereumRpcClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String rpcEndpoint;

    public EthereumRpcClient(
            @Value("${ethereum.rpc.endpoint}") String rpcEndpoint,
            @Value("${ethereum.rpc.timeout:10}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        
        this.rpcEndpoint = rpcEndpoint;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
        
        log.info("Initialized Ethereum RPC client with endpoint: {} and timeout: {}s", 
                rpcEndpoint, timeoutSeconds);
    }

    public RpcResponse.EthBlock getBlockByNumber(String blockHeight, boolean includeTransactions) throws Exception {
        log.debug("Fetching block data for height: {}", blockHeight);
        
        // Convert decimal to hex if necessary
        String hexBlockHeight = blockHeight.startsWith("0x") ? blockHeight : "0x" + Long.toHexString(Long.parseLong(blockHeight));
        
        RpcRequest request = RpcRequest.builder()
                .method("eth_getBlockByNumber")
                .params(Arrays.asList(hexBlockHeight, includeTransactions))
                .build();

        String requestJson = objectMapper.writeValueAsString(request);
        log.debug("Sending RPC request: {}", requestJson);

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
        Request httpRequest = new Request.Builder()
                .url(rpcEndpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                log.error("HTTP request failed with status: {}", response.code());
                throw new RuntimeException("HTTP request failed: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("Received RPC response: {}", responseBody);

            RpcResponse rpcResponse = objectMapper.readValue(responseBody, RpcResponse.class);
            
            if (rpcResponse.getError() != null) {
                log.error("RPC error: {} - {}", rpcResponse.getError().getCode(), rpcResponse.getError().getMessage());
                throw new RuntimeException("RPC API Error: " + rpcResponse.getError().getMessage());
            }
            
            if (rpcResponse.getResult() == null) {
                log.warn("Block not found in RPC response");
                throw new RuntimeException("Block not found");
            }
            
            return rpcResponse.getResult();
            
        } catch (IOException e) {
            log.error("Failed to communicate with RPC endpoint", e);
            throw new RuntimeException("RPC communication failed", e);
        }
    }
}