-- Top boarding stations by passenger count.
-- Parameters can be replaced by backend placeholders:
--   @route_code, @start_date, @end_date, @limit_size
SET @route_code = CONVERT('1' USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @start_date = '2026-05-06';
SET @end_date = '2026-06-04';
SELECT
  br.id AS route_id,
  br.route_code,
  br.route_name,
  rs.station_order,
  s.id AS station_id,
  s.station_name,
  s.longitude,
  s.latitude,
  COUNT(rr.id) AS boarding_count
FROM bus_route br
JOIN route_station rs ON rs.route_id = br.id
JOIN station s ON s.id = rs.station_id
LEFT JOIN ride_record rr
  ON rr.route_id = br.id
  AND rr.boarding_station_id = s.id
  AND rr.ride_time >= @start_date
  AND rr.ride_time < DATE_ADD(@end_date, INTERVAL 1 DAY)
WHERE br.route_code = @route_code
GROUP BY
  br.id,
  br.route_code,
  br.route_name,
  rs.station_order,
  s.id,
  s.station_name,
  s.longitude,
  s.latitude
ORDER BY boarding_count DESC, rs.station_order ASC
LIMIT 10;
