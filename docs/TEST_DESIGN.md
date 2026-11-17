# Test Design Explanation

This document explains how the tests are designed and why. It covers the two tests that are built, the
further scenarios I would automate in priority order, and where each kind of check belongs.

## 1. Approach

I prioritised **speed and reliability**. Fast, dependable tests give the quickest signal that something has
gone wrong, so that it can be fixed straight away. I also tried to balance safety against speed by using
good feedback gates: quick checks first, slower checks only where they prove something the quick checks cannot.

Four principles follow from that:

- **Test as close to the application logic as possible** ("shift left"). In reality this is not always
  possible, so the UI is still used to prove that the screen calls the right function and responds correctly.
- **Each test has one intent.** If a test fails, the cause should be clear.
- **Each test owns its data.** Both systems are shared public demos, so tests create what they need and
  clean up after themselves.
- **Keep the UI thin.** Too much in the UI makes tests flaky, slow to run and hard to maintain.

Automation does not replace manual testing. Exploratory testing and accessibility testing with assistive
technology still need people. What automation gives is the quickest feedback that something has broken.

## 2. What is built

| | API test | UI test |
|---|---|---|
| System | Swagger Petstore | OrangeHRM demo |
| Scenario | `GET /pet/{petId}` | Login and logout |
| Code | `api/tests/GetPetByIdApiTest` | `ui/tests/LoginUiTest` |

### 2.1 Why `GET /pet/{petId}`

It is deliberately simple, so that it can be understood and explained in full. It is also the **read path**:
every other operation (create, update, delete) is verified by reading the pet back, so it is the foundation
the other tests rely on. A failure here gives early warning with a short feedback loop.

Simple does not mean shallow. It carries a clear error contract, which gives plenty of negative cases.

| Scenario | Type | What it proves |
|---|---|---|
| Create a pet, then get it by id | Positive | The round trip works and every field matches |
| Ids that do not exist (`0`, `-1`, the largest valid id) | Negative | `404` with "Pet not found", including boundary values |
| Malformed ids (`abc`, `1.5`, `null`, one more than the largest id) | Negative | A client error (4xx) and never a server error (5xx) |
| Malformed ids, strict contract | Negative, known defect | See section 5. Disabled until the defect is fixed. |

**What it does not cover:** the other pet operations, and the store and user resources. They are in section 4.

### 2.2 Why login

Login is the first interaction a user has, and it **gates everything else**: if it breaks, nothing behind it
can be reached. It is simple but still carries meaningful positive, negative and security behaviour.

| Scenario | Type | What it proves |
|---|---|---|
| Valid login, then logout, then open a protected page directly | Positive | The session really ends on the server, not only on screen |
| Wrong password, unknown user, wrong-case password, injection-style username | Negative | "Invalid credentials" is shown and the user stays on the login page. The message does not reveal which field was wrong. |
| Both fields empty, only one empty | Negative | "Required" appears under exactly the empty fields, and no request is sent |

**What it does not cover:** anything after login. See section 4.

## 3. Where tests belong: API or UI

I test functionality as close to the logic as possible. The back end is where the interaction with the
database actually takes place, so persistence and business rules are proved at the API level. The UI tests
prove that the screen calls the correct functionality and responds successfully.

For example, take "add an employee, then check they appear in the list". As one UI test it mixes four
intents: does the form work, does the write reach the database, does the list query work, does search work.
When it fails the cause is unclear, and it is slow and flaky because everything goes through the browser.
Instead:

| Test | Layer | Intent |
|---|---|---|
| Create an employee and read it back. Required-name and duplicate-ID rules. | API | The data is saved and the rules hold |
| The Add Employee form works (one happy-path test) | UI | The form is wired to the back end |
| The list shows an employee. Create them by API in the set-up, assert once in the UI, clean up by API. | UI, thin | The list displays what is stored |

Separating the layers does not mean never combining them. One genuine end-to-end journey stays at the top as
a thin smoke test, to prove the pieces connect. The shape is a test pyramid: many fast tests underneath and a
few slow ones on top. Other speed measures follow the same logic: log in once and reuse the session for UI
tests that are not about login, and avoid sleeps by relying on Playwright's auto-waiting assertions.

