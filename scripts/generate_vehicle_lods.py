#!/usr/bin/env python3
"""PJM: генератор lod1-моделей техники.

Для каждой машины из assets/superbwarfare/sbw/vehicles/*.json создаёт
models/bedrock/vehicle_lod/<name>.lod1.geo.json, выбрасывая мелкие кубы, и
дописывает LOD-запись в Models с LODDistance=32. Скрипт идемпотентен: если
сгенерированная нами lod1-запись уже есть, её геометрия просто пересобирается
из полной модели (запись в JSON не дублируется).

Принцип тот же, что у рукодельных LOD upstream (см. mi_28): та же текстура,
та же иерархия костей, меньше кубов. Кубы несут собственные UV, поэтому
удаление одних кубов не ломает развёртку других.

- Метрика куба — площадь его максимальной грани (px² текстуры): тонкие крупные
  листы брони остаются, мелкие детали (болты, ручки, приборы) уходят.
- ЗАЩИТА ФУНКЦИОНАЛЬНЫХ КОСТЕЙ:
  * per-bone (FUNC): track*/wheel*/flare*/shell*/dummy_*/…dogTag — их кубы не
    трогаем (иначе рвутся дорожки траков и ломаются функциональные кости);
  * ПОДДЕРЕВО (BARREL): стволы/пушки/дуло (barrel/cannon/gun/muzzle/paokou/…)
    и ВСЕ их дочерние кости сохраняются целиком — ствол собран из длинных
    тонких кубов с маленькой «максимальной гранью», метрика их выбрасывала,
    и у техники визуально пропадало дуло.
- Нижняя граница: минимум KEEP_FLOOR самых крупных кубов сохраняется, чтобы
  модели из множества мелких кубов не превращались в решето.

Только тяжёлые модели (>= MIN_CUBES кубов). Рукодельные upstream-LOD
(ah_6/bmp_2/type_63/lav_150/laser_tower/plz_05/mi_28) не трогаются — их
lod-запись указывает не на наш `<name>.lod1.geo.json`.

Запуск из корня репозитория: python3 scripts/generate_vehicle_lods.py
"""

import glob
import json
import os
import re

ASSETS = 'src/main/resources/assets/superbwarfare'
FUNC = re.compile(r'^(track|wheel|flare|.*dogTag|dummy_|shell)', re.I)
# Кости ствола/пушки: сама кость и всё её поддерево сохраняются целиком.
BARREL = re.compile(r'barrel|cannon|muzzle|gun|paokou|paozuo|firePort|管|口', re.I)
THR = 32.0        # px² минимальной «максимальной грани» куба
KEEP_FLOOR = 0.45  # держим минимум 45% кубов
MIN_CUBES = 300   # генерируем только для тяжёлых моделей

# Рукодельные upstream-LOD (у них своя, вручную выверенная lod1/lod2/lod3) —
# наш скрипт их не пересобирает, даже если запись указывает на .lod1.geo.json.
HANDMADE = {'ah_6', 'bmp_2', 'type_63', 'lav_150', 'laser_tower', 'plz_05', 'mi_28'}


def face_area(c):
    sx, sy, sz = (abs(v) for v in c.get('size', [0, 0, 0]))
    inf = c.get('inflate', 0) or 0
    sx, sy, sz = sx + 2 * inf, sy + 2 * inf, sz + 2 * inf
    return max(sx * sy, sy * sz, sx * sz)


def resource_to_path(res):
    return os.path.join(ASSETS, res.split(':', 1)[1])


def protected_subtree(bones):
    """Имена костей, попадающих в поддерево какой-либо BARREL-кости."""
    by_name = {b.get('name', ''): b for b in bones}
    roots = {b.get('name', '') for b in bones if BARREL.search(b.get('name', ''))}
    protected = set()
    for b in bones:
        name = b.get('name', '')
        # идём вверх по цепочке parent; если встретили BARREL-корень — защищаем
        cur, guard = name, 0
        while cur and guard < 256:
            if cur in roots:
                protected.add(name)
                break
            cur = by_name.get(cur, {}).get('parent', '')
            guard += 1
    return protected


def build_lod(geo):
    """Мутирует geo, выкидывая мелкие кубы. Возвращает (всего, оставлено)."""
    bones = geo['minecraft:geometry'][0].get('bones', [])
    all_cubes = [c for b in bones for c in b.get('cubes', [])]
    if len(all_cubes) < MIN_CUBES:
        return None

    areas = sorted((face_area(c) for c in all_cubes), reverse=True)
    floor_thr = areas[max(0, int(len(areas) * KEEP_FLOOR) - 1)]
    thr = min(THR, floor_thr)

    barrel_bones = protected_subtree(bones)
    kept_total = 0
    for b in bones:
        cubes = b.get('cubes')
        if not cubes:
            continue
        name = b.get('name', '')
        if FUNC.match(name) or name in barrel_bones:
            kept_total += len(cubes)
            continue
        kept = [c for c in cubes if face_area(c) >= thr]
        kept_total += len(kept)
        if kept:
            b['cubes'] = kept
        else:
            del b['cubes']
    return len(all_cubes), kept_total, round(thr, 1)


def main():
    generated = []
    for vj_path in sorted(glob.glob(f'{ASSETS}/sbw/vehicles/*.json')):
        vj = json.load(open(vj_path))
        models = vj.get('Models')
        if not models:
            continue
        full = models[0]
        model_res = full.get('Model', '')
        if '/vehicle/' not in model_res:
            continue
        geo_path = resource_to_path(model_res)
        if not os.path.exists(geo_path):
            continue
        name = os.path.basename(geo_path).replace('.geo.json', '')
        if name in HANDMADE:
            continue
        lod_res = f'superbwarfare:models/bedrock/vehicle_lod/{name}.lod1.geo.json'

        # уже есть наша запись? — регенерируем на месте; чужая (>1 записи, но не
        # наша) — рукодельный LOD, не трогаем
        our_entry = next((m for m in models if m.get('Model') == lod_res), None)
        if our_entry is None and len(models) != 1:
            continue

        geo = json.load(open(geo_path))
        res = build_lod(geo)
        if res is None:
            continue
        total, kept, thr = res

        with open(resource_to_path(lod_res), 'w') as f:
            json.dump(geo, f, ensure_ascii=False, separators=(',', ':'))

        if our_entry is None:
            entry = {'Model': lod_res, 'Texture': full.get('Texture'), 'LODDistance': 32}
            if full.get('EmissiveTexture'):
                entry['EmissiveTexture'] = full['EmissiveTexture']
            models.append(entry)
            with open(vj_path, 'w') as f:
                json.dump(vj, f, ensure_ascii=False, indent=2)
                f.write('\n')

        generated.append((name, total, kept, thr))

    for name, total, kept, thr in generated:
        print(f'{name:28s} {total:5d} -> {kept:5d} ({100 * kept // total}%) thr={thr}')
    print(f'\nИтого сгенерировано: {len(generated)}')


if __name__ == '__main__':
    main()
