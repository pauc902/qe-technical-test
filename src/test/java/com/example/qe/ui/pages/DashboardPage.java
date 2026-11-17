package com.example.qe.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the page a user lands on after logging in, plus the user menu used to log out. */
public class DashboardPage {

    public static final String PATH = "/web/index.php/dashboard/index";

    private final Page page;

    public DashboardPage(Page page) {
        this.page = page;
    }

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard"));
    }

    /**
     * Found by CSS class, because OrangeHRM gives this menu no role or accessible name to search on.
     * Kept here so a markup change is a one-line fix.
     */
    public Locator userMenu() {
        return page.locator(".oxd-userdropdown-tab");
    }

    public LoginPage logout() {
        userMenu().click();
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Logout")).click();
        return new LoginPage(page);
    }
}
