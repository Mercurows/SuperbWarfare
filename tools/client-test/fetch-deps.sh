#!/usr/bin/env bash
# Fetch the client-test mod jars that the build needs as compileOnly inputs.
#
# `compat/controllable` compiles against Controllable's controller API, so
# Gradle resolves
#     superbwarfare.test:controllable-forge-1.20.1:0.21.9
# from the flatDir repository `tools/client-test/mods`. Those jars are covered
# by the repository-wide `*.jar` ignore rule, so a fresh clone does not have
# them and the build fails with
#     Could not find superbwarfare.test:controllable-forge-1.20.1:0.21.9
# until they are downloaded. This script does only that: it never launches the
# client and never touches `run/mods`.
#
# Controllable 0.21.9 optionally wants JEI >= 15.20.0.112, but Forge treats the
# range as a hard load error while this project pins 15.20.0.106. The test jar
# therefore has its JEI range relaxed to `[15.20.0,)`; the patch is applied here
# exactly like `run-client.sh` does, and it is deterministic, so the result is
# checksum-verified too.
#
# Usage:
#   ./tools/client-test/fetch-deps.sh          # download what is missing
#   ./tools/client-test/fetch-deps.sh --force  # re-download even if present
#
# Exits non-zero if a download fails or a checksum does not match.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODS="$ROOT/tools/client-test/mods"
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

# Reuse the same proxy autodetection as run-client.sh so the two scripts behave
# identically on this machine.
if [[ -z "${https_proxy:-}${HTTPS_PROXY:-}" ]] && command -v ss >/dev/null 2>&1 \
    && ss -ltn 2>/dev/null | grep -q '127.0.0.1:7897'; then
  export http_proxy=http://127.0.0.1:7897
  export https_proxy=http://127.0.0.1:7897
  export HTTP_PROXY="$http_proxy"
  export HTTPS_PROXY="$https_proxy"
  no_proxy="localhost,127.0.0.1"
  export no_proxy NO_PROXY="$no_proxy"
fi

mkdir -p "$MODS"

CONTROLLABLE="controllable-forge-1.20.1-0.21.9.jar"
FRAMEWORK="framework-forge-1.20.1-0.8.0.jar"

# name|url|raw_sha256|final_sha256
DEPS=(
  "$CONTROLLABLE|https://mediafilez.forgecdn.net/files/7943/200/controllable-forge-1.20.1-0.21.9.jar|937f628547fb077ac207ecf1180541c707c26beafa3e98b1ab239ef22bdf2b80|6b0e3939d994be0250ab4c34e514ffbf60ad65a96905099e318038b5edbd0f55"
  "$FRAMEWORK|https://mediafilez.forgecdn.net/files/7573/740/framework-forge-1.20.1-0.8.0.jar|37c267f46d8b8dfa81bae8c9fa9fa6467e456cadecae10722ee91b43437f25a0|37c267f46d8b8dfa81bae8c9fa9fa6467e456cadecae10722ee91b43437f25a0"
)

sha_of() { sha256sum "$1" | cut -d' ' -f1; }

# Relax Controllable's JEI pin. Idempotent and deterministic: the same input
# always produces the same bytes, which is what makes the final checksum usable.
relax_jei_range() {
  python3 - "$1" <<'PY'
import sys, zipfile, os

path = sys.argv[1]
marker = b'versionRange="[15.20.0,)"'
with zipfile.ZipFile(path) as zin:
    text = zin.read("META-INF/mods.toml")
if marker in text:
    sys.exit(0)
old = b'versionRange="[15.20.0.112,)"'
if old not in text:
    # A different Controllable build may not carry the pin at all.
    sys.exit(0)
tmp = path + ".patched"
with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w") as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "META-INF/mods.toml":
            data = data.replace(old, marker, 1)
        zout.writestr(item, data)
os.replace(tmp, path)
PY
}

failed=0
for entry in "${DEPS[@]}"; do
  IFS='|' read -r name url raw_sha final_sha <<<"$entry"
  target="$MODS/$name"

  if [[ -s "$target" && $FORCE -eq 0 ]]; then
    current="$(sha_of "$target")"
    if [[ "$current" == "$final_sha" ]]; then
      echo "已存在且校验通过: $name"
      continue
    fi
    if [[ "$current" == "$raw_sha" ]]; then
      # Present but unpatched: finish the job instead of downloading again.
      relax_jei_range "$target"
      if [[ "$(sha_of "$target")" == "$final_sha" ]]; then
        echo "已存在，补丁后校验通过: $name"
        continue
      fi
    fi
    echo "已有文件校验不符，重新下载: $name" >&2
  fi

  echo "下载 $name"
  if ! curl -fL --retry 3 --connect-timeout 20 -A "SuperbWarfare-fetch-deps" \
      -o "$target.partial" "$url"; then
    echo "下载失败: $name ($url)" >&2
    rm -f "$target.partial"
    failed=1
    continue
  fi

  if [[ "$(sha_of "$target.partial")" != "$raw_sha" ]]; then
    echo "下载内容校验失败（源文件已变化？）: $name" >&2
    echo "  期望 $raw_sha" >&2
    echo "  实际 $(sha_of "$target.partial")" >&2
    rm -f "$target.partial"
    failed=1
    continue
  fi
  mv -f "$target.partial" "$target"

  if [[ "$name" == "$CONTROLLABLE" ]]; then
    relax_jei_range "$target"
  fi

  if [[ "$(sha_of "$target")" == "$final_sha" ]]; then
    echo "完成: $name"
  else
    echo "最终校验失败: $name" >&2
    echo "  期望 $final_sha" >&2
    echo "  实际 $(sha_of "$target")" >&2
    failed=1
  fi
done

if [[ $failed -ne 0 ]]; then
  echo >&2
  echo "有依赖未能就绪。构建会报 Could not find superbwarfare.test:..." >&2
  exit 1
fi

echo
echo "全部就绪: $MODS"
echo "现在可以离线编译: ./gradlew classes --offline"
