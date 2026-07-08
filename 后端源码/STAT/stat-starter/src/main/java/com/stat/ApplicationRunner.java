package com.stat;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = { "com.stat" },
                      exclude = {
                          org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration.class,
                          org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration.class
                      })
@EnableTransactionManagement
@MapperScan({"com.stat.dal.mapper", "com.stat.admin.mapper"})
@EnableAsync
@EnableScheduling
public class ApplicationRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationRunner.class);

    public static void main(String[] args) {
        SpringApplication.run(ApplicationRunner.class, args);
    }

    public void run(String... args) throws Exception {
        logger.info("app start");
    }
}
