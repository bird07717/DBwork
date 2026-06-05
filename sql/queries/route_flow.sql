-- Route daily passenger flow trend.
-- Parameters can be replaced by backend placeholders:
--   @route_code, @start_date, @end_date
SET @route_code = CONVERT('1' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @start_date = '2026-05-06';
SET @end_date = '2026-06-04';

SELECT
  br.id AS route_id,
  br.route_code,
  br.route_name,
  DATE(rr.ride_time) AS stat_date,
  COUNT(rr.id) AS passenger_count
FROM bus_route br
LEFT JOIN ride_record rr
  ON rr.route_id = br.id
  AND rr.ride_time >= @start_date
  AND rr.ride_time < DATE_ADD(@end_date, INTERVAL 1 DAY)
WHERE br.route_code = @route_code
GROUP BY br.id, br.route_code, br.route_name, DATE(rr.ride_time)
ORDER BY stat_date;
