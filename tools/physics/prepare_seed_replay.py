#!/usr/bin/env python3
"""Prepare a new isolated GameTest run directory; never copy or alter a world."""
import argparse
import json
from pathlib import Path
import re
import shlex
import shutil
import uuid


RUNTIME_DIRECTORIES = (
    "config", "defaultconfigs", "mods", "kubejs", "tacz", "tacz_backup",
    "apricity", "local", "resourcepacks", "tlm_custom_pack", "schematics", "patchouli_books",
)


def prepare(source_run, output_root, seed):
    if not -(2 ** 63) <= seed < 2 ** 63:
        raise ValueError("Minecraft seed must fit a signed 64-bit integer")
    source_run = Path(source_run).resolve(strict=True)
    if not source_run.is_dir():
        raise ValueError("Source run must be a directory")
    properties = source_run / "server.properties"
    text = properties.read_text(encoding="utf-8") if properties.exists() else ""
    # Replace all existing seed entries (including Java-properties ':' syntax).
    # Leave other settings unchanged and never write the source properties.
    text = re.sub(r"(?m)^[ \t]*level-seed[ \t]*[=:][^\r\n]*", f"level-seed={seed}", text)
    if not re.search(r"(?m)^level-seed=", text):
        text = text.rstrip("\r\n") + f"\nlevel-seed={seed}\n"
    output_root = Path(output_root).resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    target = output_root / f"track-seed-replay-{uuid.uuid4()}"
    target.mkdir()  # A collision must fail, never merge into an existing run.
    copied = []
    for name in RUNTIME_DIRECTORIES:
        source = source_run / name
        if source.is_dir():
            shutil.copytree(source, target / name)
            copied.append(name)
    (target / "server.properties").write_text(text, encoding="utf-8")
    (target / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (target / "seed-replay.json").write_text(json.dumps({
        "seed": seed, "sourceRun": str(source_run), "copiedDirectories": copied,
        "worldCopied": False,
        "note": "Same generation seed, not a deterministic replay of scheduling or entity RNG",
    }, indent=2) + "\n", encoding="utf-8")
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", required=True, type=int)
    parser.add_argument("--source-run", type=Path, default=Path(__file__).resolve().parents[2] / "run")
    parser.add_argument("--output-root", type=Path, default=Path("/tmp/opencode"))
    args = parser.parse_args()
    target = prepare(args.source_run, args.output_root, args.seed)
    print(f"Prepared: {target}")
    print("Run from the repository root (this tool does not launch Gradle):")
    print(shlex.join([
        "./gradlew", "runGameTestServer", "--offline", "--console=plain", "--no-daemon",
        f"-PgameTestRunDirectory={target}", "-PgameTestNamespaces=superbwarfare_physics",
        "-PclientTestMods=false", "-PphysicsTraceSteps=3600",
        "-Dnet.minecraftforge.gradle.check.certs=false",
        "-Dorg.gradle.java.installations.auto-download=false",
        "-Dorg.gradle.java.installations.paths=/home/kaguya/.jdks/jdk-17",
    ]))


if __name__ == "__main__":
    main()
