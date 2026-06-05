import argparse
import random
import subprocess
from datetime import date, datetime, time, timedelta


ROUTE_STATION_COUNTS = {
    "1": 27,
    "2": 17,
    "3": 32,
}

ROUTE_WEIGHTS = {
    "1": 0.38,
    "2": 0.26,
    "3": 0.36,
}

PERIOD_WINDOWS = {
    "morning_peak": (time(7, 0), time(9, 0)),
    "normal": (time(9, 30), time(16, 30)),
    "evening_peak": (time(17, 0), time(19, 0)),
}

PERIOD_RIDE_WEIGHTS = {
    "morning_peak": 3.2,
    "normal": 0.8,
    "evening_peak": 2.8,
}

HOT_BOARDING_STATIONS = {
    "1": [4, 6, 9, 12, 16],
    "2": [3, 5, 8, 11],
    "3": [5, 8, 13, 18, 24],
}


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def dt_sql(value: datetime) -> str:
    return sql_string(value.strftime("%Y-%m-%d %H:%M:%S"))


def d_sql(value: date) -> str:
    return sql_string(value.isoformat())


def weighted_route(rng: random.Random) -> str:
    value = rng.random()
    cumulative = 0.0
    for route_code, weight in ROUTE_WEIGHTS.items():
        cumulative += weight
        if value <= cumulative:
            return route_code
    return "3"


def period_for_departure(index_in_day: int) -> str:
    if index_in_day < 28:
        return "morning_peak"
    if index_in_day < 68:
        return "normal"
    return "evening_peak"


