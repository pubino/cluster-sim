"""Initial conditions and integrators from Robert Vanderbei's Star Cluster app."""

import json
from dataclasses import dataclass
from math import cos, exp, log, pi, sin, sqrt
from typing import TypedDict, cast

import numpy as np
from numpy.typing import NDArray

G = 1.177e-4
INITIAL_RADIUS = (
    0.01  # Data3d's initial-condition scale. Cluster sets the force floor to zero.
)
METHODS = ("leapfrog", "hermite", "gas", "euler", "animate")
Array = NDArray[np.float64]
Indices = NDArray[np.intp]


class Preset(TypedDict):
    name: str
    count: int
    maxCount: int
    dt: float
    warp: int
    rotation: float


PRESETS: tuple[Preset, ...] = (
    dict(name="LagrangeN", count=100, maxCount=400, dt=0.001, warp=5, rotation=0),
    dict(name="FigureEight3", count=3, maxCount=3, dt=0.001, warp=5, rotation=0),
    dict(name="Gaussian", count=100, maxCount=400, dt=0.001, warp=5, rotation=0),
    dict(name="Random", count=100, maxCount=400, dt=0.001, warp=5, rotation=0),
    dict(
        name="Saturn/Janus",
        count=994,
        maxCount=2000,
        dt=0.00001,
        warp=500,
        rotation=3284.4,
    ),
)
PRESET_BY_NAME = {preset["name"]: preset for preset in PRESETS}


class JavaRandom:
    def __init__(self, seed: int) -> None:
        self.state = seed ^ 0x5DEECE66D

    def bits(self, width: int) -> int:
        self.state = (self.state * 0x5DEECE66D + 11) & ((1 << 48) - 1)
        return self.state >> (48 - width)

    def random(self) -> float:
        return ((self.bits(26) << 27) + self.bits(27)) / (1 << 53)

    def color(self) -> int:
        return rgb(*(0.5 + self.random() / 2 for _ in range(3)))


def rgb(red: float, green: float, blue: float) -> int:
    packed = 0
    for value in (red, green, blue):
        channel = int(np.float32(value) * np.float32(255) + np.float32(0.5))
        packed = (packed << 8) | channel
    return packed


def squared(vectors: Array) -> Array:
    x, y, z = np.moveaxis(vectors, -1, 0)
    return cast(Array, x * x + y * y + z * z)


def spherical(
    radius: float, azimuth: float, polar: float
) -> tuple[float, float, float]:
    return (
        radius * cos(azimuth) * sin(polar),
        radius * sin(azimuth) * sin(polar),
        radius * cos(polar),
    )


@dataclass
class BodySet:
    positions: Array
    velocities: Array
    masses: Array
    colors: list[int]


