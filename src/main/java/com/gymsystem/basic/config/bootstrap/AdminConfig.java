package com.gymsystem.basic.config.bootstrap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.admin")
public class AdminConfig {
    private String name = "Owner";
    private String email;
    private String password;
}
