import argparse
import random
import subprocess
from datetime import date, datetime, time, timedelta


VEHICLE_CAPACITY = 80
VEHICLE_COUNT = 60
DRIVER_COUNT = 60

ROUTE_PROFILES = {
    "1": {
        "station_count": 27,
        "daily_passenger_range": (4500, 5500),
        "schedule_count_range": (92, 106),
        "schedule_period_shares": {"morning_peak": 0.32, "normal": 0.34, "evening_peak": 0.34},
        "demand_period_shares": {"morning_peak": 0.34, "normal": 0.31, "evening_peak": 0.35},
        "hotspots": {
            "morning_peak": [(1, 4.8), (22, 4.3), (23, 3.8), (14, 2.8), (15, 2.8), (24, 2.0)],
            "normal": [(14, 5.2), (15, 5.2), (24, 3.4), (23, 2.8), (1, 1.8)],
            "evening_peak": [(14, 4.4), (15, 4.4), (1, 4.2), (22, 3.8), (23, 3.4), (24, 1.8)],
        },
    },
    "2": {
        "station_count": 17,
        "daily_passenger_range": (800, 1200),
        "schedule_count_range": (28, 36),
        "schedule_period_shares": {"morning_peak": 0.36, "normal": 0.28, "evening_peak": 0.36},
        "demand_period_shares": {"morning_peak": 0.40, "normal": 0.18, "evening_peak": 0.42},
        "hotspots": {
            "morning_peak": [(1, 4.8), (17, 4.4), (8, 3.2)],
            "normal": [(8, 4.0), (1, 2.6), (17, 2.4)],
            "evening_peak": [(17, 4.8), (1, 4.0), (8, 3.5)],
        },
    },
    "3": {
        "station_count": 33,
        "daily_passenger_range": (5500, 6500),
        "schedule_count_range": (108, 124),
        "schedule_period_shares": {"morning_peak": 0.34, "normal": 0.30, "evening_peak": 0.36},
        "demand_period_shares": {"morning_peak": 0.36, "normal": 0.26, "evening_peak": 0.38},
        "hotspots": {
            "morning_peak": [(1, 5.0), (29, 4.4), (7, 3.7), (22, 2.8), (23, 2.8), (33, 2.4)],
            "normal": [(22, 4.8), (23, 4.8), (32, 4.4), (33, 3.8), (1, 2.2)],
            "evening_peak": [(33, 5.2), (32, 4.8), (22, 4.0), (23, 4.0), (1, 4.0), (29, 2.8)],
        },
    },
}

PERIOD_WINDOWS = {
    "morning_peak": (time(6, 30), time(9, 0)),
    "normal": (time(9, 30), time(16, 30)),
    "evening_peak": (time(16, 30), time(21, 0)),
}

PERIOD_ORDER = ["morning_peak", "normal", "evening_peak"]


class ScheduleSlot(tuple):
    route_code: str
    depart_time: datetime
    period_type: str
    vehicle_code: str
    employee_no: str
    ride_count: int


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def dt_sql(value: datetime) -> str:
    return sql_string(value.strftime("%Y-%m-%d %H:%M:%S"))


def d_sql(value: date) -> str:
    return sql_string(value.isoformat())


def allocate_counts(total: int, weights: dict[str, float]) -> dict[str, int]:
    raw = {key: total * weight for key, weight in weights.items()}
    counts = {key: int(value) for key, value in raw.items()}
    remainder = total - sum(counts.values())
    for key, _ in sorted(raw.items(), key=lambda item: item[1] - int(item[1]), reverse=True)[:remainder]:
        counts[key] += 1
    return counts


