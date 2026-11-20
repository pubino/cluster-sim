const scope = /** @type {import("./protocol").WorkerScope} */ (/** @type {unknown} */ (self));

/** @param {string} file */
async function read(file) {
  const response = await fetch(new URL(file, scope.location.href));
  if (!response.ok) throw new Error(`Could not load ${file}.`);
  return response.text();
}

async function initialize() {
  const vendor = new URL("./vendor/", scope.location.href).href;
  scope.importScripts(new URL("pyodide.js", vendor).href);
  const [python, model] = await Promise.all([
    scope.loadPyodide({ indexURL: vendor }), read("simulation.py"),
  ]);
  await python.loadPackage("numpy");
  python.FS.writeFile("/home/pyodide/simulation.py", model);
  const data = python.runPython("from simulation import catalog, create\ncatalog()");
  if (typeof data !== "string") throw new Error("Could not load presets.");
  /** @type {import("./protocol").PresetMetadata[]} */
  const presets = JSON.parse(data);
  const create = /** @type {(parameters: string) => import("./protocol").SimulationProxy} */ (
    python.runPython("create")
  );
  return { create, presets };
}

/** @template T @param {import("./protocol").Proxy<T>} proxy */
function convert(proxy) {
  try {
    return proxy.toJs({ create_pyproxies: false });
  } finally {
    proxy.destroy();
  }
}

initialize().then(({ create, presets }) => {
  /** @type {import("./protocol").SimulationProxy | undefined} */
  let simulation;
  scope.onmessage = ({ data }) => {
    try {
      if (data.type === "reset") {
        const next = create(JSON.stringify(data.parameters));
        simulation?.destroy();
        simulation = next;
        scope.postMessage({
          type: "reset", id: data.id,
          frame: convert(simulation.frame()), reference: convert(simulation.reference()),
        });
        return;
      }
      if (!simulation) throw new Error("Select an instance.");
      simulation.set_timing(data.dt, data.rotation);
      simulation.advance(data.steps);
      scope.postMessage({ type: "advance", id: data.id, frame: convert(simulation.frame()) });
    } catch (error) {
      const message = String(error).match(/ValueError: ([^\r\n]+)/)?.[1];
      scope.postMessage({ type: "failed", id: data.id, message: message ?? "The simulation stopped unexpectedly." });
    }
  };
  scope.postMessage({ type: "ready", presets });
}, () => scope.postMessage({ type: "fatal" }));
