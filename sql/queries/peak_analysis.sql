-- Passenger comparison across morning peak, evening peak, and normal periods.
-- Parameters can be replaced by backend placeholders:
--   @route_code, @start_date, @end_date
SET @route_code = CONVERT('1' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @start_date = '2026-05-06';
SET @end_date = '2026-06-04';

SELECT
  br.id AS route_id,
  br.route_code,
  br.route_name,
  schedule_load.period_type,
  CASE schedule_load.period_type
    WHEN 'morning_peak' THEN '早高峰'
    WHEN 'evening_peak' THEN '晚高峰'
    WHEN 'normal' THEN '正常时段'
    ELSE '其他'
  END AS period_name,
  SUM(schedule_load.passenger_count) AS passenger_count,
  COUNT(schedule_load.schedule_id) AS schedule_count,
  ROUND(SUM(schedule_load.passenger_count) / COUNT(schedule_load.schedule_id), 2) AS avg_passenger_per_schedule,
  ROUND(AVG(schedule_load.passenger_count / schedule_load.capacity * 100), 2) AS avg_load_rate
FROM bus_route br
JOIN (
  SELECT
    bs.id AS schedule_id,
    bs.route_id,
    bs.period_type,
    bv.capacity,
    COUNT(rr.id) AS passenger_count
  FROM bus_schedule bs
  JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
  LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
  WHERE bs.schedule_date BETWEEN @start_date AND @end_date
  GROUP BY bs.id, bs.route_id, bs.period_type, bv.capacity
) schedule_load ON schedule_load.route_id = br.id
WHERE br.route_code = @route_code
GROUP BY br.id, br.route_code, br.route_name, schedule_load.period_type
ORDER BY FIELD(schedule_load.period_type, 'morning_peak', 'evening_peak', 'normal');
