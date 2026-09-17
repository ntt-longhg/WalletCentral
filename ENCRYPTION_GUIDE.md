# WalletCentral - Encryption Configuration Guide

## Overview

Hệ thống mã hóa cấu hình cho WalletCentral, cho phép mã hóa các thông số nhạy cảm (password DB, RabbitMQ, Mail) trực tiếp trong file `application.yml` mà không cần sử dụng library.

**Thuật toán:** AES-256 GCM (Authenticated Encryption)

---

## Table of Contents

1. [Cấu trúc Files](#1-cấu-trúc-files)
2. [Cách Sử Dụng Dev](#2-cách-sử-dụng-dev)
3. [Mã Hóa Giá Trị](#3-mã-hóa-giá-trị)
4. [Cấu Hình Production](#4-cấu-hình-production)
5. [Truyền Master Password](#5-truyền-master-password)
6. [Workflow Deploy Production](#6-workflow-deploy-production)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Cấu Trúc Files

```
src/main/java/com/gateway/walletcentral/config/encryption/
├── PropertyEncryptor.java              # AES-256 GCM encrypt/decrypt utility
├── EncryptedPropertyEnvironmentPostProcessor.java  # Auto-decrypt ENC() at startup
└── EncryptCLI.java                     # CLI tool để mã hóa giá trị

src/main/resources/
├── application.yml                     # Dev config (plaintext, KHÔNG commit password thật)
├── application-prod.yml                # Production template (dùng ENC())
├── META-INF/spring.factories           # Register EPP (Spring Boot 2.x)
└── META-INF/spring/
    └── org.springframework.boot.env.EnvironmentPostProcessor.imports  # (3.x/4.x)
```

---

## 2. Cách Sử Dụng Dev

Không cần thay đổi gì. Chạy bình thường:

```bash
./mvnw spring-boot:run
```

Dev đọc từ `application.yml` trong resources với plaintext passwords. Master password mặc định dùng `dev-only-master-key`.

---

## 3. Mã Hóa Giá Trị

### Bước 1: Chạy CLI tool

```bash
# Compile trước (nếu chưa có jar)
./mvnw clean package -DskipTests

# Chạy CLI
java -cp target/walletcentral-0.0.1-SNAPSHOT.jar \
  com.gateway.walletcentral.config.encryption.EncryptCLI encrypt
```

### Bước 2: Nhập thông tin

```
Enter master password: my-production-master-key
Enter value to encrypt: akuo-kfo

=== Encrypted value (copy this into ENC()): ===
ENC(xR3kL9mN2pQ7vB4w...base64...salt:iv:ciphertext)
```

### Bước 3: Copy kết quả vào yml

```yaml
spring:
  datasource:
    password: ENC(xR3kL9mN2pQ7vB4w...base64...)
```

### Decrypt (kiểm tra)

```bash
java -cp target/walletcentral-0.0.1-SNAPSHOT.jar \
  com.gateway.walletcentral.config.encryption.EncryptCLI decrypt 'salt:iv:ciphertext'
```

---

## 4. Cấu Hình Production

### application-prod.yml

Đặt file này **bên ngoài jar** (không cần build lại khi thay đổi):

```bash
/opt/walletcentral/
├── walletcentral.jar
├── application-prod.yml    ← FILE NÀY
└── logs/
```

Nội dung `application-prod.yml` tham khảo:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://prod-db-host:3306/walletcentral
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:ENC(encrypted_value_here)}
    driver-class-name: org.mariadb.jdbc.Driver

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:ENC(encrypted_value_here)}

app:
  mail:
    host: smtp.gmail.com
    port: 587
    password: ${MAIL_PASSWORD:ENC(encrypted_value_here)}

encryption:
  master-password: ${ENCRYPTION_MASTER_PASSWORD}
```

### Hỗ trợ cả ENV Variable và ENC()

```yaml
# Cách 1: Dùng ENC() - mã hóa trước, giải mã tự động
password: ENC(salt:iv:ciphertext)

# Cách 2: Dùng env var - truyền trực tiếp khi chạy
password: ${DB_PASSWORD}

# Cách 3: Kết hợp - env var có fallback ENC()
password: ${DB_PASSWORD:ENC(fallback_encrypted_value)}
```

---

## 5. Truyền Master Password

Master password được tìm theo thứ tự ưu tiên:

| Ưu tiên | Nguồn | Ví dụ |
| --------- | ------- | ------- |
| 1 | JVM arg `-D` | `-Dencryption.master-password=xxx` |
| 2 | Env var | `ENCRYPTION_MASTER_PASSWORD=xxx` |
| 3 | Property trong yml | `encryption.master-password: xxx` |

### Khuyến nghị: dùng Env var (an toàn nhất)

```bash
# Production
export ENCRYPTION_MASTER_PASSWORD="my-super-secret-key-2024"
java -jar walletcentral.jar --spring.profiles.active=prod

# Hoặc inline
ENCRYPTION_MASTER_PASSWORD="my-super-secret-key" java -jar walletcentral.jar --spring.profiles.active=prod
```

### Docker / Docker Compose

```yaml
# docker-compose.yml
services:
  walletcentral:
    image: walletcentral:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - ENCRYPTION_MASTER_PASSWORD=${ENCRYPTION_MASTER_PASSWORD}
      - DB_USERNAME=wallet_user
      - DB_PASSWORD=${DB_PASSWORD}
      - RABBITMQ_HOST=rabbitmq
```

### Systemd Service

```ini
# /etc/systemd/system/walletcentral.service
[Service]
Environment="ENCRYPTION_MASTER_PASSWORD=my-secret-key"
Environment="SPRING_PROFILES_ACTIVE=prod"
ExecStart=/usr/bin/java -jar /opt/walletcentral/walletcentral.jar
```

---

## 6. Workflow Deploy Production

### Lần đầu deploy

```bash
# 1. Chọn master password (LƯU Ý: mất password = mất data!)
MASTER_KEY="my-production-master-key-$(date +%Y%m%d)"

# 2. Mã hóa các giá trị nhạy cảm
java -cp walletcentral.jar com.gateway.walletcentral.config.encryption.EncryptCLI encrypt
# → Nhập MASTER_KEY
# → Nhập từng password
# → Copy kết quả ENC(...)

# 3. Tạo application-prod.yml bên ngoài jar
cat > /opt/walletcentral/application-prod.yml << 'EOF'
spring:
  datasource:
    url: jdbc:mariadb://prod-host:3306/walletcentral
    username: root
    password: ENC(salt:iv:ciphertext...)
  rabbitmq:
    host: localhost
    password: ENC(salt:iv:ciphertext...)
encryption:
  master-password: ${ENCRYPTION_MASTER_PASSWORD}
EOF

# 4. Set env var và chạy
export ENCRYPTION_MASTER_PASSWORD="$MASTER_KEY"
java -jar walletcentral.jar --spring.profiles.active=prod
```

### Thay đổi config không cần build lại

```bash
# Chỉ cần edit file yml bên ngoài, restart service
sudo systemctl edit walletcentral
# Edit /opt/walletcentral/application-prod.yml
sudo systemctl restart walletcentral
```

---

## 7. Troubleshooting

### Lỗi: "Failed to decrypt property"

**Nguyên nhân:** Sai master password hoặc encrypted value bị hỏng.

```bash
# Kiểm tra lại bằng cách decrypt thủ công
java -cp walletcentral.jar com.gateway.walletcentral.config.encryption.EncryptCLI decrypt 'your-encrypted-value'
# Nếu đúng password → giải mã thành công
# Nếu sai password → sẽ báo lỗi
```

### Lỗi: "Invalid encrypted format"

**Nguyên nhân:** Encrypted value không đúng format `salt:iv:ciphertext`.

```yaml
# Sai
password: ENC(plaintext)

# Đúng
password: ENC(xR3kL9mN2pQ7...base64...characters)
```

### Dev không cần password

Dev không cần set master password. Hệ thống dùng default `dev-only-master-key` và đọc plaintext từ `application.yml` trong resources.

### Kiểm tra EnvironmentPostProcessor có hoạt động không

```bash
# Add debug log
java -Dlogging.level.com.gateway.walletcentral.config.encryption=DEBUG \
  -jar walletcentral.jar --spring.profiles.active=prod
```

---

## Security Notes

- **KHÔNG** commit file chứa master password lên git
- **KHÔNG** hardcode master password trong source code
- Master password chỉ truyền qua env var hoặc JVM args
- Thuật toán AES-256 GCM đảm bảo tính Confidentiality + Integrity
- Mỗi lần encrypt tạo salt + IV khác nhau (deterministic encryption không an toàn)
- Encrypted value format: `Base64(salt):Base64(iv):Base64(ciphertext)`
