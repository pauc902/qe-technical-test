package com.example.qe.ui.support;

import com.example.qe.config.TestConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Browser lifecycle shared by every UI test class.
 * - One browser per test class: starting a browser is slow.
 * - A fresh BrowserContext per test: like a brand-new private window, so no test inherits
 *   another's login session or cookies, and the tests can run in any order.
 * - Every test is recorded with Playwright tracing. The recording and a screenshot are kept only when the
 *   test FAILS, in target/playwright-artifacts, so a failure on a build server can be investigated
 *   without re-running it. Open a trace by dropping the .zip onto https://trace.playwright.dev
 *   (it is opened inside your browser, not uploaded).
 */
@Tag("ui")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UiTestBase {

    private static final Path ARTIFACTS = Paths.get("target", "playwright-artifacts");

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private boolean traceSaved;
    protected Page page;

    // JUnit calls this right after each test and before @AfterEach, while the page is still open.
    @RegisterExtension
    final AfterTestExecutionCallback saveEvidenceOnFailure = this::saveEvidenceIfFailed;

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = browserType().launch(new BrowserType.LaunchOptions()
                .setHeadless(TestConfig.getBoolean("ui.headless"))
                .setSlowMo(TestConfig.getInt("ui.slowMoMs")));
    }

    @BeforeEach
    void openNewContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(TestConfig.get("orangehrm.baseUrl"))
                .setViewportSize(1366, 768));
        int timeout = TestConfig.getInt("ui.timeoutMs");
        context.setDefaultTimeout(timeout);
        context.setDefaultNavigationTimeout(timeout * 2.0);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        traceSaved = false;
        page = context.newPage();
    }

    private void saveEvidenceIfFailed(ExtensionContext test) {
        if (test.getExecutionException().isEmpty()) {
            return;
        }
        String name = (test.getRequiredTestClass().getSimpleName() + "_" + test.getDisplayName())
                .replaceAll("[^A-Za-z0-9._-]+", "_");
        page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(ARTIFACTS.resolve(name + ".png")));
        context.tracing().stop(new Tracing.StopOptions().setPath(ARTIFACTS.resolve(name + "-trace.zip")));
        traceSaved = true;
        System.err.println("UI failure evidence saved to " + ARTIFACTS.toAbsolutePath() + " (" + name + ")");
    }

    @AfterEach
    void closeContext() {
        if (!traceSaved) {
            context.tracing().stop(); // the test passed: throw the recording away
        }
        context.close();
    }

    @AfterAll
    void closeBrowser() {
        browser.close();
        playwright.close();
    }

    private BrowserType browserType() {
        return switch (TestConfig.get("ui.browser").toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }
}
