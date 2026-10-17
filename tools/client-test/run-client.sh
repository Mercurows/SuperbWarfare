#!/usr/bin/env bash
# Launch the Superb Warfare Forge dev client with Controllable and a Legion Go
# split-controller bridge. Intended to be started from the KDE application menu.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STATE="${XDG_STATE_HOME:-$HOME/.local/state}/superbwarfare-test"
SHARE="${XDG_DATA_HOME:-$HOME/.local/share}/superbwarfare-test"
MODS="$SHARE/mods"
LOG="$STATE/client.log"
JAVA_HOME_DEFAULT="$HOME/.jdks/jdk-17"

mkdir -p "$STATE" "$ROOT/run/mods" "$ROOT/run/config/controllable"
exec > >(tee -a "$LOG") 2>&1
echo "==== $(date '+%F %T') Superb Warfare test client ===="

if [[ -x "$JAVA_HOME_DEFAULT/bin/java" ]]; then
  export JAVA_HOME="$JAVA_HOME_DEFAULT"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [[ -z "${https_proxy:-}${HTTPS_PROXY:-}" ]] && ss -ltn | grep -q '127.0.0.1:7897'; then
  export http_proxy=http://127.0.0.1:7897
  export https_proxy=http://127.0.0.1:7897
  export HTTP_PROXY="$http_proxy"
  export HTTPS_PROXY="$https_proxy"
  export no_proxy=localhost,127.0.0.1,::1
  export NO_PROXY="$no_proxy"
fi

install_mod() {
  local name="$1" url="$2"
  if [[ ! -s "$MODS/$name" ]]; then
    mkdir -p "$MODS"
    echo "下载 $name"
    curl -fL --retry 3 -A "SuperbWarfare-test" -o "$MODS/$name.partial" "$url"
    mv "$MODS/$name.partial" "$MODS/$name"
  fi
  cp -f "$MODS/$name" "$ROOT/run/mods/$name"
}

install_mod controllable-forge-1.20.1-0.21.9.jar \
  "https://mediafilez.forgecdn.net/files/7943/200/controllable-forge-1.20.1-0.21.9.jar"
install_mod framework-forge-1.20.1-0.8.0.jar \
  "https://mediafilez.forgecdn.net/files/7573/740/framework-forge-1.20.1-0.8.0.jar"

# Controllable 0.21.9 optionally wants JEI >= 15.20.0.112. Forge still treats
# that as a hard load error, and this project pins 15.20.0.106. Relax only the
# test jar so the dev client can start without changing the mod's JEI pin.
python3 - "$MODS/controllable-forge-1.20.1-0.21.9.jar" << 'PY'
import sys, zipfile, os
path = sys.argv[1]
marker = b'versionRange="[15.20.0,)"'
with zipfile.ZipFile(path) as zin:
    text = zin.read("META-INF/mods.toml")
if marker in text:
    sys.exit(0)
old = b'versionRange="[15.20.0.112,)"'
if old not in text:
    sys.exit(0)
tmp = path + ".patched"
with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, "w") as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == "META-INF/mods.toml":
            data = data.replace(old, marker, 1)
        zout.writestr(item, data)
os.replace(tmp, path)
print("relaxed Controllable JEI range for the test jar")
PY
mkdir -p "$ROOT/tools/client-test/mods"
cp -f "$MODS/controllable-forge-1.20.1-0.21.9.jar" "$ROOT/tools/client-test/mods/controllable-forge-1.20.1-0.21.9.jar"
cp -f "$MODS/framework-forge-1.20.1-0.8.0.jar" "$ROOT/tools/client-test/mods/framework-forge-1.20.1-0.8.0.jar"
# Production jars in run/mods are not deobfuscated and crash userdev mixins.
rm -f "$ROOT/run/mods/controllable-forge-1.20.1-0.21.9.jar" "$ROOT/run/mods/framework-forge-1.20.1-0.8.0.jar"

# 载具的摇杆转向／油门与扳机开火／瞄准由 SBW 的 compat/controllable 直接读手柄
# （Controllable 已作为可选依赖接入）。这里只补轴表达不了、且必须走按键的操作。
#
# Controllable 的 Key Adapter 会把按钮写成对应 KeyMapping 的按下状态，并合成一次
# 真实按键事件，所以既能驱动轮询 isDown 的载具方向键，也能驱动监听原始事件的开火键。
# 注意：桥只合成标准 Xbox 按键，Legion Go 的背键走厂商接口（root-only），
# 所以 bindings.properties 里 paddle_* 的绑定按不到，sneak 需要改到可达按钮。
ADAPTERS="$ROOT/run/config/controllable/key_adapters.properties"
if ! grep -q '^key\.superbwarfare\.' "$ADAPTERS" 2>/dev/null; then
  echo "写入手柄按键适配（Controllable Key Adapters）"
  cat >> "$ADAPTERS" << 'EOF'

# Superb Warfare 载具键位（由 tools/client-test/run-client.sh 写入）
key.superbwarfare.reload=controllable.button.b
key.superbwarfare.fire_mode=controllable.button.right_bumper
key.superbwarfare.dismount=controllable.button.left_bumper
key.superbwarfare.move_space=controllable.button.left_thumb_stick
key.sneak=controllable.button.right_thumb_stick
EOF
fi

export SBW_RUN_DIR="$ROOT/run"
BRIDGE_PID=""
cleanup() {
  if [[ -n "$BRIDGE_PID" ]] && kill -0 "$BRIDGE_PID" 2>/dev/null; then
    kill "$BRIDGE_PID" 2>/dev/null || true
    wait "$BRIDGE_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "启动 Legion Go 分体手柄桥（供 Controllable 识别为一只 Xbox 手柄）"
python3 "$ROOT/tools/client-test/legion_pad_bridge.py" &
BRIDGE_PID=$!
for _ in $(seq 1 30); do
  if [[ -s "$STATE/pad.ready" ]]; then
    echo "虚拟手柄已就绪: $(cat "$STATE/pad.ready")"
    break
  fi
  if ! kill -0 "$BRIDGE_PID" 2>/dev/null; then
    echo "手柄桥退出了。客户端仍会启动，但分体手柄可能无法合成。"
    BRIDGE_PID=""
    break
  fi
  sleep 0.2
done

PROXY_ARGS=()
proxy_url="${https_proxy:-${HTTPS_PROXY:-}}"
if [[ "$proxy_url" =~ ^https?://([^:/]+):([0-9]+) ]]; then
  PROXY_ARGS=(
    -Dhttp.proxyHost="${BASH_REMATCH[1]}"
    -Dhttp.proxyPort="${BASH_REMATCH[2]}"
    -Dhttps.proxyHost="${BASH_REMATCH[1]}"
    -Dhttps.proxyPort="${BASH_REMATCH[2]}"
  )
fi

cd "$ROOT"
set +e
./gradlew runClient \
  --console=plain \
  -Dnet.minecraftforge.gradle.check.certs=false \
  -Dorg.gradle.java.installations.auto-download=false \
  -Dorg.gradle.java.installations.paths="${JAVA_HOME:-$JAVA_HOME_DEFAULT}" \
  "${PROXY_ARGS[@]}"
status=$?
set -e
exit "$status"
