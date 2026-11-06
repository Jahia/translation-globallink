// CSV and JSON exports built in the browser from the report a screen already holds.

const FORMULA_START = /^[=+\-@\t\r]/;

export const csvCell = value => {
    let text = value === null || value === undefined ? '' : String(value);
    if (FORMULA_START.test(text)) {
        // Spreadsheets run a cell starting with these characters as a formula
        text = `'${text}`;
    }

    return `"${text.replaceAll('"', '""')}"`;
};

export const toCsv = (header, rows) =>
    `\uFEFF${[header, ...rows].map(row => row.map(csvCell).join(',')).join('\r\n')}\r\n`;

export const download = (content, type, fileName) => {
    const url = URL.createObjectURL(new Blob([content], {type}));
    const link = Object.assign(document.createElement('a'), {href: url, download: fileName});
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
};

export const exportFileName = (feature, siteKey, language, extension) =>
    `${feature}-${siteKey}-${language}-${new Date().toISOString().slice(0, 10)}.${extension}`;
