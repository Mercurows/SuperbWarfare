#!/usr/bin/env python3
"""Summarize bounded server traces without treating missing samples as a pass."""
import argparse
from collections import Counter, defaultdict
import gzip
import math
import re


def world_velocity(local, rotation):
    """Invert the trace's inverse-quaternion transform; never equate local XZ to world XZ."""
    x, y, z = local
    qx, qy, qz, qw = rotation
    tx, ty, tz = 2 * (qy * z - qz * y), 2 * (qz * x - qx * z), 2 * (qx * y - qy * x)
    return (x + qw * tx + qy * tz - qz * ty,
            y + qw * ty + qz * tx - qx * tz,
            z + qw * tz + qx * ty - qy * tx)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", help="GameTest output or run/logs/latest.log")
    args = parser.parse_args()
    vehicles = defaultdict(lambda: {"events": Counter(), "steps": [], "terrain_issues": Counter()})
    impulse_sources = defaultdict(Counter)
    results = []
    completed = None
    failed = None
    passed = False
    pattern = re.compile(
        r"\[vehicle-physics\] (\S+) frame=(\d+) reason=(\S+).*?"
        r"pos=\(([^)]+)\) localV=\(([^)]+)\)"
    )
    opener = gzip.open if args.log.endswith(".gz") else open
    with opener(args.log, "rt", encoding="utf-8", errors="replace") as stream:
        for line in stream:
            if ("[track-test]" in line or "[track-test-environment]" in line or "failed!" in line or "GAME TESTS COMPLETE" in line
                    or "required tests failed" in line or "required tests passed" in line):
                results.append(line.rstrip())
            completion = re.search(r"(\d+) GAME TESTS COMPLETE", line)
            if completion:
                completed = int(completion[1])
            failure = re.search(r"(\d+) required tests failed", line)
            if failure:
                failed = int(failure[1])
            if "required tests passed" in line:
                passed = True
            impulse = re.search(r"\[vehicle-impulse\] (\S+).*?source=(.*)", line)
            if impulse:
                source = "fluid-pushing" if "updateFluidHeightAndDoFluidPushing" in impulse[2] else (
                    "test-fixture" if "TrackContactGameTests" in impulse[2] else "other")
                impulse_sources[impulse[1]][source] += 1
            match = pattern.search(line)
            if not match:
                continue
            name, frame, reason, position, velocity = match.groups()
            vehicle = vehicles[name]
            vehicle["events"][reason] += 1
            if reason in ("initial-terrain-wait", "runtime-terrain-loss"):
                issue = re.search(r"terrainIssue=(.*?) active=", line)
                if issue:
                    vehicle["terrain_issues"][issue[1]] += 1
            if reason != "step":
                continue
            position = tuple(map(float, position.split(",")))
            velocity = tuple(map(float, velocity.split(",")))
            rotation = re.search(r"rotation=\(([^)]+)\)", line)
            world = world_velocity(velocity, tuple(map(float, rotation[1].split(",")))) if rotation else None
            contacts = re.findall(r"hit=(true|false) travel=\S+ N=(\S+)", line)
            loaded = sum(hit == "true" and float(force) > 0 for hit, force in contacts)
            vehicle["steps"].append((int(frame), position, velocity, loaded, world))
    for name, sources in sorted(impulse_sources.items()):
        print(name, "bounded_impulse_sources=", dict(sources))
    for name, vehicle in sorted(vehicles.items()):
        print(name, "events=", dict(vehicle["events"]))
        if vehicle["terrain_issues"]:
            print("  terrain_issues=", dict(vehicle["terrain_issues"]))
        samples = vehicle["steps"]
        if not samples:
            print("  no step samples; not validated")
            continue
        frame, position, velocity, loaded, world = samples[-1]
        # This is chassis-local speed, not a substitute for the world-horizontal
        # substep observer (a tilted chassis mixes vertical and local-Z motion).
        print(f"  samples={len(samples)} frames={samples[0][0]}..{frame} "
              f"last_pos={position} last_local_v={velocity} loaded={loaded} "
              f"local_xz_speed={math.hypot(velocity[0], velocity[2]):.6g}")
        if world is not None:
            print(f"  last_world_v={world} world_horizontal_speed={math.hypot(world[0], world[2]):.6g}")
    print("\nGameTest messages (completion alone is not a pass):")
    print("\n".join(results) or "No completion/result messages found.")
    if completed is None or completed == 0:
        print("\nRESULT: NOT VALIDATED (no nonzero completed test roster).")
    elif failed is not None and failed > 0:
        print(f"\nRESULT: FAILED ({failed}/{completed} required tests failed).")
    elif passed:
        print(f"\nRESULT: PASSED reported roster ({completed} completed); verify the expected namespace/count.")
    else:
        print(f"\nRESULT: NOT VALIDATED ({completed} completed but no final pass/failure summary).")


if __name__ == "__main__":
    main()