## 4. Further scenarios, in priority order

**How priority was decided.** I used impact first, then likelihood, cost and layer. The same rule gives
different answers for the two systems. The Petstore holds throwaway data in a public sandbox, so security
checks follow the core flows. OrangeHRM holds the kind of data that is personal and sensitive, so session
security comes first. For the Petstore I went breadth first: one end-to-end flow for every resource, then
depth.

### 4.1 Petstore API

| P | Scenario | Why |
|---|---|---|
| 1 | **User** flow: create, login, get, update, delete. Negative: wrong password, duplicate username. | Completes the core, and it handles credentials |
| 2 | **Store** flow: place an order, get it, delete it. Negative: unknown order, order for a pet that does not exist. | Completes the core. A business flow across two resources. |
| 3 | **Pet depth:** invalid bodies on create, `PUT` and `DELETE` lifecycle, `findByStatus` | The pet resource goes deeper once every resource has a flow |
| 4 | **Security checks:** no internal errors or stack traces in responses, injection-style input, authorisation on write endpoints | The defect in section 5 shows this is a real risk |
| 5 | **Response-time indicator** | Recorded and flagged, never a pass/fail gate. The server is shared and its timing varies, so a hard limit would make the suite flaky. |

### 4.2 OrangeHRM UI

| P | Scenario | Why |
|---|---|---|
| 1 | **Session handling:** an expired or cleared session goes to login, and the back button after logout shows no data. Expiry is simulated, not waited for. | Highest impact, and cheap to run reliably |
| 2 | **Dashboard smoke test with an accessibility scan** | One quick check that the whole application loads |
| 3 | **Role-based access:** a basic employee cannot reach admin or HR pages, by menu or by direct address | Exposure of people's data. The user is created through the API where possible. |
| 4 | **Add employee, then find them in the list** (see section 3) | The core HR journey |
| 5 | **List search and filter**, including a "no records" result | The most-used admin feature |
| Last | **Visual snapshot** with dynamic areas masked | Highest upkeep. The dashboard shows live data, and rendering differs between machines. |

**Accessibility (WCAG 2.2 AA) applies throughout,** so every page the UI tests visit gets an automated scan.
Automated tools find only part of the issues (the GDS Way puts it at about 30%), so manual checks and
assistive-technology testing are also needed.

## 5. Findings

1. **Malformed ids return the wrong status and leak internals.** `GET /pet/abc` returns `404` with a message
   naming `java.lang.NumberFormatException`. The API specification says `400` "Invalid ID supplied". This is
   both a contract defect and an information leak. It is kept as a disabled test, so it stays visible in the
   report without turning the build red, and it can be re-enabled when the defect is fixed.
2. **The login form fields have no linked labels.** The labels are shown but not connected to their inputs, so
   the fields cannot be found by label. That is probably an accessibility failure and is worth a full scan.
   It is also why the tests locate the fields by placeholder.

## 6. Trade-offs

- **Shared public systems.** Anyone can change or reset the Petstore and the OrangeHRM demo at any time.
  Tests create and delete their own data and do not depend on global state. A real team would use a
  dedicated environment.
- **Demo credentials are committed to the repository.** This is a deliberate choice for simplicity: they are
  the public demo values the site itself displays. They can be overridden from the command line or an
  environment variable, and real credentials would come from a secrets store.
- **Some locators are not user-facing,** because OrangeHRM gives a few elements no role or accessible name.
  They are marked and kept in the page objects, so a change is a one-line fix.
- **Not built in the time box:** the further scenarios in section 4, and reuse of a saved login session.

## 7. Practices followed from the GDS Way

- A README written for a newcomer, with detail moved into `docs/`
- Commit messages that explain why a change was made
- The pipeline has read-only permissions, uses only GitHub's own actions, and pins each to a full commit ID
- WCAG 2.2 AA as the accessibility standard
