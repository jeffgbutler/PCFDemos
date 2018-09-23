package com.jgb;

import javax.sql.DataSource;

import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud")
public class CloudConfiguration extends AbstractCloudConfig {

    @Bean
    public DataSource dataSource() {
        // pull database connection from cloud foundry
        return connectionFactory().dataSource();
    }
}
