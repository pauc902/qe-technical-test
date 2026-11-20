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
        // Deliberately NOT skipping the browser download here. Playwright sets itself up once per test run,
        // so if the API tests ran first with downloads skipped, the UI tests would then find no browser.
        playwright = Playwright.create();
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
