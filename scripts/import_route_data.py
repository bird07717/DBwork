import argparse
import hashlib
import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ROUTE_DIR = ROOT / "data" / "real_routes"


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def station_code(station: dict) -> str:
    raw = f"{station['stationName']}|{station['longitude']:.6f}|{station['latitude']:.6f}"
    return "ST" + hashlib.sha1(raw.encode("utf-8")).hexdigest()[:12].upper()


def load_routes() -> list[dict]:
    routes = []
    for path in sorted(ROUTE_DIR.glob("route_*.json")):
        routes.append(json.loads(path.read_text(encoding="utf-8")))
    if not routes:
        raise RuntimeError(f"No route JSON files found under {ROUTE_DIR}")
    return routes


def build_sql(routes: list[dict]) -> str:
    route_codes = [route["routeCode"] for route in routes]
    statements = [
        "USE bus_agent",
        "START TRANSACTION",
    ]

    codes_sql = ", ".join(sql_string(code) for code in route_codes)
    statements.append(
        "DELETE rs FROM route_station rs "
        "JOIN bus_route br ON rs.route_id = br.id "
        f"WHERE br.route_code IN ({codes_sql})"
    )

    seen_stations = {}
    for route in routes:
        direction = route.get("direction") or f"{route['startStation']} -> {route['endStation']}"
        statements.append(
            "INSERT INTO bus_route ("
            "route_code, route_name, direction, start_station_name, end_station_name, status, create_time, update_time"
            ") VALUES ("
            f"{sql_string(route['routeCode'])}, "
            f"{sql_string(route['routeName'])}, "
            f"{sql_string(direction)}, "
            f"{sql_string(route['startStation'])}, "
            f"{sql_string(route['endStation'])}, "
            "1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
            ") ON DUPLICATE KEY UPDATE "
            "route_name = VALUES(route_name), "
            "direction = VALUES(direction), "
            "start_station_name = VALUES(start_station_name), "
            "end_station_name = VALUES(end_station_name), "
            "status = VALUES(status), "
            "update_time = CURRENT_TIMESTAMP"
        )

        for station in route["stations"]:
            code = station_code(station)
            if code not in seen_stations:
                seen_stations[code] = station
                statements.append(
                    "INSERT INTO station ("
                    "station_code, station_name, area_name, longitude, latitude, status, create_time, update_time"
                    ") VALUES ("
                    f"{sql_string(code)}, "
                    f"{sql_string(station['stationName'])}, "
                    "NULL, "
                    f"{station['longitude']:.6f}, "
                    f"{station['latitude']:.6f}, "
                    "1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP"
                    ") ON DUPLICATE KEY UPDATE "
                    "station_name = VALUES(station_name), "
                    "longitude = VALUES(longitude), "
                    "latitude = VALUES(latitude), "
                    "status = VALUES(status), "
                    "update_time = CURRENT_TIMESTAMP"
                )

            statements.append(
                "INSERT INTO route_station (route_id, station_id, station_order, distance_from_start, create_time) "
                "SELECT br.id, s.id, "
                f"{int(station['stationOrder'])}, NULL, CURRENT_TIMESTAMP "
                "FROM bus_route br, station s "
                f"WHERE br.route_code = {sql_string(route['routeCode'])} "
                f"AND s.station_code = {sql_string(code)} "
                "ON DUPLICATE KEY UPDATE "
                "station_id = VALUES(station_id), "
                "station_order = VALUES(station_order), "
                "distance_from_start = VALUES(distance_from_start)"
            )

    statements.extend(["COMMIT"])
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
        raise RuntimeError(f"mysql import failed with exit code {proc.returncode}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Import real bus routes into MySQL.")
    parser.add_argument("--dry-run", action="store_true", help="Print generated SQL instead of executing it.")
    args = parser.parse_args()

    routes = load_routes()
    sql = build_sql(routes)
    if args.dry_run:
        print(sql)
        return
    execute_sql(sql)
    print(f"Imported {len(routes)} routes from {ROUTE_DIR}")


if __name__ == "__main__":
    main()
