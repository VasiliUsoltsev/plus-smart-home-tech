package ru.yandex.practicum.kafka.telemetry.analyzer.config.grps;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grpc.client.hub-router")
@Getter
@Setter
public class AnalyzerGrpsProperties {
    private String address;
}
