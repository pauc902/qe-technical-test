package com.example.qe.api.tests;

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
 * The base URL can be overridden on the command line: mvn test -Dpetstore.baseUrl=...
 */
@Tag("api")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    private static final String BASE_URL = System.getProperty("petstore.baseUrl", "https://petstore.swagger.io/v2/");

    private Playwright playwright;
    protected APIRequestContext request;

    @BeforeAll
    void openConnection() {
        // API tests never open a browser, so skip Playwright's ~500 MB browser download.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(BASE_URL)
                .setExtraHTTPHeaders(Map.of("Accept", "application/json")));
    }

    @AfterAll
    void closeConnection() {
        request.dispose();
        playwright.close();
    }
}
