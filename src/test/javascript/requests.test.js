import {describe, expect, it} from 'vitest';
import {jcontentPath, matches, olderThan, parseTargets, toRows} from '../../javascript/Requests/requests';
import {csvCell, toCsv} from '../../javascript/utils/exports';

const node = (uuid, properties, refNode = null) => ({
    uuid,
    properties: Object.entries(properties).map(([name, value]) => (Array.isArray(value) ? {name, values: value} : {name, value})),
    targetNode: {refNode}
});

describe('parseTargets', () => {
    it('reads locale, ticket and word count of each target', () => {
        expect(parseTargets('fr-FR_abc123_250, de-DE_def456_')).toEqual([
            {locale: 'fr-FR', ticket: 'abc123', words: 250},
            {locale: 'de-DE', ticket: 'def456', words: null}
        ]);
    });

    it('returns nothing for an empty value', () => {
        expect(parseTargets(null)).toEqual([]);
    });
});

describe('jcontentPath', () => {
    it('opens a page in the pages accordion', () => {
        expect(jcontentPath({isPage: true, path: '/sites/acme/home/about'}, 'acme')).toBe('pages/home/about');
    });

    it('opens content on its closest page', () => {
        const refNode = {
            isPage: false,
            path: '/sites/acme/home/about/main/text',
            parent: {path: '/sites/acme/home/about/main'},
            ancestors: [{path: '/sites/acme/home', isPage: true}, {path: '/sites/acme/home/about', isPage: true}, {path: '/sites/acme/home/about/main', isPage: false}]
        };
        expect(jcontentPath(refNode, 'acme')).toBe('pages/home/about');
    });

    it('opens other content in its content folder', () => {
        const refNode = {isPage: false, path: '/sites/acme/contents/news/n1', parent: {path: '/sites/acme/contents/news'}, ancestors: []};
        expect(jcontentPath(refNode, 'acme')).toBe('content-folders/contents/news');
    });

    it('has no target for deleted content', () => {
        expect(jcontentPath(null, 'acme')).toBeNull();
    });
});

describe('toRows', () => {
    const nodes = [
        node('a', {name: 'Old', 'jcr:created': '2026-01-01T10:00:00.000Z', gblSubmitState: 'request.translated', sourceLanguage: 'en###en-US', targetLanguage: ['fr###fr-FR'], targetTicket: 'fr-FR_t1_100'}),
        node('b', {name: 'New', 'jcr:created': '2026-03-01T10:00:00.000Z', gblError: 'Connector refused'}),
        node('c', {name: 'Skipped', 'jcr:created': '2026-02-01T10:00:00.000Z', skipTranslated: 'true', gblContentCount: '0'})
    ];

    it('sorts newest first, maps states and errors, drops requests that found nothing to translate', () => {
        const rows = toRows(nodes, 'acme');
        expect(rows.map(row => row.id)).toEqual(['b', 'a']);
        expect(rows[0].status).toBe('error');
        expect(rows[1]).toMatchObject({status: 'translated', source: 'en-US', targets: ['fr-FR'], words: 100});
    });

    it('keeps a request without state as created', () => {
        expect(toRows([node('d', {name: 'Fresh', 'jcr:created': '2026-03-02T10:00:00.000Z'})], 'acme')[0].status).toBe('created');
    });

    it('selects what the cleanup deletes: created on or before midnight, n days ago', () => {
        const rows = toRows(nodes, 'acme');
        expect(olderThan(rows, 30, new Date('2026-03-15T12:00:00Z')).map(row => row.id)).toEqual(['a']);
    });

    it('searches name, languages and status label', () => {
        const [row] = toRows([nodes[0]], 'acme');
        expect(matches(row, 'fr-fr', 'Translated')).toBe(true);
        expect(matches(row, 'translated', 'Translated')).toBe(true);
        expect(matches(row, 'nothing', 'Translated')).toBe(false);
    });
});

describe('CSV export', () => {
    it('quotes every cell and neutralises formulas', () => {
        expect(csvCell('=SUM(A1)')).toBe('"\'=SUM(A1)"');
        expect(csvCell('say "hi"')).toBe('"say ""hi"""');
        expect(csvCell(null)).toBe('""');
    });

    it('starts with a BOM and ends lines with CRLF', () => {
        const csv = toCsv(['a'], [['1']]);
        expect(csv.codePointAt(0)).toBe(0xFEFF);
        expect(csv.slice(1)).toBe('"a"\r\n"1"\r\n');
    });
});
