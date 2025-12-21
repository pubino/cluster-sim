# Star Cluster Simulator

A Java-based N-body gravitational simulator by Professor Robert Vanderbei, Princeton University, for studying star cluster dynamics. This simulation demonstrates various configurations including Lagrange equilibria, figure-eight three-body orbits, Gaussian distributions, and Saturn's co-orbital moons.

## Overview

This simulation models:
- Globular and open star cluster dynamics
- Various N-body configurations (Lagrange, Figure-Eight, Random, Gaussian)
- Saturn's Epimetheus and Janus co-orbital moons
- Multiple numerical integrators (Midpoint/Leapfrog, Hermite, Euler, Runge-Kutta, Gas Model)
- Rotating reference frame visualization

## Running the Application

### Prerequisites

- Java 11 or later (tested with Java 25)

### Quick Start

From the `cluster-sim` directory:

```bash
# Build and run
make run

# Or build the macOS app and double-click it
make app
open dist/ClusterSimulator.app
```

### Usage

1. Select a scenario from the **Instance** dropdown:
   - **LagrangeN**: N-body Lagrange equilibrium configuration
   - **FigureEight3**: Famous figure-eight 3-body periodic orbit
   - **Gaussian**: Gaussian-distributed star cluster
   - **Random**: Randomly distributed cluster
   - **Epimetheus/Janus**: Saturn's co-orbital moons
   - **Saturn/Janus**: Saturn with Janus and ring particles

2. Select an integrator from the **Integrator** dropdown:
   - **Midpoint (Leapfrog)**: Symplectic integrator (default)
   - **Hermite**: Higher-order predictor-corrector
   - **Gas Model**: Spherically symmetric approximation
   - **Euler**: Simple first-order method
   - **RungeKutta4**: Fourth-order Runge-Kutta
   - **TrapNewt**: Trapezoidal-Newton method

3. Adjust parameters:
   - **FrameRotationRate**: Angular velocity of rotating frame
   - **dt**: Time step for numerical integration
   - **warp**: Speed multiplier (steps per display update)
   - **n**: Number of bodies

4. Click **Start** to begin the animation
5. Use **Pause/Play** to control the simulation
6. Use **+/-** buttons to zoom in/out
7. Toggle **Show/Hide Trails** to see orbital paths

## Development

### Project Structure

```
cluster-sim/
├── astro/galaxy/
│   ├── Cluster.java         # Main application (JApplet-based GUI)
│   ├── ClusterCanvas.java   # Animation canvas and physics engine
│   ├── Body3d.java          # 3D celestial body class
│   └── Data3d.java          # Initial conditions and ephemeris data
├── myutil/
│   ├── AppletFrame.java     # Frame wrapper for running applets standalone
│   ├── GL.java              # Graphics library for 2D drawing
│   ├── SL.java              # String/number parsing utilities
│   └── Format.java          # Number formatting utilities
├── scripts/
│   ├── build.sh             # Build JAR file
│   ├── create-app-bundle.sh # Create macOS .app bundle
│   ├── setup-notarization.sh# Setup Apple credentials
│   └── sign-and-notarize.sh # Sign and notarize for distribution
├── Makefile                 # Build automation
├── README.md
└── .gitignore
```

### Building with Make

```bash
make           # Build JAR file
make app       # Build macOS app bundle
make run       # Build and run
make clean     # Clean build artifacts
make distclean # Clean everything
```

## Scientific Background

### Star Clusters

Star clusters are gravitationally bound groups of stars that form from the same molecular cloud. This simulator models the N-body dynamics of such clusters using direct gravitational force calculations.

### Figure-Eight Orbit

The figure-eight three-body orbit is a remarkable periodic solution discovered by Moore (1993) where three equal-mass bodies chase each other along a figure-eight-shaped path. This is one of the few known periodic solutions to the three-body problem.

### Lagrange Configuration

The Lagrange equilibrium places N bodies at the vertices of a regular polygon, with the central body at rest. This generalizes the classical three-body triangular configuration.

### Numerical Integrators

- **Midpoint (Leapfrog)**: Second-order symplectic integrator - conserves energy well
- **Hermite**: Fourth-order predictor-corrector with jerk calculation
- **Gas Model**: Treats the cluster as a spherically symmetric mass distribution
- **Euler/RK4**: Standard ODE integrators for comparison

## History

This simulator was originally written as a Java Applet for web browsers. It has been updated to run as a standalone desktop application.
