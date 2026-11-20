import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from typing import Any, ClassVar

import numpy as np

from web.simulation import PRESET_BY_NAME, create, initial_conditions

ROOT = Path(__file__).resolve().parents[1]
METHODS = {
    "leapfrog": "Midpoint (Leapfrog)",
    "hermite": "Hermite",
    "gas": "Gas Model",
    "euler": "Euler",
    "animate": "Animate Only",
}


class ClusterTest(unittest.TestCase):
    directory: ClassVar[str]

    @classmethod
    def setUpClass(cls) -> None:
        directory = tempfile.TemporaryDirectory()
        cls.addClassCleanup(directory.cleanup)
        cls.directory = directory.name
        subprocess.run(
            [
                "javac",
                "--release",
                "25",
                "-d",
                cls.directory,
                *sorted((ROOT / "astro/galaxy").glob("*.java")),
                *sorted((ROOT / "myutil").glob("*.java")),
                ROOT / "tests/JavaReference.java",
            ],
            check=True,
            timeout=60,
        )

    @classmethod
    def java(cls, *args: str) -> list[Any]:
        output = subprocess.check_output(
            [
                "java",
                "--add-opens",
                "java.base/java.lang=ALL-UNNAMED",
                "-Djava.awt.headless=true",
                "-cp",
                cls.directory,
                "astro.galaxy.JavaReference",
                *args,
            ],
            text=True,
            timeout=45,
        )
        result = json.loads(output.splitlines()[-1])
        assert isinstance(result, list)
        return result

    def compare(self, name: str, method: str, count: int | None = None) -> None:
        preset = PRESET_BY_NAME[name]
        count = preset["count"] if count is None else count
        model = create(
            json.dumps(
                {
                    "preset": name,
                    "method": method,
                    "dt": preset["dt"],
                    "rotation": preset["rotation"],
                    "count": count,
                }
            ),
        )
        before = 0
        for step, elapsed, angle, bodies in self.java(
            name, METHODS[method], str(count), "0", "1", "12", "32"
        ):
            if step > before:
                model.advance(step - before)
            indices = model.order
            body_data = np.asarray(bodies)
            actual = np.column_stack(
                (model.positions, model.velocities, model.acceleration, model.jerk)
            )
            np.testing.assert_allclose(
                actual[indices], body_data[:, 2:], rtol=3e-10, atol=3e-10
            )
            np.testing.assert_allclose(
                model.masses[indices], body_data[:, 0], rtol=2e-14
            )
            np.testing.assert_array_equal(
                np.asarray(model.colors)[indices], body_data[:, 1]
            )
            self.assertEqual(model.time, elapsed)
            self.assertEqual(model.angle, angle)
            before = step

    def test_all_implemented_modes_follow_java(self) -> None:
        for method in METHODS:
            with self.subTest(method=method):
                self.compare("Gaussian", method)

    def test_every_available_preset_follows_java(self) -> None:
        for name in PRESET_BY_NAME:
            if name != "Gaussian":
                with self.subTest(preset=name):
                    self.compare(name, "leapfrog")

    def test_saturn_massless_particles_and_hermite_follow_java(self) -> None:
        for count in (2, 18, 34):
            with self.subTest(count=count):
                self.compare("Saturn/Janus", "hermite", count)

    def test_uninitialized_desktop_scenarios_have_clear_errors(self) -> None:
        orbit = self.java("Saturn/Janus", "Animate Only", "1000", "0")[0]
        self.assertEqual(sum(body[1] == -1 for body in orbit[-1]), 6)
        with self.assertRaisesRegex(ValueError, "Choose 2 bodies"):
            initial_conditions("Saturn/Janus", 1000)

        unavailable = self.java("Epimetheus/Janus", "Animate Only", "3", "0")[0]
        self.assertTrue(all(body[0] == 0 for body in unavailable[-1]))
        with self.assertRaisesRegex(ValueError, "valid preset"):
            initial_conditions("Epimetheus/Janus", 3)
