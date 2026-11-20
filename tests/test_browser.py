import threading
import unittest
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    expect,
    sync_playwright,
)

BUILD = Path(__file__).resolve().parents[1] / "build"


class BrowserTest(unittest.TestCase):
    server: ThreadingHTTPServer
    thread: threading.Thread
    playwright: Playwright
    browser: Browser
    context: BrowserContext
    page: Page

    @classmethod
    def setUpClass(cls) -> None:
        if not (BUILD / "site/index.html").is_file():
            raise RuntimeError("Build the site before running browser tests.")
        # Serve from a subdirectory to match a GitHub project Pages URL.
        cls.server = ThreadingHTTPServer(
            ("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=BUILD)
        )
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.addClassCleanup(cls.server.server_close)
        cls.addClassCleanup(cls.thread.join, 5)
        cls.addClassCleanup(cls.server.shutdown)
        cls.playwright = sync_playwright().start()
        cls.addClassCleanup(cls.playwright.stop)
        cls.browser = cls.playwright.chromium.launch()
        cls.addClassCleanup(cls.browser.close)

    def setUp(self) -> None:
        self.context = self.browser.new_context()
        self.addCleanup(self.context.close)
        self.page = self.context.new_page()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}/site/"

    def test_simulator_controls(self) -> None:
        self.page.goto(self.base_url)
        step = self.page.get_by_role("button", name="Step", exact=True)
        expect(step).to_be_enabled(timeout=45_000)
        chart = self.page.locator("#plot")
        count = self.page.get_by_label("Bodies", exact=True)
        preset = self.page.get_by_label("Instance")
        method = self.page.get_by_label("Integrator")

        expect(preset).to_have_value("Gaussian")
        expect(preset.locator("option")).to_have_count(5)
        expect(self.page.locator("#body-count")).to_have_text("100")
        before = chart.evaluate("(canvas) => canvas.toDataURL()")
        self.page.get_by_label("Inclination").press("End")
        self.assertNotEqual(chart.evaluate("(canvas) => canvas.toDataURL()"), before)
        step.click()
        expect(self.page.locator("#step-number")).to_have_text("1")
        expect(self.page.locator("#time")).to_have_text("0.0010000")

        method.select_option("gas")
        expect(step).to_be_enabled()
        step.click()
        expect(self.page.locator("#step-number")).to_have_text("1")

        method.select_option("hermite")
        expect(step).to_be_enabled()
        preset.select_option("Saturn/Janus")
        expect(self.page.locator("#body-count")).to_have_text("994")
        expect(self.page.get_by_label("Frame rotation")).to_have_value("3284.4")
        count.fill("1000")
        count.press("Tab")
        expect(self.page.get_by_role("alert")).to_contain_text("Choose 2 bodies")
        count.fill("34")
        count.press("Tab")
        expect(self.page.locator("#body-count")).to_have_text("34")
        step.click()
        expect(self.page.locator("#step-number")).to_have_text("1")

        preset.select_option("FigureEight3")
        expect(self.page.locator("#body-count")).to_have_text("3")
        expect(count).to_be_disabled()
        self.page.set_viewport_size({"width": 390, "height": 844})
        self.assertTrue(
            self.page.evaluate("document.documentElement.scrollWidth <= innerWidth")
        )

    def test_runtime_failure_shows_retry(self) -> None:
        self.page.route("**/vendor/pyodide.js", lambda route: route.abort())
        self.page.goto(self.base_url)
        expect(self.page.get_by_role("alert")).to_contain_text("Python could not load")
        expect(self.page.get_by_role("button", name="Reload")).to_be_visible()
