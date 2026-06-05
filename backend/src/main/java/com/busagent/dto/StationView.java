package com.busagent.dto;

import java.math.BigDecimal;

public record StationView(
        Long id,
        String stationCode,
        String stationName,
        Integer stationOrder,
        BigDecimal longitude,
        BigDecimal latitude
) {
}
