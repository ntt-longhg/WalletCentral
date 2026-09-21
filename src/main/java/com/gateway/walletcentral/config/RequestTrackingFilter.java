package com.gateway.walletcentral.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestTrackingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTrackingFilter.class);
    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 1. Lấy requestId từ Header (nếu hệ thống truyền qua) hoặc tự
        // sinh mới
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString(); // Lấy ngắn gọn 8 ký tự
        }

        // 2. Đưa requestId vào MDC để tất cả các log trong cùng request này đều tự động
        // kèm theo requestId
        MDC.put(REQUEST_ID, requestId);

        // Trả requestId về cho client qua Response Header để tiện debug
        response.setHeader("X-Request-Id", requestId);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        log.info(">> [INCOMING] {} {} | IP: {}", method, uri, clientIp);

        try {
            // 3. Cho phép request tiếp tục đi vào Controller
            filterChain.doFilter(request, response);
        } finally {
            // 4. Tính toán thời gian xử lý sau khi Controller chạy xong
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            log.info("<< [OUTGOING] {} {} | Status: {} | Taken: {} ms", method, uri, status,
                    duration);

            // 5. Xóa MDC sau khi hoàn thành request để tránh rò rỉ bộ nhớ (ThreadLocal)
            MDC.clear();
        }
    }

}
