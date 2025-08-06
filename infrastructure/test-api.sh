#!/bin/bash

# Test script for Ethereum Block Address API
echo "=== Ethereum Block Address API Test Suite ===" > test-results.txt
echo "Test started at: $(date)" >> test-results.txt
echo "" >> test-results.txt

# Test 1: Health check
echo "Test 1: Health Check" >> test-results.txt
curl -s http://localhost:8080/actuator/health >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 2: Valid block number (recent block)
echo "Test 2: Valid Block Number (18500000)" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks/18500000/addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 3: Valid block number in hex format
echo "Test 3: Valid Block Number in Hex (0x11a9760 = 18500000)" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks/0x11a9760/addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 4: Invalid block height (negative number)
echo "Test 4: Invalid Block Height (-1)" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks/-1/addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 5: Invalid block height (non-numeric)
echo "Test 5: Invalid Block Height (invalid)" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks/invalid/addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 6: Very high block number (likely doesn't exist)
echo "Test 6: Non-existent Block (99999999)" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks/99999999/addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

# Test 7: Empty block height
echo "Test 7: Empty Block Height" >> test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/blocks//addresses >> test-results.txt 2>&1
echo "" >> test-results.txt

echo "=== Test completed at: $(date) ===" >> test-results.txt
echo "Tests completed. Results saved to test-results.txt"