#!/usr/bin/env python3
"""Merge Legion Go split controllers into one virtual Xbox 360 pad.

Handheld Daemon only emulates a full gamepad while the firmware is in xinput
mode. Detached / dual_dinput controllers show up as two HID gamepads (report
IDs 7 and 8 on 17ef:61ed). Controllable speaks SDL and expects one pad, so
this bridge reads both evdev nodes — the same devices HHD enumerates — and
publishes a virtual pad whose layout matches Controllable's Linux Xbox 360
mapping.

The vendor HID interface HHD uses for shortcuts is root-only. Button names in
pad-map.json follow HHD's Legion Go map (A/B/X/Y on the right half, d-pad and
LB on the left). Set LEGION_SWAP_SIDES=1 if the halves come up reversed.
"""

from __future__ import annotations

import json
import os
import select
import signal
import sys
import time
from pathlib import Path

from evdev import AbsInfo, InputDevice, UInput, ecodes, list_devices

ROOT = Path(__file__).resolve().parent
STATE = Path.home() / ".local/state/superbwarfare-test"
MAP_PATH = Path(os.environ.get("LEGION_PAD_MAP", ROOT / "pad-map.json"))
READY = STATE / "pad.ready"
MAPPING = STATE / "gamecontrollerdb.txt"
CONFIG_MAPPING = (
    Path(os.environ.get("SBW_RUN_DIR", ROOT.parents[1] / "run"))
    / "config/controllable/gamecontrollerdb.txt"
)

LEN_VID = 0x17EF
XINPUT_PIDS = {0x6182, 0x61EB}
GAME_BUTTONS = (
    ecodes.BTN_A,
    ecodes.BTN_B,
    ecodes.BTN_X,
    ecodes.BTN_Y,
    ecodes.BTN_TL,
    ecodes.BTN_TR,
    ecodes.BTN_SELECT,
    ecodes.BTN_START,
    ecodes.BTN_MODE,
    ecodes.BTN_THUMBL,
    ecodes.BTN_THUMBR,
)
XBOX_VENDOR = 0x045E
XBOX_PRODUCT = 0x028E
XBOX_VERSION = 0x0104
XBOX_NAME = "Microsoft X-Box 360 pad"
XBOX_MAP = (
    "a:b0,b:b1,back:b6,dpdown:h0.4,dpleft:h0.8,dpright:h0.2,dpup:h0.1,"
    "guide:b8,leftshoulder:b4,leftstick:b9,lefttrigger:a2,leftx:a0,lefty:a1,"
    "rightshoulder:b5,rightstick:b10,righttrigger:a5,rightx:a3,righty:a4,"
    "start:b7,x:b2,y:b3,platform:Linux,"
)
VIRTUAL = {
    "a": ecodes.BTN_A,
    "b": ecodes.BTN_B,
    "x": ecodes.BTN_X,
    "y": ecodes.BTN_Y,
    "lb": ecodes.BTN_TL,
    "rb": ecodes.BTN_TR,
    "back": ecodes.BTN_SELECT,
    "start": ecodes.BTN_START,
    "guide": ecodes.BTN_MODE,
    "ls": ecodes.BTN_THUMBL,
    "rs": ecodes.BTN_THUMBR,
}


def log(msg: str) -> None:
    print(f"[legion-pad] {msg}", flush=True)


def crc16_for_byte(byte: int) -> int:
    crc = 0
    for _ in range(8):
        if (byte ^ crc) & 1:
            crc = (crc >> 1) ^ 0xA001
        else:
            crc >>= 1
        byte >>= 1
    return crc


def sdl_crc16(data: bytes, crc: int = 0) -> int:
    # Same CRC Handheld Daemon uses when it builds an SDL GUID.
    for byte in data:
        crc = crc16_for_byte(crc ^ byte) ^ (crc >> 8)
    return crc & 0xFFFF


def sdl_guid(name: str, vendor: int, product: int, version: int, crc: int | None = None) -> str:
    if crc is None:
        crc = sdl_crc16(name.encode())
    raw = bytearray(16)
    raw[0:2] = (0x0003).to_bytes(2, "little")
    raw[2:4] = crc.to_bytes(2, "little")
    raw[4:6] = vendor.to_bytes(2, "little")
    raw[8:10] = product.to_bytes(2, "little")
    raw[12:14] = version.to_bytes(2, "little")
    return raw.hex()


