#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOUNDS_ROOT="$ROOT/src/main/resources/assets/superbwarfare/sounds"
SOUNDS_JSON="$ROOT/src/main/resources/assets/superbwarfare/sounds.json"

failures=0

fail() {
    printf 'FAIL: %s\n' "$*" >&2
    failures=$((failures + 1))
}

for command in ffmpeg ffprobe python3; do
    command -v "$command" >/dev/null || {
        printf 'Missing required command: %s\n' "$command" >&2
        exit 2
    }
done

# Vehicle/cannon recordings replaced by the current sound pass are positional in every view.
# OpenAL spatialises positional sounds correctly only when the source is mono.
shot_dirs=(
    bl_132 hpj11 mk_42
    vehicle/a_10a vehicle/ac_130h vehicle/ah_6 vehicle/bmp_2 vehicle/bradley
    vehicle/happiest_ghast vehicle/lav_150 vehicle/lav_ad vehicle/m_1a_2
    vehicle/mi_28 vehicle/plz_05 vehicle/t_90a vehicle/yx_100 vehicle/ztz_99a
)

while IFS= read -r -d '' sound; do
    channels="$(ffprobe -v error -select_streams a:0 -show_entries stream=channels -of default=nw=1:nk=1 "$sound")"
    if (( channels != 1 )); then
        fail "positional shot is not mono: ${sound#"$ROOT/"} ($channels channels)"
    fi
done < <(
    for dir in "${shot_dirs[@]}"; do
        find "$SOUNDS_ROOT/$dir" -type f -name '*.ogg' \
            \( -iname '*fire*' -o -iname '*far*' \) -print0
    done
)

while IFS= read -r -d '' sound; do
    stats="$(ffmpeg -nostdin -hide_banner -nostats -i "$sound" -af volumedetect -f null - 2>&1)"
    mean_volume="$(printf '%s\n' "$stats" | sed -n 's/.*mean_volume: \([-0-9.]*\) dB/\1/p')"
    if [[ -z "$mean_volume" ]] || awk -v mean="$mean_volume" 'BEGIN { exit !(mean < -18.0) }'; then
        fail "distant engine layer is too quiet: ${sound#"$ROOT/"} (${mean_volume:-unknown} dB mean)"
    fi
done < <(find "$SOUNDS_ROOT/vehicle/engine" -type f -name '*distance*.ogg' -print0)

python3 - "$SOUNDS_JSON" "$SOUNDS_ROOT" "$ROOT" <<'PY' || failures=$((failures + 1))
import json
import pathlib
import sys

sounds_json = pathlib.Path(sys.argv[1])
sounds_root = pathlib.Path(sys.argv[2])
project_root = pathlib.Path(sys.argv[3])
events = json.loads(sounds_json.read_text(encoding="utf-8"))

referenced = set()
bad_distance_events = []
for event_name, event in events.items():
    for value in event.get("sounds", []):
        sound = {"name": value} if isinstance(value, str) else value
        name = sound.get("name")
        if name:
            referenced.add(sounds_root / f"{name.removeprefix('superbwarfare:')}.ogg")
        if event_name.startswith("engine_") and "distance" in event_name:
            if sound.get("attenuation_distance", 16) < 384:
                bad_distance_events.append(event_name)

files = set(sounds_root.rglob("*.ogg"))
orphans = sorted(files - referenced)
missing = sorted(referenced - files)

missing_events = []
for data_file in (project_root / "src/main/resources/data/superbwarfare/sbw").rglob("*.json"):
    data = json.loads(data_file.read_text(encoding="utf-8"))

    def visit(value):
        if isinstance(value, dict):
            sound_info = value.get("SoundInfo")
            if isinstance(sound_info, dict):
                for sound_value in sound_info.values():
                    values = sound_value if isinstance(sound_value, list) else [sound_value]
                    for event in values:
                        if isinstance(event, str) and event.startswith("superbwarfare:"):
                            event_id = event.removeprefix("superbwarfare:")
                            if event_id not in events:
                                missing_events.append(f"{data_file}: {event_id}")
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(data)

if orphans:
    print("FAIL: unreferenced .ogg files:\n  " + "\n  ".join(map(str, orphans)), file=sys.stderr)
if missing:
    print("FAIL: sounds.json references missing .ogg files:\n  " + "\n  ".join(map(str, missing)), file=sys.stderr)
if missing_events:
    print("FAIL: SoundInfo references undefined sound events:\n  " + "\n  ".join(sorted(set(missing_events))), file=sys.stderr)
if bad_distance_events:
    print(
        "FAIL: engine distant events must use attenuation_distance >= 384:\n  "
        + "\n  ".join(sorted(set(bad_distance_events))),
        file=sys.stderr,
    )

if orphans or missing or missing_events or bad_distance_events:
    raise SystemExit(1)
PY

abrams_start_int="$SOUNDS_ROOT/vehicle/engine/abrams/start_int.ogg"
abrams_start_ext="$SOUNDS_ROOT/vehicle/engine/abrams/start_ext.ogg"
duration_int="$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$abrams_start_int")"
duration_ext="$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$abrams_start_ext")"
duration="$(python3 -c 'import sys; print(max(map(float, sys.argv[1:])))' "$duration_int" "$duration_ext")"
expected_ticks="$(python3 -c 'import math,sys; print(math.ceil(float(sys.argv[1]) * 20))' "$duration")"
startup_source="$ROOT/src/main/kotlin/com/atsuishio/superbwarfare/entity/vehicle/M1A2Entity.kt"
if ! grep -Eq "engineStartupDurationTicks\(\).*${expected_ticks}" "$startup_source"; then
    fail "Abrams startup must last $expected_ticks ticks (longest startup layer is ${duration}s)"
fi

if (( failures > 0 )); then
    printf '%d sound asset check(s) failed.\n' "$failures" >&2
    exit 1
fi

printf 'Sound assets verified.\n'
