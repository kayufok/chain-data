#!/bin/bash

# Batch Processing Health Check Script
# Usage: ./batch-health-check.sh

echo "🔍 BATCH PROCESSING HEALTH CHECK"
echo "================================"
echo "Timestamp: $(date)"
echo ""

# API Base URL
API_BASE="http://localhost:8080/api/v1/batch"

echo "📊 1. CURRENT BATCH STATUS"
echo "-------------------------"
curl -s "${API_BASE}/status" | jq '.' || echo "❌ Failed to get batch status"
echo ""

echo "🧠 2. MEMORY STATUS"
echo "------------------"
curl -s "${API_BASE}/memory-status" | jq '.' || echo "❌ Failed to get memory status"
echo ""

echo "📈 3. CACHE STATISTICS"
echo "---------------------"
curl -s "${API_BASE}/cache-stats" | jq '.' || echo "❌ Failed to get cache stats"
echo ""

echo "🐳 4. DOCKER CONTAINER STATUS"
echo "----------------------------"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo "💾 5. DATABASE CONNECTION TEST"
echo "-----------------------------"
docker exec postgres-db psql -U chainuser -d chaindata -c "SELECT 
    count(*) as total_addresses,
    (SELECT count(*) FROM address_chain) as total_relationships,
    pg_size_pretty(pg_total_relation_size('address')) as address_table_size,
    pg_size_pretty(pg_total_relation_size('address_chain')) as relationship_table_size;" 2>/dev/null || echo "❌ Database connection failed"
echo ""

echo "📋 6. INDEX HEALTH CHECK"
echo "-----------------------"
docker exec postgres-db psql -U chainuser -d chaindata -c "SELECT 
    schemaname, tablename, indexname, 
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes 
WHERE tablename IN ('address', 'address_chain')
ORDER BY pg_relation_size(indexrelid) DESC;" 2>/dev/null || echo "❌ Index check failed"
echo ""

echo "⚠️  7. RECENT ERROR LOGS (Last 20 lines)"
echo "----------------------------------------"
docker logs spring-boot-app --tail=20 --since=5m | grep -E "(ERROR|WARN|Failed)" || echo "✅ No recent errors found"
echo ""

echo "🚀 8. PERFORMANCE METRICS"
echo "------------------------"
BATCH_STATUS=$(curl -s "${API_BASE}/status")
if [ $? -eq 0 ]; then
    echo "Current Block: $(echo $BATCH_STATUS | jq -r '.data.currentBlockNumber // "N/A"')"
    echo "Blocks Processed: $(echo $BATCH_STATUS | jq -r '.data.blocksProcessed // "N/A"')"
    echo "Processing Rate: $(echo $BATCH_STATUS | jq -r '.data.processingRate // "N/A"')"
    echo "Cache Hit Rate: $(echo $BATCH_STATUS | jq -r '.data.cacheHitRatePercent // "N/A"')%"
    echo "Total Batches: $(echo $BATCH_STATUS | jq -r '.data.totalBatches // "N/A"')"
    echo "Failed Blocks: $(echo $BATCH_STATUS | jq -r '.data.failedBlocks // "N/A"')"
else
    echo "❌ Could not retrieve performance metrics"
fi
echo ""

echo "🔧 9. RECOMMENDED ACTIONS"
echo "------------------------"

# Check cache hit rate
CACHE_HIT_RATE=$(echo $BATCH_STATUS | jq -r '.data.cacheHitRatePercent // 0')
if [ "$CACHE_HIT_RATE" -lt 50 ]; then
    echo "⚠️  Cache hit rate is low ($CACHE_HIT_RATE%). Consider:"
    echo "   - Increasing cache size"
    echo "   - Running: curl -X POST ${API_BASE}/cache-cleanup"
fi

# Check if batch is stuck
BLOCKS_REMAINING=$(echo $BATCH_STATUS | jq -r '.data.blocksRemaining // 0')
if [ "$BLOCKS_REMAINING" -eq 0 ]; then
    echo "✅ Batch processing appears healthy"
else
    echo "📊 Batch in progress: $BLOCKS_REMAINING blocks remaining"
fi

echo ""
echo "🛠️  MANUAL COMMANDS FOR TROUBLESHOOTING"
echo "======================================="
echo "Stop batch:      curl -X POST ${API_BASE}/stop"
echo "Start batch:     curl -X POST ${API_BASE}/start"
echo "Cache cleanup:   curl -X POST ${API_BASE}/cache-cleanup"
echo "View full logs:  docker logs spring-boot-app --tail=100"
echo "Database shell:  docker exec -it postgres-db psql -U chainuser -d chaindata"
echo ""
echo "Health check completed at $(date)"