def depart_time_for(day: date, period_type: str, index_in_period: int) -> datetime:
    start, end = PERIOD_WINDOWS[period_type]
    start_minutes = start.hour * 60 + start.minute
    end_minutes = end.hour * 60 + end.minute
    span = max(1, end_minutes - start_minutes)
    minute = start_minutes + (index_in_period * 7) % span
    return datetime.combine(day, time(minute // 60, minute % 60))


def weighted_boarding_station(rng: random.Random, route_code: str) -> int:
    station_count = ROUTE_STATION_COUNTS[route_code]
    hot_stations = HOT_BOARDING_STATIONS[route_code]
    if rng.random() < 0.68:
        jittered = rng.choice(hot_stations) + rng.choice([-1, 0, 0, 1])
        return max(1, min(station_count - 1, jittered))
    return rng.randint(1, station_count - 1)


def station_pair(rng: random.Random, route_code: str) -> tuple[int, int]:
    station_count = ROUTE_STATION_COUNTS[route_code]
    boarding = weighted_boarding_station(rng, route_code)
    max_delta = min(8, station_count - boarding)
    if rng.random() < 0.62:
        delta = rng.randint(max(1, max_delta // 2), max_delta)
    else:
        delta = rng.randint(1, max_delta)
    alighting = boarding + delta
    return boarding, alighting


def ride_bounds(period_type: str) -> tuple[int, int]:
    if period_type == "morning_peak":
        return 39, 50
    if period_type == "evening_peak":
        return 34, 47
    return 8, 18


def initial_ride_count(rng: random.Random, period_type: str) -> int:
    low, high = ride_bounds(period_type)
    if period_type == "morning_peak":
        return min(50, round(rng.triangular(low, high, 47)))
    if period_type == "evening_peak":
        return round(rng.triangular(low, high, 42))
    return round(rng.triangular(low, high, 12))


def normalize_ride_counts(rng: random.Random, schedules: list[tuple[str, datetime, str]], ride_count: int) -> list[int]:
    counts = [initial_ride_count(rng, period_type) for _, _, period_type in schedules]
    target = max(0, min(ride_count, 50 * len(schedules)))
    diff = target - sum(counts)
    attempts = 0
    while diff != 0 and attempts < len(counts) * 100:
        index = rng.randrange(len(counts))
        low, high = ride_bounds(schedules[index][2])
        high = min(high, 50)
        if diff > 0 and counts[index] < high:
            counts[index] += 1
            diff -= 1
        elif diff < 0 and counts[index] > low:
            counts[index] -= 1
            diff += 1
        attempts += 1
    return counts


def build_sql(end_date: date, days: int, ride_count: int) -> str:
    rng = random.Random(20260604)
    start_date = end_date - timedelta(days=days - 1)
    statements = [
        "USE bus_agent",
        "START TRANSACTION",
        "DELETE FROM ride_record",
        "DELETE FROM bus_schedule",
        "DELETE FROM bus_vehicle",
        "DELETE FROM driver",
    ]

    for i in range(1, 31):
        statements.append(
            "INSERT INTO bus_vehicle (vehicle_code, plate_no, capacity, status, create_time, update_time) "
            f"VALUES ('BUS{i:03d}', '苏F{i:05d}', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            "ON DUPLICATE KEY UPDATE capacity = VALUES(capacity), status = VALUES(status), update_time = CURRENT_TIMESTAMP"
        )

    for i in range(1, 31):
        statements.append(
            "INSERT INTO driver (employee_no, driver_name, phone, status, create_time, update_time) "
            f"VALUES ('D{i:03d}', '司机{i:02d}', '1380000{i:04d}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            "ON DUPLICATE KEY UPDATE driver_name = VALUES(driver_name), phone = VALUES(phone), status = VALUES(status), update_time = CURRENT_TIMESTAMP"
        )

    schedules: list[tuple[str, datetime, str]] = []
    period_counters: dict[tuple[date, str], int] = {}
    for day_offset in range(days):
        current_day = start_date + timedelta(days=day_offset)
        for index in range(100):
            period_type = period_for_departure(index)
            key = (current_day, period_type)
            period_index = period_counters.get(key, 0)
            period_counters[key] = period_index + 1
            depart_time = depart_time_for(current_day, period_type, period_index)
            route_code = weighted_route(rng)
            vehicle_code = f"BUS{((day_offset * 100 + index) % 30) + 1:03d}"
            employee_no = f"D{((day_offset * 100 + index + 7) % 30) + 1:03d}"
            schedules.append((route_code, depart_time, period_type))
            statements.append(
                "INSERT INTO bus_schedule (route_id, vehicle_id, driver_id, schedule_date, depart_time, period_type, status, create_time, update_time) "
                "SELECT br.id, bv.id, d.id, "
                f"{d_sql(current_day)}, {dt_sql(depart_time)}, {sql_string(period_type)}, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                "FROM bus_route br, bus_vehicle bv, driver d "
                f"WHERE br.route_code = {sql_string(route_code)} "
                f"AND bv.vehicle_code = {sql_string(vehicle_code)} "
                f"AND d.employee_no = {sql_string(employee_no)} "
                "ON DUPLICATE KEY UPDATE status = VALUES(status), period_type = VALUES(period_type), update_time = CURRENT_TIMESTAMP"
            )

    ride_counts = normalize_ride_counts(rng, schedules, ride_count)
    for (route_code, depart_time, schedule_period), schedule_ride_count in zip(schedules, ride_counts):
        for _ in range(schedule_ride_count):
            if schedule_period == "morning_peak":
                offset_minutes = rng.randint(0, 105)
            elif schedule_period == "evening_peak":
                offset_minutes = rng.randint(0, 110)
            else:
                offset_minutes = rng.randint(0, 360)
            ride_time = depart_time + timedelta(minutes=offset_minutes)
            boarding, alighting = station_pair(rng, route_code)
            statements.append(
                "INSERT INTO ride_record (route_id, schedule_id, boarding_station_id, alighting_station_id, ride_time, period_type, pay_type, create_time) "
                "SELECT br.id, bs.id, board_station.station_id, alight_station.station_id, "
                f"{dt_sql(ride_time)}, {sql_string(schedule_period)}, 'mock', CURRENT_TIMESTAMP "
                "FROM bus_route br "
                "JOIN bus_schedule bs ON bs.route_id = br.id "
                "JOIN route_station board_station ON board_station.route_id = br.id "
                "JOIN route_station alight_station ON alight_station.route_id = br.id "
                f"WHERE br.route_code = {sql_string(route_code)} "
                f"AND bs.depart_time = {dt_sql(depart_time)} "
                f"AND board_station.station_order = {boarding} "
                f"AND alight_station.station_order = {alighting} "
                "LIMIT 1"
            )

    statements.append("COMMIT")
    return ";\n".join(statements) + ";\n"


def execute_sql(sql: str) -> None:
    proc = subprocess.run(
        [
            "docker",
            "exec",
            "-i",
            "bus-agent-mysql",
            "mysql",
            "-uroot",
            "-proot123456",
            "--default-character-set=utf8mb4",
        ],
        input=sql,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"mysql mock-data import failed with exit code {proc.returncode}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate deterministic mock operation data.")
    parser.add_argument("--end-date", default="2026-06-04", help="Last operation date, YYYY-MM-DD.")
    parser.add_argument("--days", type=int, default=30, help="Number of days to generate.")
    parser.add_argument("--ride-count", type=int, default=90000, help="Number of ride records to generate.")
    parser.add_argument("--dry-run", action="store_true", help="Print generated SQL instead of executing it.")
    args = parser.parse_args()

    sql = build_sql(date.fromisoformat(args.end_date), args.days, args.ride_count)
    if args.dry_run:
        print(sql)
        return
    execute_sql(sql)
    print(f"Generated 30 vehicles, 30 drivers, {args.days * 100} schedules, {args.ride_count} ride records.")


if __name__ == "__main__":
    main()