def hhd_mode() -> str:
    try:
        import socket
        from http.client import HTTPConnection

        class Unix(HTTPConnection):
            def connect(self):
                self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
                self.sock.connect("/run/hhd/api")

        con = Unix("hhd")
        con.request("GET", "/api/v1/state")
        res = con.getresponse()
        if res.status != 200:
            return "unknown"
        text = res.read().decode()
    except Exception as exc:
        log(f"HHD API unavailable ({exc}); falling back to evdev.")
        return "unknown"
    # Firmware mode is not a setting. Infer it from the live evdev name HHD logs,
    # and from whether HHD has already published a full virtual pad.
    if "dual_dinput" in text:
        return "dual_dinput"
    return "configured"


def load_map() -> dict:
    data = json.loads(MAP_PATH.read_text())
    if os.environ.get("LEGION_SWAP_SIDES", "").lower() in {"1", "true", "yes"}:
        data["swap_sides"] = not data.get("swap_sides", False)
    return data


def is_legion_pad(dev: InputDevice) -> bool:
    info = dev.info
    if info.vendor != LEN_VID:
        return False
    if "Legion-Controller" not in (dev.name or ""):
        return False
    if "Keyboard" in dev.name or "Mouse" in dev.name or "Touchpad" in dev.name:
        return False
    caps = dev.capabilities()
    return ecodes.EV_ABS in caps and ecodes.BTN_A in caps.get(ecodes.EV_KEY, [])


def is_hhd_gamepad(dev: InputDevice) -> bool:
    name = dev.name or ""
    if "Shortcuts" in name or "HHD" not in name and "Xbox" not in name and "DualSense" not in name:
        return False
    phys = dev.phys or ""
    if "hhd" not in phys and not name.startswith("Microsoft") and "DualSense" not in name:
        return False
    caps = dev.capabilities()
    axes = caps.get(ecodes.EV_ABS, [])
    keys = caps.get(ecodes.EV_KEY, [])
    return ecodes.ABS_RX in axes and ecodes.BTN_A in keys and "Shortcuts" not in name


def scan() -> tuple[list[InputDevice], bool]:
    pads = []
    hhd_pad = False
    for path in list_devices():
        try:
            dev = InputDevice(path)
        except OSError:
            continue
        try:
            if is_hhd_gamepad(dev):
                hhd_pad = True
                dev.close()
                continue
            if is_legion_pad(dev):
                pads.append(dev)
            else:
                dev.close()
        except OSError:
            dev.close()
    pads.sort(key=lambda d: d.path)
    return pads, hhd_pad


def scale_stick(value: int, invert: bool, deadzone: float) -> int:
    norm = (value - 2048) / 2048.0
    if invert:
        norm = -norm
    norm = max(-1.0, min(1.0, norm))
    if abs(norm) < deadzone:
        return 0
    return int(norm * 32767)


def scale_axis(value: int, span: int) -> int:
    if span <= 0:
        return 0
    return max(0, min(255, int(value / span * 255)))