def initial_conditions(name: str, count: int, seed: int = 524) -> BodySet:
    preset = PRESET_BY_NAME.get(name)
    if preset is None:
        raise ValueError("Choose a valid preset.")
    if type(count) is not int or not 2 <= count <= preset["maxCount"]:
        raise ValueError(f"Use 2 to {preset['maxCount']} bodies for this preset.")
    if name == "FigureEight3" and count != 3:
        raise ValueError("FigureEight3 uses three bodies.")
    if name == "Saturn/Janus" and count != 2 and (count < 16 or count % 16 > 2):
        raise ValueError(
            "Choose 2 bodies or a count in groups like 16–18, 32–34, or 992–994."
        )
    if type(seed) is not int or not 0 <= seed <= 2**32 - 1:
        raise ValueError("The seed must be an unsigned 32-bit integer.")

    rng = JavaRandom(seed)
    positions = np.zeros((count, 3))
    velocities = np.zeros_like(positions)
    masses = np.ones(count)
    if name == "Saturn/Janus":
        masses[:] = 0
        masses[:2] = [95.2, 3.366e-7]
        radius = 0.00101251
        speed = sqrt(G * masses[0] / radius)
        positions[1, 0] = radius
        velocities[1, 1] = speed
        colors = [rgb(1, 1, 0.3), rgb(0.3, 1, 0.3)]
        angle = pi / 3
        for index in range(2, count):
            group, ray = divmod(index - 2, 16)
            phase = (2 * pi * ray) / 16
            extra_speed = 0.001 * speed * (group + 1) * 16 / count
            positions[index] = (
                radius * cos(angle),
                radius * sin(angle),
                0.000001 * (rng.random() - 0.5) * radius,
            )
            velocities[index] = (
                -speed * sin(angle) - extra_speed * sin(angle + phase),
                speed * cos(angle) + extra_speed * cos(angle + phase),
                0,
            )
            blue = group * 16.0 / count
            peak = max(blue, 1 - blue)
            colors.append(rgb((1 - blue) / peak, 0, blue / peak))
    else:
        escape_speed = sqrt(3 * G * (count - 1) / INITIAL_RADIUS)
        if name == "FigureEight3":
            masses[:] = 1 / G
            positions[:, :2] = [
                [-0.0, 0.0],
                [0.965502, -0.241961],
                [-0.965502, 0.241961],
            ]
            positions[:, 2] = [0.01 * rng.random() for _ in range(count)]
            velocities[:, :2] = [
                [0.933232, 0.865461],
                [-0.466616, -0.432730],
                [-0.466616, -0.432730],
            ]
        elif name == "LagrangeN":
            speed = 0.05 * escape_speed
            for index in range(count):
                angle = (24.0 * index / count) * 2 * pi / 24
                positions[index] = (
                    0.1 * cos(angle),
                    0.1 * sin(angle),
                    1e-9 * rng.random(),
                )
                velocities[index] = (-speed * sin(angle), speed * cos(angle), 0)
        elif name == "Gaussian":
            speed = 0.01 * escape_speed
            for index in range(count):
                radius, angle = sqrt(-2 * log(rng.random())), 2 * pi * rng.random()
                positions[index, :2] = (radius * cos(angle), radius * sin(angle))
                radius, angle = sqrt(-2 * log(rng.random())), 2 * pi * rng.random()
                positions[index, 2] = radius * cos(angle)
                velocities[index, 0] = speed * sin(angle)
                rng.random()  # The final Gaussian radius is drawn but unused in Data3d.
                angle = 2 * pi * rng.random()
                velocities[index, 1:] = (speed * sin(angle), speed * cos(angle))
        else:
            for index in range(count):
                for coordinates in (positions, velocities):
                    radius = -0.10 * log(rng.random())
                    coordinates[index] = spherical(
                        radius, 2 * pi * rng.random(), pi * rng.random()
                    )

        colors = []
        for index in range(count):
            if name in ("Gaussian", "Random"):
                masses[index] = exp(3 * (rng.random() - 0.5))
            colors.append(rng.color())

    for component in range(3):
        momentum = 0.0
        for index in range(1, count):
            momentum += float(masses[index] * velocities[index, component])
        velocities[0, component] = -momentum / masses[0]
    return BodySet(positions, velocities, masses, colors)


def sort_by_radius(order: Indices, radius: Array) -> None:
    """Mirror the canvas partition and pivot swaps, including ties."""
    pending = [(0, len(order) - 1)]
    while pending:
        left, right = pending.pop()
        if left >= right:
            continue
        middle = (left + right) // 2
        order[left], order[middle] = order[middle], order[left]
        last = left
        for index in range(left + 1, right + 1):
            if radius[order[index]] < radius[order[left]]:
                last += 1
                order[last], order[index] = order[index], order[last]
        order[left], order[last] = order[last], order[left]
        pending.extend(((last + 1, right), (left, last - 1)))


