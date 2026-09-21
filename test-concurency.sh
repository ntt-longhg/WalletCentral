#!/bin/bash

echo "🚀 Bắt đầu gửi request mỗi 500ms... Nhấn [CTRL+C] để dừng."

while true; do
  curl --request POST \
    --url http://localhost:8080/api/v1/billing/webhook \
    --header 'Content-Type: application/json' \
    --header 'x-api-key: dev:123456789' \
    --data '{
    "serviceCode": "kfc",
    "usageUnits": 100,
    "referenceId": "SMS-20260914-001",
    "description": "SMS verification codes batch",
    "webhookUrl": "https://service.example.com/webhook/billing",
    "webhookAuth": "Bearer abc123"
  }'
  
  echo -e "\n--------------------------------------------"
  
  # Nghỉ 0.5 giây (tương đương 500ms)
  sleep 0.5
done