class Bridge:
    def __init__(self, mapping: dict, dump: bool):
        self.mapping = mapping
        self.dump = dump
        self.ui: UInput | None = None
        self.devices: dict[str, InputDevice] = {}
        self.seen_abs: set[tuple[str, int]] = set()
        self.buttons = {code: 0 for code in GAME_BUTTONS}
        self.axes = {
            ecodes.ABS_X: 0,
            ecodes.ABS_Y: 0,
            ecodes.ABS_Z: 0,
            ecodes.ABS_RX: 0,
            ecodes.ABS_RY: 0,
            ecodes.ABS_RZ: 0,
            ecodes.ABS_HAT0X: 0,
            ecodes.ABS_HAT0Y: 0,
        }
        self.running = True

    def open_virtual(self) -> None:
        if self.ui is not None:
            return
        abs_info = AbsInfo(0, -32768, 32767, 16, 128, 0)
        trigger = AbsInfo(0, 0, 255, 0, 0, 0)
        hat = AbsInfo(0, -1, 1, 0, 0, 0)
        self.ui = UInput(
            {
                ecodes.EV_KEY: list(GAME_BUTTONS),
                ecodes.EV_ABS: [
                    (ecodes.ABS_X, abs_info),
                    (ecodes.ABS_Y, abs_info),
                    (ecodes.ABS_Z, trigger),
                    (ecodes.ABS_RX, abs_info),
                    (ecodes.ABS_RY, abs_info),
                    (ecodes.ABS_RZ, trigger),
                    (ecodes.ABS_HAT0X, hat),
                    (ecodes.ABS_HAT0Y, hat),
                ],
            },
            name=XBOX_NAME,
            vendor=XBOX_VENDOR,
            product=XBOX_PRODUCT,
            version=XBOX_VERSION,
            bustype=0x0003,
        )
        exact = sdl_guid(XBOX_NAME, XBOX_VENDOR, XBOX_PRODUCT, XBOX_VERSION)
        zero = sdl_guid(XBOX_NAME, XBOX_VENDOR, XBOX_PRODUCT, XBOX_VERSION, crc=0)
        text = (
            f"{exact},Xbox 360 Controller,{XBOX_MAP}\n"
            f"{zero},Xbox 360 Controller,{XBOX_MAP}\n"
        )
        STATE.mkdir(parents=True, exist_ok=True)
        MAPPING.write_text(text)
        CONFIG_MAPPING.parent.mkdir(parents=True, exist_ok=True)
        CONFIG_MAPPING.write_text(text)
        READY.write_text(self.ui.device.path + "\n")
        log(f"virtual pad {self.ui.device.path} guid={exact}")

    def close_virtual(self) -> None:
        if self.ui is not None:
            self.ui.close()
            self.ui = None
        READY.unlink(missing_ok=True)

    def close_devices(self) -> None:
        for dev in self.devices.values():
            try:
                dev.close()
            except OSError:
                pass
        self.devices.clear()

    def refresh(self) -> None:
        pads, hhd_pad = scan()
        if hhd_pad and len(pads) < 2:
            if self.ui is not None:
                log("HHD is already publishing a full gamepad; releasing the bridge pad.")
            self.close_virtual()
            self.close_devices()
            return
        wanted = {d.path: d for d in pads}
        if set(wanted) == set(self.devices):
            for dev in wanted.values():
                dev.close()
            return
        self.close_devices()
        self.devices = wanted
        self.seen_abs.clear()
        if not self.devices:
            log("waiting for Legion Go controllers...")
            self.close_virtual()
            return
        self.open_virtual()
        names = ", ".join(f"{p} ({d.name})" for p, d in self.devices.items())
        log(f"reading {len(self.devices)} Legion pad(s): {names}")

    def sides(self) -> tuple[InputDevice | None, InputDevice | None]:
        devs = list(self.devices.values())
        if not devs:
            return None, None
        if len(devs) == 1:
            return devs[0], None
        devs.sort(key=lambda d: d.path)
        if self.mapping.get("swap_sides"):
            devs.reverse()
        return devs[0], devs[1]

    def emit(self) -> None:
        if self.ui is None:
            return
        for code, value in self.axes.items():
            self.ui.write(ecodes.EV_ABS, code, int(value))
        for code, value in self.buttons.items():
            self.ui.write(ecodes.EV_KEY, code, int(value))
        self.ui.syn()

    def apply_buttons(self, dev: InputDevice, table: dict, code: int, value: int) -> None:
        name = ecodes.BTN.get(code) or ecodes.KEY.get(code)
        if isinstance(name, list):
            name = name[0]
        target = table.get(name or "")
        if target not in VIRTUAL:
            return
        self.buttons[VIRTUAL[target]] = 1 if value else 0

    def trigger_of(self, dev: InputDevice, codes: tuple[int, ...]) -> int:
        best = 0
        caps = {code: info for code, info in dev.capabilities().get(ecodes.EV_ABS, []) if not isinstance(code, int) or True}
        # capabilities() without verbose returns codes, sometimes (code, AbsInfo)
        raw = dev.capabilities().get(ecodes.EV_ABS, [])
        info = {}
        for item in raw:
            if isinstance(item, tuple):
                info[item[0]] = item[1]
            else:
                info[item] = None
        for code in codes:
            if (dev.path, code) not in self.seen_abs:
                continue
            try:
                value = dev.absinfo(code).value
            except OSError:
                continue
            span = 255 if code in (ecodes.ABS_GAS, ecodes.ABS_BRAKE) else 4095
            if code in info and getattr(info[code], "max", None):
                span = max(1, info[code].max)
            best = max(best, scale_axis(value, span))
        return best

    def handle(self, dev: InputDevice, code: int, value: int, ev_type: int) -> None:
        if self.dump:
            log(f"{dev.path} type={ev_type} code={code} value={value}")
        if ev_type == ecodes.EV_ABS:
            self.seen_abs.add((dev.path, code))
        left, right = self.sides()
        if left is None:
            return
        deadzone = float(self.mapping.get("deadzone", 0.12))
        invert = bool(self.mapping.get("invert_y", True))
        single = right is None
        if ev_type == ecodes.EV_KEY:
            if dev.path == left.path:
                self.apply_buttons(dev, self.mapping.get("left_buttons", {}), code, value)
                if single:
                    self.apply_buttons(dev, self.mapping.get("right_buttons", {}), code, value)
            elif right is not None and dev.path == right.path:
                self.apply_buttons(dev, self.mapping.get("right_buttons", {}), code, value)
            return
        if ev_type != ecodes.EV_ABS:
            return
        if dev.path == left.path:
            if code == ecodes.ABS_X:
                self.axes[ecodes.ABS_X] = scale_stick(value, False, deadzone)
            elif code == ecodes.ABS_Y:
                self.axes[ecodes.ABS_Y] = scale_stick(value, invert, deadzone)
            elif code in (ecodes.ABS_HAT0X, ecodes.ABS_HAT0Y):
                self.axes[code] = value
            elif single and code == ecodes.ABS_RX:
                self.axes[ecodes.ABS_RX] = scale_stick(value, False, deadzone)
            elif single and code == ecodes.ABS_RY:
                self.axes[ecodes.ABS_RY] = scale_stick(value, invert, deadzone)
            elif single and code == ecodes.ABS_Z and ecodes.ABS_RX in {
                item[0] if isinstance(item, tuple) else item
                for item in dev.capabilities().get(ecodes.EV_ABS, [])
            }:
                self.axes[ecodes.ABS_Z] = scale_axis(value, 255 if value <= 255 else 4095)
            elif single and code in (ecodes.ABS_Z, ecodes.ABS_RZ) and ecodes.ABS_RX not in {
                item[0] if isinstance(item, tuple) else item
                for item in dev.capabilities().get(ecodes.EV_ABS, [])
            }:
                dest = ecodes.ABS_RX if code == ecodes.ABS_Z else ecodes.ABS_RY
                self.axes[dest] = scale_stick(value, invert if dest == ecodes.ABS_RY else False, deadzone)
        elif right is not None and dev.path == right.path:
            if code == ecodes.ABS_X:
                self.axes[ecodes.ABS_RX] = scale_stick(value, False, deadzone)
            elif code == ecodes.ABS_Y:
                self.axes[ecodes.ABS_RY] = scale_stick(value, invert, deadzone)
            elif code in (ecodes.ABS_HAT0X, ecodes.ABS_HAT0Y) and (left.path, ecodes.ABS_HAT0X) not in self.seen_abs:
                self.axes[code] = value
        left_now, right_now = self.sides()
        if left_now is not None:
            self.axes[ecodes.ABS_Z] = self.trigger_of(left_now, (ecodes.ABS_GAS, ecodes.ABS_BRAKE, ecodes.ABS_Z))
        if right_now is not None:
            self.axes[ecodes.ABS_RZ] = self.trigger_of(right_now, (ecodes.ABS_GAS, ecodes.ABS_BRAKE, ecodes.ABS_Z, ecodes.ABS_RZ))
        elif single and left_now is not None:
            self.axes[ecodes.ABS_RZ] = self.trigger_of(left_now, (ecodes.ABS_BRAKE, ecodes.ABS_RZ))

    def loop(self) -> None:
        next_scan = 0.0
        while self.running:
            now = time.monotonic()
            if now >= next_scan:
                self.refresh()
                next_scan = now + 1.0
            fds = [d.fd for d in self.devices.values()]
            readable, _, _ = select.select(fds, [], [], 0.4) if fds else ([], [], [])
            for dev in list(self.devices.values()):
                if dev.fd not in readable:
                    continue
                try:
                    events = list(dev.read())
                except OSError:
                    continue
                for event in events:
                    self.handle(dev, event.code, event.value, event.type)
            if self.ui is not None and (readable or not self.seen_abs):
                self.emit()

    def stop(self, *_args) -> None:
        self.running = False


def main() -> int:
    dump = "--dump" in sys.argv
    STATE.mkdir(parents=True, exist_ok=True)
    mode = hhd_mode()
    log(f"HHD state probe: {mode}. Firmware dual_dinput is merged here; xinput stays with HHD.")
    bridge = Bridge(load_map(), dump)
    signal.signal(signal.SIGTERM, bridge.stop)
    signal.signal(signal.SIGINT, bridge.stop)
    try:
        bridge.loop()
    finally:
        bridge.close_virtual()
        bridge.close_devices()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
