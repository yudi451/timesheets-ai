package com.timesheets.ai;

import com.timesheets.ai.mcp.TimesheetTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TimesheetsAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimesheetsAiApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider timesheetToolCallbackProvider(TimesheetTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
