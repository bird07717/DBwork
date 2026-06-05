-- Schedule load rate statistics.
-- Parameters can be replaced by backend placeholders:
--   @route_code, @start_date, @end_date
SET @route_code = CONVERT('1' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @start_date = '2026-05-06';
SET @end_date = '2026-06-04';

SELECT
  bs.id AS schedule_id,
  br.id AS route_id,
  br.route_code,
  br.route_name,
  bs.schedule_date,
  TIME(bs.depart_time) AS depart_time,
  bs.period_type,
  bv.vehicle_code,
  bv.plate_no,
  bv.capacity,
  COUNT(rr.id) AS passenger_count,
  ROUND(COUNT(rr.id) / bv.capacity * 100, 2) AS load_rate
FROM bus_schedule bs
JOIN bus_route br ON br.id = bs.route_id
JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
WHERE br.route_code = @route_code
  AND bs.schedule_date BETWEEN @start_date AND @end_date
GROUP BY
  bs.id,
  br.id,
  br.route_code,
  br.route_name,
  bs.schedule_date,
  bs.depart_time,
  bs.period_type,
  bv.vehicle_code,
  bv.plate_no,
  bv.capacity
ORDER BY bs.schedule_date, bs.depart_time;
