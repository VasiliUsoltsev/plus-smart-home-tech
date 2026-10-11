package ru.yandex.practicum.kafka.telemetry.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ScenarioConditionPk implements Serializable {
    @Column(name = "sensor_id")
    private String sensorId;

    @Column(name = "scenario_id")
    private Long scenarioId;

    @Column(name = "condition_id")
    private Long conditionId;

}