def depart_time_for(day: date, period_type: str, index_in_period: int, total_in_period: int, route_code: str) -> datetime:
    start, end = PERIOD_WINDOWS[period_type]
    start_minutes = start.hour * 60 + start.minute
    end_minutes = end.hour * 60 + end.minute
    span = max(1, end_minutes - start_minutes)
    route_offset = {"1": 0, "2": 3, "3": 6}.get(route_code, 0)
    minute = start_minutes + min(span - 1, round((index_in_period + 0.5) * span / max(1, total_in_period)))
    minute = min(end_minutes - 1, minute + route_offset)
    return datetime.combine(day, time(minute // 60, minute % 60))


def build_schedule_slots(rng: random.Random, day: date, day_offset: int, route_code: str, day_target: int) -> list[dict]:
    profile = ROUTE_PROFILES[route_code]
    schedule_total = rng.randint(*profile["schedule_count_range"])
    period_schedule_counts = allocate_counts(schedule_total, profile["schedule_period_shares"])
    period_ride_targets = allocate_counts(day_target, profile["demand_period_shares"])
    slots: list[dict] = []

    for period_type in PERIOD_ORDER:
        period_slots: list[dict] = []
        schedule_count = period_schedule_counts[period_type]
        target = period_ride_targets[period_type]
        if schedule_count <= 0:
            continue
        base = target // schedule_count
        for index in range(schedule_count):
            depart_time = depart_time_for(day, period_type, index, schedule_count, route_code)
            global_index = len(slots) + day_offset * 300 + int(route_code) * 1000
            period_slots.append(
                {
                    "route_code": route_code,
                    "depart_time": depart_time,
                    "period_type": period_type,
                    "vehicle_code": f"BUS{(global_index % VEHICLE_COUNT) + 1:03d}",
                    "employee_no": f"D{((global_index + 11) % DRIVER_COUNT) + 1:03d}",
                    "ride_count": max(0, base + rng.randint(-6, 7)),
                }
            )
        normalize_period_counts(rng, period_slots, target)
        slots.extend(period_slots)
    return slots


def normalize_period_counts(rng: random.Random, slots: list[dict], target: int) -> None:
    if not slots:
        return
    diff = target - sum(slot["ride_count"] for slot in slots)
    attempts = 0
    while diff != 0 and attempts < len(slots) * 200:
        slot = slots[rng.randrange(len(slots))]
        if diff > 0 and slot["ride_count"] < VEHICLE_CAPACITY:
            slot["ride_count"] += 1
            diff -= 1
        elif diff < 0 and slot["ride_count"] > 1:
            slot["ride_count"] -= 1
            diff += 1
        attempts += 1


def scale_daily_targets(day_targets: list[dict], requested_total: int | None) -> None:
    if not requested_total:
        return
    current_total = sum(item["target"] for item in day_targets)
    if current_total <= 0:
        return
    scale = requested_total / current_total
    for item in day_targets:
        item["target"] = max(1, round(item["target"] * scale))


def weighted_boarding_station(rng: random.Random, route_code: str, period_type: str) -> int:
    profile = ROUTE_PROFILES[route_code]
    station_count = profile["station_count"]
    hotspots = profile["hotspots"][period_type]
    if rng.random() < 0.76:
        total_weight = sum(weight for _, weight in hotspots)
        value = rng.random() * total_weight
        cumulative = 0.0
        selected = hotspots[-1][0]
        for station_order, weight in hotspots:
            cumulative += weight
            if value <= cumulative:
                selected = station_order
                break
        if rng.random() < 0.24:
            selected += rng.choice([-1, 1])
        return max(1, min(station_count, selected))
    return rng.randint(1, station_count)


def station_pair(rng: random.Random, route_code: str, period_type: str) -> tuple[int, int]:
    station_count = ROUTE_PROFILES[route_code]["station_count"]
    boarding = weighted_boarding_station(rng, route_code, period_type)
    if boarding >= station_count:
        alighting = rng.randint(max(1, station_count - 10), station_count - 1)
    elif boarding <= 1 and rng.random() < 0.26:
        alighting = rng.randint(2, station_count)
    elif rng.random() < 0.18:
        alighting = rng.randint(1, max(1, boarding - 1))
    else:
        max_delta = max(1, min(10, station_count - boarding))
        delta = rng.randint(max(1, max_delta // 2), max_delta)
        alighting = min(station_count, boarding + delta)
    if alighting == boarding:
        alighting = station_count if boarding < station_count else 1
    return boarding, alighting


def ride_time_for(rng: random.Random, depart_time: datetime, period_type: str) -> datetime:
    if period_type == "morning_peak":
        return depart_time + timedelta(minutes=rng.randint(0, 55))
    if period_type == "evening_peak":
        return depart_time + timedelta(minutes=rng.randint(0, 70))
    return depart_time + timedelta(minutes=rng.randint(0, 95))


def build_sql(end_date: date, days: int, ride_count: int | None) -> str:
    rng = random.Random(20260604)
    start_date = end_date - timedelta(days=days - 1)
    statements = [
        "USE bus_agent",
        "START TRANSACTION",
        "DELETE FROM dispatch_advice",
        "DELETE FROM ride_record",
        "DELETE FROM bus_schedule",
        "DELETE FROM bus_vehicle",
        "DELETE FROM driver",
    ]

    for i in range(1, VEHICLE_COUNT + 1):
        statements.append(
            "INSERT INTO bus_vehicle (vehicle_code, plate_no, capacity, status, create_time, update_time) "
            f"VALUES ('BUS{i:03d}', '苏F{i:05d}', {VEHICLE_CAPACITY}, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            "ON DUPLICATE KEY UPDATE capacity = VALUES(capacity), status = VALUES(status), update_time = CURRENT_TIMESTAMP"
        )

    for i in range(1, DRIVER_COUNT + 1):
        statements.append(
            "INSERT INTO driver (employee_no, driver_name, phone, status, create_time, update_time) "
            f"VALUES ('D{i:03d}', '司机{i:02d}', '1380000{i:04d}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            "ON DUPLICATE KEY UPDATE driver_name = VALUES(driver_name), phone = VALUES(phone), status = VALUES(status), update_time = CURRENT_TIMESTAMP"
        )

    day_targets: list[dict] = []
    for day_offset in range(days):
        current_day = start_date + timedelta(days=day_offset)
        weekday_factor = 0.94 if current_day.weekday() >= 5 else 1.0
        for route_code, profile in ROUTE_PROFILES.items():
            raw_target = rng.randint(*profile["daily_passenger_range"])
            day_targets.append({"day": current_day, "day_offset": day_offset, "route_code": route_code, "target": round(raw_target * weekday_factor)})
    scale_daily_targets(day_targets, ride_count)

    schedules: list[dict] = []
    for item in day_targets:
        schedules.extend(build_schedule_slots(rng, item["day"], item["day_offset"], item["route_code"], item["target"]))

    for slot in schedules:
        statements.append(
            "INSERT INTO bus_schedule (route_id, vehicle_id, driver_id, schedule_date, depart_time, period_type, status, create_time, update_time) "
            "SELECT br.id, bv.id, d.id, "
            f"{d_sql(slot['depart_time'].date())}, {dt_sql(slot['depart_time'])}, {sql_string(slot['period_type'])}, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
            "FROM bus_route br, bus_vehicle bv, driver d "
            f"WHERE br.route_code = {sql_string(slot['route_code'])} "
            f"AND bv.vehicle_code = {sql_string(slot['vehicle_code'])} "
            f"AND d.employee_no = {sql_string(slot['employee_no'])} "
            "ON DUPLICATE KEY UPDATE route_id = VALUES(route_id), vehicle_id = VALUES(vehicle_id), driver_id = VALUES(driver_id), "
            "schedule_date = VALUES(schedule_date), depart_time = VALUES(depart_time), period_type = VALUES(period_type), "
            "status = VALUES(status), update_time = CURRENT_TIMESTAMP"
        )

    for slot in schedules:
        for _ in range(slot["ride_count"]):
            ride_time = ride_time_for(rng, slot["depart_time"], slot["period_type"])
            boarding, alighting = station_pair(rng, slot["route_code"], slot["period_type"])
            statements.append(
                "INSERT INTO ride_record (route_id, schedule_id, boarding_station_id, alighting_station_id, ride_time, period_type, pay_type, create_time) "
                "SELECT br.id, bs.id, board_station.station_id, alight_station.station_id, "
                f"{dt_sql(ride_time)}, {sql_string(slot['period_type'])}, '调查口径模拟', CURRENT_TIMESTAMP "
                "FROM bus_route br "
                "JOIN bus_schedule bs ON bs.route_id = br.id "
                "JOIN bus_vehicle bv ON bv.id = bs.vehicle_id "
                "JOIN route_station board_station ON board_station.route_id = br.id "
                "JOIN route_station alight_station ON alight_station.route_id = br.id "
                f"WHERE br.route_code = {sql_string(slot['route_code'])} "
                f"AND bs.depart_time = {dt_sql(slot['depart_time'])} "
                f"AND bv.vehicle_code = {sql_string(slot['vehicle_code'])} "
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
    parser = argparse.ArgumentParser(description="Generate deterministic operation data from investigated route demand ranges.")
    parser.add_argument("--end-date", default="2026-06-04", help="Last operation date, YYYY-MM-DD.")
    parser.add_argument("--days", type=int, default=30, help="Number of days to generate.")
    parser.add_argument("--ride-count", type=int, default=None, help="Optional total ride records. Omit to use investigated daily ranges.")
    parser.add_argument("--dry-run", action="store_true", help="Print generated SQL instead of executing it.")
    args = parser.parse_args()

    sql = build_sql(date.fromisoformat(args.end_date), args.days, args.ride_count)
    if args.dry_run:
        print(sql)
        return
    execute_sql(sql)
    mode = f"{args.ride_count} ride records" if args.ride_count else "investigated daily passenger ranges"
    print(f"Generated {VEHICLE_COUNT} vehicles, {DRIVER_COUNT} drivers, {args.days} days of schedules and ride records using {mode}.")


if __name__ == "__main__":
    main()
