#!/usr/bin/env python3
"""PJM: генератор lod1-моделей техники.

Для каждой машины из assets/superbwarfare/sbw/vehicles/*.json, у которой ровно
одна запись в Models (то есть LOD ещё нет) и в полной модели >= MIN_CUBES кубов,
создаёт models/bedrock/vehicle_lod/<name>.lod1.geo.json, выбрасывая мелкие кубы,
и дописывает LOD-запись в Models с LODDistance=32.

Принцип тот же, что у рукодельных LOD upstream (см. mi_28): та же текстура,
та же иерархия костей, меньше кубов. Кубы несут собственные UV, поэтому
удаление одних кубов не ломает развёртку других.

- Метрика куба — площадь его максимальной грани (px² текстуры): тонкие крупные
  листы брони остаются, мелкие детали (болты, ручки, приборы) уходят.
- Кости track*/wheel*/flare*/shell*/dummy_*/…dogTag защищены: их кубы не
  выбрасываются (иначе рвутся дорожки траков и ломаются функциональные кости).
- Нижняя граница: минимум KEEP_FLOOR самых крупных кубов сохраняется, чтобы
  модели из множества мелких кубов не превращались в решето.

Запуск из корня репозитория: python3 scripts/generate_vehicle_lods.py
Уже существующие LOD (len(Models) > 1) не перезаписываются.
"""

import glob
import json
import os
import re

ASSETS = 'src/main/resources/assets/superbwarfare'
FUNC = re.compile(r'^(track|wheel|flare|.*dogTag|dummy_|shell)', re.I)
THR = 32.0        # px² минимальной «максимальной грани» куба
KEEP_FLOOR = 0.45  # держим минимум 45% кубов
MIN_CUBES = 300   # генерируем только для тяжёлых моделей


def face_area(c):
    sx, sy, sz = (abs(v) for v in c.get('size', [0, 0, 0]))
    inf = c.get('inflate', 0) or 0
    sx, sy, sz = sx + 2 * inf, sy + 2 * inf, sz + 2 * inf
    return max(sx * sy, sy * sz, sx * sz)


def resource_to_path(res):
    return os.path.join(ASSETS, res.split(':', 1)[1])


def main():
    generated = []
    for vj_path in sorted(glob.glob(f'{ASSETS}/sbw/vehicles/*.json')):
        vj = json.load(open(vj_path))
        models = vj.get('Models')
        if not models or len(models) != 1:
            continue  # нет Models или LOD уже есть
        model_res = models[0].get('Model', '')
        if '/vehicle/' not in model_res:
            continue
        geo_path = resource_to_path(model_res)
        if not os.path.exists(geo_path):
            continue
        geo = json.load(open(geo_path))
        bones = geo['minecraft:geometry'][0].get('bones', [])
        all_cubes = [(b, c) for b in bones for c in b.get('cubes', [])]
        if len(all_cubes) < MIN_CUBES:
            continue

        # порог с нижней границей: минимум KEEP_FLOOR самых крупных кубов остаются
        areas = sorted((face_area(c) for _, c in all_cubes), reverse=True)
        floor_thr = areas[max(0, int(len(areas) * KEEP_FLOOR) - 1)]
        thr = min(THR, floor_thr)

        kept_total = 0
        for b in bones:
            cubes = b.get('cubes')
            if not cubes:
                continue
            if FUNC.match(b.get('name', '')):
                kept_total += len(cubes)
                continue
            kept = [c for c in cubes if face_area(c) >= thr]
            kept_total += len(kept)
            if kept:
                b['cubes'] = kept
            else:
                del b['cubes']

        name = os.path.basename(geo_path).replace('.geo.json', '')
        lod_res = f'superbwarfare:models/bedrock/vehicle_lod/{name}.lod1.geo.json'
        with open(resource_to_path(lod_res), 'w') as f:
            json.dump(geo, f, ensure_ascii=False, separators=(',', ':'))

        entry = {'Model': lod_res, 'Texture': models[0].get('Texture'), 'LODDistance': 32}
        if models[0].get('EmissiveTexture'):
            entry['EmissiveTexture'] = models[0]['EmissiveTexture']
        models.append(entry)
        with open(vj_path, 'w') as f:
            json.dump(vj, f, ensure_ascii=False, indent=2)
            f.write('\n')

        generated.append((name, len(all_cubes), kept_total, round(thr, 1)))

    for name, total, kept, thr in generated:
        print(f'{name:28s} {total:5d} -> {kept:5d} ({100 * kept // total}%) thr={thr}')
    print(f'\nИтого сгенерировано: {len(generated)}')


if __name__ == '__main__':
    main()
