# Jahia GlobalLink Translation Connector

The GlobalLink Translation Connector links Jahia to [GlobalLink Connect](https://www.translations.com/), the translation management platform of Translations.com.

Editors send pages from jContent to Translations.com. When the translation is complete, the connector imports the translated text into the target languages of the same pages. Editors then review and publish the result.

- [Requirements](#requirements)
- [Installation](#installation)
- [Configure a site](#configure-a-site)
- [Request a translation](#request-a-translation)
- [Follow your requests](#follow-your-requests)
- [Email notifications](#email-notifications)
- [Troubleshooting](#troubleshooting)
- [Support and license](#support-and-license)

## Requirements

- Jahia 8.2.0.0 or later.
- A GlobalLink Connect account from Translations.com. Translations.com gives you these values:
  - the API URL
  - a username and password (use a technical account)
  - the connector name
  - the file format (usually `Jahia_XML`)
- Outbound HTTPS access from Jahia to the GlobalLink API URL. In a cluster, the processing server must have this access, because it sends and retrieves the translations.
- Optional: a mail server configured in Jahia, for the [email notifications](#email-notifications).

## Installation

1. Get the module from the [Jahia Store](https://store.jahia.com/contents/modules-repository/org/jahia/modules/jahia-translation-globallink.html).
2. In **Administration > Modules and extensions > Modules**, install the module **Jahia GlobalLink Translation Connector** (`jahia-translation-globallink`).
3. Enable the module on each site that needs translations.

## Configure a site

You configure the connector for each site separately. To do this, you need the `adminGlobalLinkTranslation` permission. By default, the **site-administrator** and **editor-in-chief** roles have this permission.

1. In **Administration**, select the site, then open **GlobalLink - Settings**.
2. Fill in the connection settings (see the table below).
3. In **Content types to translate**, select the content types that the connector sends for translation. You must select at least one type. For example, to translate rich text, select **Rich text**. Use the **Editorial** filter to show only editorial content types, or **All** to show all of them.
4. Click **Save Settings**. The connector connects to GlobalLink to check the settings. If the settings are correct, it shows **The configuration is valid.**
5. In **Language mappings**, link each site language to a GlobalLink language. The list shows only the language directions that your GlobalLink connector supports.
6. Click **Save Settings** again.

> [!IMPORTANT]
> Save the settings at least once before editors request translations. The first save creates the folder that stores the translation requests of the site.

### Connection settings

| Setting | Description |
|---|---|
| **Enable** | Turns the connector on or off for the site. When the connector is off, it does not send or retrieve translations for the site. |
| **Username** | Your GlobalLink username. GlobalLink shows this user as the submitter of all requests. Use a technical account. |
| **Password** | The password of the GlobalLink user. |
| **GlobalLink API URL** | The API URL that Translations.com gives you, for example `https://connect.translations.com/api/v2/`. |
| **Connector name** | The name of your GlobalLink connector. If the name is incorrect, the page shows the connector names that your account can use. |
| **Prefix for the default name of the requests** | When an editor does not give a request name, the connector uses `<prefix> - <page name>`. |
| **File format** | The file format of your GlobalLink connector. The default is `Jahia_XML`. Ask Translations.com if you are not sure. |
| **Documents location** | The server folder for the XML files that the connector sends and receives. Leave this field empty to use the default folder, `<Tomcat folder>/temp/gbl`. |
| **Submissions, retrieval interval (minutes)** | How often the connector sends new requests, retrieves completed translations and imports them. For example, `2`. |

If Translations.com adds or removes languages on your connector, click **Reload GlobalLink languages directions**. Then update the language mappings.

> [!NOTE]
> The connector stores the GlobalLink username and password as properties of the site in the Jahia content repository. Give the `adminGlobalLinkTranslation` permission only to trusted users, and protect your site exports and repository backups.

## Request a translation

1. In jContent, select a page.
2. Open the page actions menu and select **Request a translation**.
3. Fill in the form:

   | Field | Description |
   |---|---|
   | **Request name** | Optional. If you leave it empty, the connector uses the default name `<prefix> - <page name>`. |
   | **Starting node** | The page that you selected. The connector sends the content of this page. |
   | **Source language** | The language to translate from. The list shows only the site languages that have a language mapping. |
   | **Target language** | One or more languages to translate into. The list shows only the languages that GlobalLink can translate from the source language. |
   | **Also translate sub pages** | Also sends all the sub pages of the selected page. |
   | **Skip already translated content** | Does not send content that an earlier request already translated from the same source language, for any target language. See the warning below. |
   | **Due date** | Optional. The date when the translation must be complete. The default is 5 days from now. If you set a date, it must be more than one day in the future. |

4. Save the request.

> [!WARNING]
> **Skip already translated content** marks content as translated when the connector imports its first translation. The mark stays when an editor changes the source text later. To send changed content again, do not select this option.

### What the connector sends

The connector sends only:

- content of the types that the site administrator selected in **Content types to translate**,
- text properties that can have a different value for each language.

It does not send images, files, dates or links to other content.

The connector locks the source content that it sent. It removes the lock when it imports the translation.

### What happens next

1. At the next interval, the connector sends the request to GlobalLink.
2. Translations.com translates the content.
3. The connector retrieves the completed translation and writes the translated text into the target languages of the same content.
4. **The connector does not publish the translated content.** Editors must review and publish it in jContent.

If an editor deletes a page while its translation is in progress, the connector cancels the request in GlobalLink.

## Follow your requests

In jContent, open **GlobalLink - Requests** to see all the translation requests of the site. You need the `adminGlobalLinkTranslation` permission.

For each request, the dashboard shows the status, the language direction, the submitter, the submission date, the due date, the word count and a link to the content in jContent.

| Status | Meaning |
|---|---|
| **CREATED** | The request is saved in Jahia. The connector has not sent it yet. |
| **SUBMITTED** | The connector sent the request to GlobalLink. |
| **RETRIEVED** | The connector downloaded the translation. It has not imported it yet. |
| **TRANSLATED** | The translated text is in Jahia, ready for review and publication. |
| **ERROR** | The connector could not import the translation. See [Troubleshooting](#troubleshooting). |
| **CANCELLED** | Someone cancelled the request in GlobalLink, or someone deleted the page in Jahia. |
| **DELETED** | The request no longer exists in GlobalLink. |
| **No content to translate** | The page has no content of the selected content types. |

You can also follow your requests in GlobalLink.

To remove old requests from the dashboard, use **Clean submission older than _n_ days**. This removes the requests from Jahia only. It does not change them in GlobalLink.

> [!WARNING]
> **Clean** removes all the requests that are older than the number of days, whatever their status. If you remove a request that is still in progress, the connector does not import its translation.

## Email notifications

If a mail server is configured in Jahia, the connector sends an email to the user who created the request when:

- the connector sends the request to GlobalLink,
- the translation is imported,
- an error occurs.

The connector uses the email address in the user's profile. If the user has no email address, it uses the default recipient of the Jahia mail server settings.

If the connector cannot connect to GlobalLink, it also sends an alert to the default recipient of the Jahia mail server settings.

## Troubleshooting

| Problem | What to do |
|---|---|
| The settings page shows **The connector name "…" was not found. You can use: "…"**. | Copy one of the names that the message shows into **Connector name**, then save again. |
| The settings page shows **There is a configuration issue**. | Check the API URL, the username and the password. Make sure that Jahia can connect to the API URL. |
| The **Language mappings** list is empty or incomplete. | Click **Reload GlobalLink languages directions**. If languages are still missing, ask Translations.com to add them to your connector. |
| A request stays in **CREATED**. | Make sure that **Enable** is on and that the site settings are valid. Wait for the interval to pass. In a cluster, make sure that the processing server can connect to GlobalLink. |
| A request is in **ERROR**. | Make sure that the source and target languages exist on the site and have a language mapping. Then look at the Jahia logs. |
| **Request a translation** fails for a site. | Make sure that a site administrator saved the GlobalLink settings of the site at least once. |
| The **Source language** list is empty. | Make sure that **Enable** is on and that the site languages have a language mapping. |

The connector writes its log messages under the `org.jahia.translation.globallink` logger. To get more details, set this logger to `DEBUG`. For example, in the Karaf console:

```
log:set DEBUG org.jahia.translation.globallink
```

## Support and license

This is a community module. To report a problem or suggest an improvement, open an issue in this repository.

For questions about your GlobalLink account, your connector or the translation service, contact Translations.com.

This module is distributed under the [Apache License 2.0](LICENSE).
