package com.example.qe.ui.support;

import com.example.qe.config.TestConfig;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

/**
 * Browser lifecycle shared by every UI test class.
 * - One browser per test class: starting a browser is slow.
 * - A fresh BrowserContext per test: like a brand-new private window, so no test inherits
 *   another's login session or cookies, and the tests can run in any order.
 */
@Tag("ui")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UiTestBase {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    protected Page page;

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
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
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
