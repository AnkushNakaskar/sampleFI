package com.fis.excel.fisexcelproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "project")
@Validated
@Data
public class ProjectConfig {

    private String group;

    private String team;

    private String env;

    private String application;

    private String destinationPath;


}
