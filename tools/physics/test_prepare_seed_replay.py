import json
from pathlib import Path
import tempfile
import unittest

from prepare_seed_replay import prepare


class SeedReplayTest(unittest.TestCase):
    def test_copies_runtime_without_worlds_or_mutating_source(self):
        with tempfile.TemporaryDirectory() as root:
            root = Path(root)
            source = root / "run"
            source.mkdir()
            original = "level-seed=\nrandomTickSpeed=3\n"
            (source / "server.properties").write_text(original)
            for name in ("config", "world", "gametest-failed", "saves", "logs"):
                (source / name).mkdir()
                (source / name / "keep.txt").write_text(name)
            target = prepare(source, root / "replays", 3837692264114305011)
            self.assertEqual((target / "config/keep.txt").read_text(), "config")
            for name in ("world", "gametest-failed", "saves", "logs"):
                self.assertFalse((target / name).exists())
            self.assertEqual((source / "server.properties").read_text(), original)
            self.assertEqual((target / "server.properties").read_text(),
                             "level-seed=3837692264114305011\nrandomTickSpeed=3\n")
            self.assertFalse(json.loads((target / "seed-replay.json").read_text())["worldCopied"])
            self.assertNotEqual(target, prepare(source, root / "replays", 3837692264114305011))

    def test_seed_replacement_or_insertion(self):
        with tempfile.TemporaryDirectory() as root:
            root = Path(root)
            source = root / "run"
            source.mkdir()
            for text in ("motd=test\n", " level-seed : 123\nmotd=test\n", "level-seed=1\nlevel-seed=2\n"):
                with self.subTest(text=text):
                    (source / "server.properties").write_text(text)
                    target = prepare(source, root / "replays", -(2 ** 63))
                    result = (target / "server.properties").read_text()
                    self.assertIn(f"level-seed={-(2 ** 63)}\n", result)
                    self.assertNotIn("level-seed=1\n", result)
                    self.assertNotIn("level-seed=2\n", result)

    def test_invalid_seed_does_not_create_output(self):
        with tempfile.TemporaryDirectory() as root:
            for seed in (2 ** 63, -(2 ** 63) - 1):
                with self.assertRaises(ValueError):
                    prepare(root, Path(root) / "output", seed)
            self.assertFalse((Path(root) / "output").exists())


if __name__ == "__main__":
    unittest.main()
