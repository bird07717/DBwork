package com.busagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.time.LocalTime;

@TableName("bus_route")
public class BusRoute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String routeCode;
    private String routeName;
    private String direction;
    private String startStationName;
    private String endStationName;
    private LocalTime operationStartTime;
    private LocalTime operationEndTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getStartStationName() { return startStationName; }
    public void setStartStationName(String startStationName) { this.startStationName = startStationName; }
    public String getEndStationName() { return endStationName; }
    public void setEndStationName(String endStationName) { this.endStationName = endStationName; }
    public LocalTime getOperationStartTime() { return operationStartTime; }
    public void setOperationStartTime(LocalTime operationStartTime) { this.operationStartTime = operationStartTime; }
    public LocalTime getOperationEndTime() { return operationEndTime; }
    public void setOperationEndTime(LocalTime operationEndTime) { this.operationEndTime = operationEndTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
