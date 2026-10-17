#!/usr/bin/env python3
"""Read-only check of server road-wheel data against the built-in Bedrock pivots.

SBM reflects model X, then the vehicle renderer applies yaw +180 degrees:
authored physics coordinates are therefore (model.x, model.y, -model.z) / 16.
Bone ids are explicit: PLZ sub-bones are not wheels and YX ids are not ordered.
Radii describe road wheels, not their suspension arms or the track envelope.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources"
COUNTS = {"bmp_2": 6, "bradley": 6, "m_1a_2": 7, "plz_05": 6,
          "prism_tank": 5, "t_90a": 6, "yx_100": 7, "ztz_99a": 6}


def main():
    for name, count in COUNTS.items():
        data = json.loads((ROOT / f"data/superbwarfare/sbw/vehicles/{name}.json").read_text())
        model = json.loads((ROOT / f"assets/superbwarfare/models/bedrock/vehicle/{name}.geo.json").read_text())
        bones = {b["name"]: b for b in model["minecraft:geometry"][0]["bones"]}
        for side, letter in [("Left", "L"), ("Right", "R")]:
            wheels = [w for w in data["Wheels"] if w["Side"] == side]
            assert len({w["Index"] for w in wheels}) == len(wheels), (name, side, "duplicate id")
            for role, expected in [("Support", count), ("DriveGuide", 1), ("IdlerGuide", 1)]:
                assert sum(w["Role"] == role for w in wheels) == expected, (name, side, role)
            for wheel in wheels:
                pivot = bones[f"wheel{letter}{wheel['Index']}"]["pivot"]
                expected = [pivot[0] / 16, pivot[1] / 16, -pivot[2] / 16]
                assert max(abs(a - b) for a, b in zip(wheel["Position"], expected)) < 2e-6, (name, wheel)
                assert .05 <= wheel["Radius"] <= wheel.get("ContactRadius", wheel["Radius"]) <= 2
        print(f"{name}: {count} SUPPORT per side; guide counts, indices and model pivots OK")


if __name__ == "__main__":
    main()
