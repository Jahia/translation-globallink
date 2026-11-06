# Jahia GlobalLink Translation Connector

Sends Jahia pages to [GlobalLink](https://www.translations.com/) (translations.com) for translation and imports the
translated content back. API documentation: https://connect-dev.translations.com/docs/api/

## Requirements

- Jahia 8.2, jContent 3.7 or later (the module does not start on an older jContent)
- A GlobalLink account: username, password, API URL and connector name, given by translations.com

## Screens

| Screen | Where | Who |
|---|---|---|
| GlobalLink translation | Administration > Sites > *site* > GlobalLink translation | `adminGlobalLinkTranslation` (site administrators, editors-in-chief) |
| Translation requests | jContent > Additional > GlobalLink requests | `adminGlobalLinkTranslation` |
| Request a translation | menu of a page in jContent (page tree, content list, right-click) | editors, on sites where requests are switched on |

**Settings.** Connection (switch, credentials, API URL, connector, request name prefix, file format, documents
location, check interval), language directions of the connector with the GlobalLink locale of each site
language, and the content types sent for translation. The password is never sent back to the browser: leave the
field empty to keep the stored one. *Reload GlobalLink languages* reads the connector's language directions
again.

**Requests.** Every request of the site with its page, languages, dates, volume and status; search, sorting,
pages, CSV and JSON export, and deletion of requests older than a number of days.

## Build and deploy

Maven 3.8+, JDK 17 (the front end builds with Node 22 and Yarn 1, installed by the build):

```sh
mvn clean install
```

Then upload `target/jahia-translation-globallink-<version>.jar` in Administration > Modules, or deploy it with
`mvn jahia:deploy`. Front-end unit tests (Vitest) run in the `test` phase; `-DskipTests` skips them with the
Java tests.

## Front end

React 18 UI extension (webpack Module Federation, loaded by app-shell 3 and 4), built from Moonstone components:

- `src/javascript/init.js`: registrations (Site settings route, jContent app, page action, Content Editor config)
- `src/javascript/Settings/`, `src/javascript/Requests/`: the two screens, loaded on first display
- `src/main/resources/javascript/locales/`: EN, FR, DE labels

The screens call the module's actions with `XMLHttpRequest` (the CSRF guard of jContent pages signs them):
`globalLinkSettingsData` (read), `globalLinkConfig`, `globalLinkRefreshLanguages`, `globalLinkSubmissionClean`.
