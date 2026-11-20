package com.example.qe.api.tests;

import com.example.qe.config.TestConfig;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

/**
 * Sets up and tears down the Playwright connection used by every API test class.
 * The Petstore address comes from the petstore.baseUrl setting (see config.properties).
 */
@Tag("api")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    private Playwright playwright;
    protected APIRequestContext request;

    @BeforeAll
    void openConnection() {
        // API tests never open a browser, so skip Playwright's ~500 MB browser download.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(TestConfig.get("petstore.baseUrl"))
                .setExtraHTTPHeaders(Map.of("Accept", "application/json")));
    }

    @AfterAll
    void closeConnection() {
        request.dispose();
        playwright.close();
    }
}
