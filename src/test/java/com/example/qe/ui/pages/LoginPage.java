package com.example.qe.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the OrangeHRM login page: where things are, and what a user can do there.
 * It never asserts. The tests decide what should be true.
 * Elements are found the way a user finds them (placeholder, role, visible text), not by CSS classes.
 */
public class LoginPage {

    public static final String PATH = "/web/index.php/auth/login";

    private final Page page;
    private final Locator username;
    private final Locator password;
    private final Locator loginButton;

    public LoginPage(Page page) {
        this.page = page;
        this.username = page.getByPlaceholder("Username");
        this.password = page.getByPlaceholder("Password");
        this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
    }

    public LoginPage open() {
        page.navigate(PATH);
        return this;
    }

    /** Fills in and submits the form. A null value leaves that field empty, for the "missing field" cases. */
    public void submit(String user, String pass) {
        if (user != null) {
            username.fill(user);
        }
        if (pass != null) {
            password.fill(pass);
        }
        loginButton.click();
    }

    /** For tests where logging in is only a step on the way, not the thing being tested. */
    public DashboardPage loginAs(String user, String pass) {
        submit(user, pass);
        return new DashboardPage(page);
    }

    public Locator passwordField() {
        return password;
    }

    public Locator loginButton() {
        return loginButton;
    }

    /** The banner for a rejected login, e.g. "Invalid credentials". */
    public Locator errorAlert() {
        return page.getByRole(AriaRole.ALERT);
    }

    public Locator usernameValidation() {
        return validationFor(username);
    }

    public Locator passwordValidation() {
        return validationFor(password);
    }

    /**
     * Finds the "Required" message by its visible text. The field wrapper is used only to pick the right
     * field, because the page does not link each message to its input in a way we can search on.
     */
    private Locator validationFor(Locator field) {
        return page.locator(".oxd-input-group")
                .filter(new Locator.FilterOptions().setHas(field))
                .getByText("Required");
    }
}
