package com.fmpse.energy_monitoring.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
@Data
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private float corrente;

    @Column(nullable = false)
    private float tensao;

    @Column(name = "energia_total", nullable = false)
    private float energiaTotal;
    @Column(nullable = false)
    private float custo;

    @Column(nullable = false)
    private LocalDateTime timestamp;

}
