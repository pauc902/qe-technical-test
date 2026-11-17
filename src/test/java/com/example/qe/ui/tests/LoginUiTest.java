package com.example.qe.ui.tests;

import com.example.qe.config.TestConfig;
import com.example.qe.ui.pages.DashboardPage;
import com.example.qe.ui.pages.LoginPage;
import com.example.qe.ui.support.UiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Feature under test: OrangeHRM login. Everything else in the product sits behind it, so if login
 * breaks nothing else can be reached.
 * Playwright's assertThat(...) waits and retries until the check passes or times out, so there are no sleeps.
 */
@DisplayName("UI: OrangeHRM login")
class LoginUiTest extends UiTestBase {

    private static final Pattern LOGIN_URL = Pattern.compile(".*/auth/login$");
    private static final Pattern DASHBOARD_URL = Pattern.compile(".*/dashboard/index$");

    private LoginPage loginPage;

    @BeforeEach
    void openLoginPage() {
        loginPage = new LoginPage(page).open();
        assertThat(loginPage.loginButton()).isVisible();
    }

    @Test
    @DisplayName("valid credentials land on the dashboard; logging out ends the session")
    void validLoginThenLogout() {
        DashboardPage dashboard = loginPage.loginAs(
                TestConfig.get("orangehrm.username"), TestConfig.get("orangehrm.password"));

        assertThat(page).hasURL(DASHBOARD_URL);
        assertThat(dashboard.heading()).isVisible();

        dashboard.logout();
        assertThat(page).hasURL(LOGIN_URL);

        // The session must really be gone: a direct link to a protected page goes back to login.
        page.navigate(DashboardPage.PATH);
        assertThat(page).hasURL(LOGIN_URL);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("rejected credentials show 'Invalid credentials' and stay on the login page")
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            wrong password                 | Admin           | not-the-password
            unknown username               | qe-no-such-user | admin123
            password is case-sensitive     | Admin           | ADMIN123
            SQL-injection style username   | ' OR '1'='1     | anything
            """)
    void invalidCredentialsAreRejected(String scenario, String user, String pass) {
        loginPage.submit(user, pass);

        assertThat(loginPage.errorAlert()).containsText("Invalid credentials");
        assertThat(page).hasURL(LOGIN_URL);
        // The message must not reveal WHICH of the two was wrong, or attackers could discover valid usernames.
        assertThat(loginPage.errorAlert()).not().containsText("username");
        assertThat(loginPage.errorAlert()).not().containsText("password");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("missing fields show 'Required' under exactly the empty fields")
    @CsvSource(delimiter = '|', nullValues = "<empty>", textBlock = """
            both fields empty | <empty> | <empty>  | true  | true
            password empty    | Admin   | <empty>  | false | true
            username empty    | <empty> | admin123 | true  | false
            """)
    void missingFieldsShowRequired(String scenario, String user, String pass,
                                   boolean usernameRequired, boolean passwordRequired) {
        loginPage.submit(user, pass);

        assertThat(loginPage.usernameValidation()).hasCount(usernameRequired ? 1 : 0);
        assertThat(loginPage.passwordValidation()).hasCount(passwordRequired ? 1 : 0);
        // The form is stopped in the browser, so no request is sent and no rejection banner appears.
        assertThat(loginPage.errorAlert()).hasCount(0);
        assertThat(page).hasURL(LOGIN_URL);
    }
}
