package com.busagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("route_station")
public class RouteStation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long stationId;
    private Integer stationOrder;
    private BigDecimal distanceFromStart;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public Integer getStationOrder() { return stationOrder; }
    public void setStationOrder(Integer stationOrder) { this.stationOrder = stationOrder; }
    public BigDecimal getDistanceFromStart() { return distanceFromStart; }
    public void setDistanceFromStart(BigDecimal distanceFromStart) { this.distanceFromStart = distanceFromStart; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
