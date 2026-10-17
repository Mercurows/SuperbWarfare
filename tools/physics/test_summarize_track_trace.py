#!/usr/bin/env python3
"""Run with: python3 -m unittest discover -s tools/physics -p 'test_*.py'."""
import contextlib
import io
import gzip
import math
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import summarize_track_trace


class SummaryTest(unittest.TestCase):
    def summarize(self, text, compressed=False):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / ("trace.log.gz" if compressed else "trace.log")
            if compressed:
                with gzip.open(log, "wt", encoding="utf-8") as stream:
                    stream.write(text)
            else:
                log.write_text(text, encoding="utf-8")
            out = io.StringIO()
            with patch.object(sys, "argv", ["summarize_track_trace.py", str(log)]):
                with contextlib.redirect_stdout(out):
                    summarize_track_trace.main()
            return out.getvalue()

    def test_truncated_and_empty_logs_are_not_passes(self):
        for text in ("", "8 GAME TESTS COMPLETE\n"):
            with self.subTest(text=text):
                self.assertIn("RESULT: NOT VALIDATED", self.summarize(text))

    def test_failure_is_preserved(self):
        result = self.summarize("8 GAME TESTS COMPLETE\n1 required tests failed :(\n")
        self.assertIn("RESULT: FAILED (1/8", result)
        self.assertNotIn("RESULT: PASSED", result)

    def test_zero_tests_is_not_a_pass(self):
        self.assertIn("RESULT: NOT VALIDATED", self.summarize(
            "0 GAME TESTS COMPLETE\nAll required tests passed :)\n"))

    def test_explicit_pass_keeps_roster_warning(self):
        result = self.summarize("8 GAME TESTS COMPLETE\nAll required tests passed :)\n")
        self.assertIn("RESULT: PASSED reported roster (8 completed)", result)
        self.assertIn("verify the expected namespace/count", result)

    def test_terrain_reason_and_loaded_contacts(self):
        result = self.summarize(
            "[vehicle-physics] ztz/id frame=2 reason=runtime-terrain-loss "
            "waiting=true terrainIssue=dirty:SectionPos{x=64, y=4, z=64} active=true "
            "pos=(1, 2, 3) localV=(0, 0, 0)\n"
            "[vehicle-physics] ztz/id frame=3 reason=step waiting=false terrainIssue=null active=true "
            "pos=(1, 2, 3) localV=(0, 0, 0) "
            "contacts=LEFT:1 hit=true travel=0 N=10 RIGHT:1 hit=false travel=-.24 N=0\n")
        self.assertIn("dirty:SectionPos{x=64, y=4, z=64}", result)
        self.assertIn("loaded=1", result)
        self.assertIn("RESULT: NOT VALIDATED", result)

    def test_gzip_environment_and_impulse_sources(self):
        result = self.summarize(
            "[vehicle-impulse] prism/id value=(1,0,0) source=Entity.updateFluidHeightAndDoFluidPushing\n"
            "[vehicle-impulse] prism/id value=(1,0,0) source=TrackContactGameTests.lambda\n"
            "[vehicle-impulse] prism/id value=(1,0,0) source=Unknown.push\n"
            "[track-test-environment] prism heading=90 fluids=27\n"
            "8 GAME TESTS COMPLETE\n2 required tests failed :(\n", compressed=True)
        self.assertIn("'fluid-pushing': 1", result)
        self.assertIn("'test-fixture': 1", result)
        self.assertIn("'other': 1", result)
        self.assertIn("fluids=27", result)
        self.assertIn("RESULT: FAILED (2/8", result)

    def test_world_horizontal_speed_is_not_local_xz(self):
        q = (math.sqrt(.5), 0, 0, math.sqrt(.5))
        world = summarize_track_trace.world_velocity((0, 1, 0), q)
        self.assertAlmostEqual(world[1], 0)
        self.assertAlmostEqual(world[2], 1)
        result = self.summarize(
            "[vehicle-physics] ztz/id frame=3 reason=step "
            "pos=(1, 2, 3) localV=(0, 1, 0) rotation=(0.7071067811865476, 0, 0, 0.7071067811865476)\n")
        self.assertIn("local_xz_speed=0", result)
        self.assertIn("world_horizontal_speed=1", result)
        self.assertIn("RESULT: NOT VALIDATED", result)


if __name__ == "__main__":
    unittest.main()