class Simulation:
    def __init__(
        self, bodies: BodySet, method: str, dt: float, rotation: float
    ) -> None:
        self.positions = bodies.positions
        self.velocities = bodies.velocities
        self.masses = bodies.masses
        self.colors = bodies.colors
        self.sources = int(np.flatnonzero(self.masses)[-1]) + 1
        self.order = np.arange(len(self.masses))
        self.method = method
        self.dt, self.rotation = dt, rotation
        self.steps = 0
        self.time = self.angle = 0.0
        self.acceleration = np.zeros_like(self.positions)
        self.jerk = np.zeros_like(self.positions)
        if method == "leapfrog":
            self.acceleration, _ = self.force(self.positions)
            self.velocities += self.acceleration * dt / 2
        elif method == "hermite":
            self.acceleration, self.jerk = self.force(
                self.positions, self.velocities, initial=True
            )

    def force(
        self, x: Array, v: Array | None = None, *, initial: bool = False
    ) -> tuple[Array, Array]:
        delta = x[None, : self.sources, :] - x[:, None, :]
        r2 = squared(delta)
        np.fill_diagonal(r2, np.inf)
        if np.any(r2 == 0):
            raise ValueError("Two bodies have zero separation. Reset to continue.")
        distance = np.sqrt(r2)
        cube = r2 * distance if v is not None else distance * distance * distance
        strength = G * self.masses[None, : self.sources, None]
        acceleration = np.cumsum(strength * delta / cube[..., None], axis=1)[:, -1, :]
        jerk = np.zeros_like(x)
        if v is not None:
            # The Java initialization includes the radial term. The refresh
            # only includes it below the force floor, which is zero in Cluster.
            change = v[None, : self.sources, :] - v[:, None, :]
            if initial:
                dot = np.sum(delta * change, axis=2)
                change -= 3 * (dot / r2)[..., None] * delta
            jerk = np.cumsum(strength * change / cube[..., None], axis=1)[:, -1, :]
        return acceleration, jerk

    def hermite(self, x: Array, v: Array) -> tuple[Array, Array, Array, Array]:
        dt, a0, j0 = self.dt, self.acceleration, self.jerk
        predicted_x = x + (v * dt + a0 * dt * dt / 2 + j0 * dt * dt * dt / 6)
        predicted_v = v + (a0 * dt + j0 * dt * dt / 2)
        a1, j1 = self.force(predicted_x, predicted_v)
        velocity = v + (a0 + a1) * dt / 2 + (j0 - j1) * dt * dt / 12
        position = x + (v + velocity) * dt / 2 + (a0 - a1) * dt * dt / 12
        return position, velocity, a1, j1

    def gas_acceleration(self, x: Array) -> Array:
        # ClusterCanvas accumulates the centroid without assigning it to its
        # center body. The gas shells in the running Java model use the origin.
        radius = np.sqrt(squared(x))
        if np.any(radius == 0):
            raise ValueError("A gas shell reached the origin. Reset to continue.")
        sort_by_radius(self.order, radius)
        enclosed = np.concatenate(([0.0], np.cumsum(self.masses[self.order])[:-1]))
        ordered = x[self.order]
        r = radius[self.order]
        acceleration = np.empty_like(x)
        acceleration[self.order] = (
            -G * enclosed[:, None] * ordered / (r * r * r)[:, None]
        )
        return acceleration

    def advance(self, steps: int) -> None:
        if type(steps) is not int or not 1 <= steps <= 500:
            raise ValueError("Use 1 to 500 steps per frame.")
        try:
            with np.errstate(over="raise", invalid="raise", divide="raise"):
                for _ in range(steps):
                    x, v, dt = self.positions, self.velocities, self.dt
                    if self.method == "hermite":
                        x, v, self.acceleration, self.jerk = self.hermite(x, v)
                    elif self.method != "animate":
                        if self.method == "euler":
                            self.acceleration, _ = self.force(x)
                        x = x + v * dt
                        if self.method == "leapfrog":
                            self.acceleration, _ = self.force(x)
                        elif self.method == "gas":
                            self.acceleration = self.gas_acceleration(x)
                        v = v + self.acceleration * dt

                    if not np.isfinite(x).all() or not np.isfinite(v).all():
                        raise FloatingPointError("Non-finite position or velocity.")
                    self.positions, self.velocities = x, v
                    self.steps += 1
                    self.time += dt
                    self.angle += dt * self.rotation
        except FloatingPointError as error:
            raise ValueError("The integrator exceeded its numerical range.") from error

    def frame(self) -> tuple[int, float, float, list[list[float]]]:
        return (
            self.steps,
            self.time,
            self.angle,
            cast(list[list[float]], self.positions.tolist()),
        )

    def reference(self) -> list[int]:
        return self.colors

    def set_timing(self, dt: float, rotation: float) -> None:
        step = real(dt, 1e-8, 0.2, "Time step")
        rate = real(rotation, -10000, 10000, "Frame rotation")
        self.dt, self.rotation = step, rate


def real(value: object, low: float, high: float, name: str) -> float:
    if (
        isinstance(value, bool)
        or not isinstance(value, (float, int))
        or not low <= value <= high
    ):
        raise ValueError(f"{name} must be a number from {low:g} to {high:g}.")
    return float(value)


def catalog() -> str:
    return json.dumps(PRESETS)


def create(parameters: str) -> Simulation:
    options = json.loads(parameters)
    if not isinstance(options, dict):
        raise ValueError("Choose an integrator and simulation parameters.")
    name, count, method = (
        options.get("preset"),
        options.get("count"),
        options.get("method"),
    )
    if not isinstance(name, str) or type(count) is not int:
        raise ValueError("Choose a preset and a whole number of bodies.")
    if method not in METHODS:
        raise ValueError("Choose a supported integrator.")
    dt = real(options.get("dt"), 1e-8, 0.2, "Time step")
    rotation = real(options.get("rotation"), -10000, 10000, "Frame rotation")
    return Simulation(initial_conditions(name, count), method, dt, rotation)
