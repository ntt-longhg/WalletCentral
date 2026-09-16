package com.gateway.walletcentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class WalletCentralGatewayApplication {

    public static void main(String[] args) {
//        SpringApplication.run(WalletCentralGatewayApplication.class, args);
        SpringApplication app = new SpringApplication(WalletCentralGatewayApplication.class);
        app.setApplicationStartup(new BufferingApplicationStartup(2048));
        app.run(args);
    }

}
