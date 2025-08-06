#!/bin/bash

# Test script for Entity CRUD API endpoints
echo "=== Entity CRUD API Test Suite ===" > entity-test-results.txt
echo "Test started at: $(date)" >> entity-test-results.txt
echo "" >> entity-test-results.txt

BASE_URL="http://localhost:8080/api/v1"

# Test 1: Get all addresses (should be empty initially)
echo "Test 1: GET All Addresses" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" "$BASE_URL/addresses" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 2: Create a new address
echo "Test 2: POST Create Address" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" \
  -H "Content-Type: application/json" \
  -d '{"walletAddress":"0x1234567890abcdef1234567890abcdef12345678"}' \
  "$BASE_URL/addresses" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 3: Get all addresses (should show created address)
echo "Test 3: GET All Addresses After Creation" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" "$BASE_URL/addresses" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 4: Get address by ID
echo "Test 4: GET Address by ID (1)" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" "$BASE_URL/addresses/1" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 5: Update address
echo "Test 5: PUT Update Address" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{"walletAddress":"0xabcdefabcdefabcdefabcdefabcdefabcdefabcdef"}' \
  "$BASE_URL/addresses/1" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 6: Get all chains
echo "Test 6: GET All Chains" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" "$BASE_URL/chains" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 7: Create a new chain
echo "Test 7: POST Create Chain" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" \
  -H "Content-Type: application/json" \
  -d '{"chainName":"Arbitrum","chainId":"42161","nextBlockNumber":150000000}' \
  "$BASE_URL/chains" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 8: Invalid address creation (should fail validation)
echo "Test 8: POST Invalid Address (Bad Format)" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" \
  -H "Content-Type: application/json" \
  -d '{"walletAddress":"invalid-address"}' \
  "$BASE_URL/addresses" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 9: Get non-existent address
echo "Test 9: GET Non-existent Address (ID 999)" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" "$BASE_URL/addresses/999" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

# Test 10: Delete address
echo "Test 10: DELETE Address" >> entity-test-results.txt
curl -s -w "HTTP Status: %{http_code}\n" \
  -X DELETE \
  "$BASE_URL/addresses/1" >> entity-test-results.txt 2>&1
echo "" >> entity-test-results.txt

echo "=== Test completed at: $(date) ===" >> entity-test-results.txt
echo "Entity API tests completed. Results saved to entity-test-results.txt"