# QE technical test: Playwright Java tests for an API and a user interface

This project has automated tests for two public demo systems:

| Layer | System | What is tested | Test class |
|---|---|---|---|
| **API** (a program talking to a server) | [Swagger Petstore](https://petstore.swagger.io/) | Getting a pet by its id: `GET /pet/{petId}` | [`GetPetByIdApiTest`](src/test/java/com/example/qe/api/tests/GetPetByIdApiTest.java) |
| **UI** (a person using a web page) | [OrangeHRM demo](https://opensource-demo.orangehrmlive.com/) | Logging in and out | [`LoginUiTest`](src/test/java/com/example/qe/ui/tests/LoginUiTest.java) |

Each test covers a successful case and the ways it can go wrong, such as unknown ids, malformed input and
wrong passwords.

The reasoning behind the tests, and the further scenarios that would be automated next, are in
[docs/TEST_DESIGN.md](docs/TEST_DESIGN.md).

## What you need

- **Java 17 or later.** Check with `java -version`.
- **Maven 3.9 or later.** Maven is the tool that downloads the libraries, compiles the code and runs the
  tests. Check with `mvn -v`.
- **An internet connection.** Both systems under test are public websites. On a company network, the
  browser download may need your proxy setting (the `HTTPS_PROXY` environment variable).

Nothing else needs installing. The first time the UI tests run, Playwright downloads the browsers it
controls (about 500 MB, once). That first run takes a few minutes. After that, the whole suite takes about
a minute.

## Run the tests

```bash
mvn test
```

Other ways to run them:

```bash
mvn test -Dgroups=api          # API tests only (a few seconds)
mvn test -Dgroups=ui           # UI tests only
mvn test -Dgroups=negative     # only the tests for things going wrong (also: positive)
mvn test -Dgroups=ui -Dui.headless=false                      # watch the browser
mvn test -Dgroups=ui -Dui.headless=false -Dui.slowMoMs=500    # watch it slowed down, for a demonstration
```

"Headless" means the browser runs without a visible window.

**On Windows PowerShell,** put each `-D` option in quotes, because PowerShell can split an option at its
dots: `mvn test "-Dgroups=ui" "-Dui.headless=false"`. Plain `mvn test` needs no quotes, and the Command
Prompt (`cmd`) needs none either. The pipeline runs the tests on both Linux and Windows.

**One test is skipped on purpose.** It records a real defect in the Petstore, which is described in
[docs/TEST_DESIGN.md](docs/TEST_DESIGN.md#5-findings). A skipped test does not fail the build.

## Read the results

| Where | What you get |
|---|---|
| The terminal | A line per test class, then the totals |
| `target/test-summary.md` | One table: result, layer (API or UI), area of functionality, and whether the test is positive or negative. Open it in an editor with a Markdown preview. |
| **GitHub Actions** | The same table on the **Summary** page of each run. Open the repository's **Actions** tab and choose a run. |
| `target/playwright-artifacts/` | Only when a UI test fails: a full-page screenshot and a Playwright trace |

A trace is a recording of the failed test. To replay it, drop the `-trace.zip` file onto
<https://trace.playwright.dev>. It opens in your browser and is not uploaded.

The `target/` folder is created by each run and is not kept in git.

## Settings

Defaults are in [`src/test/resources/config.properties`](src/test/resources/config.properties). You can
change any of them for one run with `-Dname=value`, or with an environment variable (the name in capitals,
with dots changed to underscores, such as `ORANGEHRM_PASSWORD`).

| Setting | Default | Meaning |
|---|---|---|
| `orangehrm.baseUrl` | `https://opensource-demo.orangehrmlive.com` | Address of the UI system |
| `orangehrm.username`, `orangehrm.password` | `Admin`, `admin123` | The public demo login, which the site displays on its own login page |
| `petstore.baseUrl` | `https://petstore.swagger.io/v2/` | Address of the API |
| `ui.headless` | `true` | Run the browser without a visible window |
| `ui.browser` | `chromium` | `chromium`, `firefox` or `webkit` |
| `ui.timeoutMs` | `15000` | How long to wait for the page before failing |
| `ui.slowMoMs` | `0` | Pause between browser actions, in milliseconds |

The demo login is committed on purpose, because it is public. Real credentials must never be committed. They
would come from an environment variable or a secrets store.

## How the code is organised

```
src/test/java/com/example/qe/
├── api/
│   ├── client/      Sends the requests (PetApiClient) and holds the response (ApiResult). It never checks anything.
│   ├── model/       The shape of a pet and of an error, plus PetBuilder, which makes valid test data
│   └── tests/       The API test and its shared set-up
├── ui/
│   ├── pages/       One class per web page, holding where things are and what a user can do there
│   ├── support/     Opens and closes the browser for each test, and saves evidence when a test fails
│   └── tests/       The UI test
├── config/          Reads the settings above
└── reporting/       Builds the results table
```

API tests and UI tests are in separate folders, and each carries a tag (`api` or `ui`) so they can be run
separately.

## Add a test

1. Add a method to a test class, or create a new class that extends `ApiTestBase` or `UiTestBase`.
2. Tag it `@Tag("positive")` or `@Tag("negative")`, and label it with `@Area("...")`. These feed the results
   table.
3. Run `mvn test` and check that the new row appears in `target/test-summary.md`.

Each test creates the data it needs and deletes it afterwards, because both systems are shared with other
people.

## Pipeline

The tests run on every push in GitHub Actions
([`.github/workflows/tests.yml`](.github/workflows/tests.yml)). The workflow has read-only access to the
repository, uses only GitHub's own actions, and pins each one to a full commit ID.
