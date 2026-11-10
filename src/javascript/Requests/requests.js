import {gql} from '@apollo/client';

export const REQUESTS_QUERY = gql`
    query GlobalLinkRequests($sitePath: String!, $language: String!) {
        jcr {
            nodesByCriteria(criteria: {nodeType: "gblnt:globalLinkProject", paths: [$sitePath]}) {
                nodes {
                    uuid
                    workspace
                    path
                    properties(names: ["name", "submissionTicket", "sourceLanguage", "targetLanguage", "targetTicket", "gblContentCount", "gblError", "gblSubmitState", "skipTranslated", "dueDate", "jcr:created", "jcr:createdBy"]) {
                        name
                        value
                        values
                    }
                    targetNode: property(name: "targetNode") {
                        refNode {
                            uuid
                            workspace
                            path
                            displayName(language: $language)
                            isPage: isNodeType(type: {types: ["jnt:page"]})
                            parent {
                                uuid
                                workspace
                                path
                            }
                            ancestors(upToPath: $sitePath) {
                                uuid
                                workspace
                                path
                                isPage: isNodeType(type: {types: ["jnt:page"]})
                            }
                        }
                    }
                }
            }
        }
    }
`;

const MAPPING_SEPARATOR = '###';

// The gblSubmitState property holds the resource bundle key the translation job wrote
const STATES = {
    'request.submitted': 'submitted',
    'request.retrieved': 'retrieved',
    'request.translated': 'translated',
    'request.cancelled': 'cancelled',
    'request.deleted': 'deleted',
    'request.no.document': 'noDocument',
    'request.content.error': 'error'
};

export const STATUS_COLORS = {
    created: 'default',
    submitted: 'accent',
    retrieved: 'accent',
    translated: 'success',
    cancelled: 'warning',
    deleted: 'warning',
    noDocument: 'default',
    error: 'danger'
};

const after = (text, separator) => {
    const index = (text || '').indexOf(separator);
    return index < 0 ? (text || '') : text.slice(index + separator.length);
};

const before = (text, separator) => {
    const index = (text || '').indexOf(separator);
    return index < 0 ? (text || '') : text.slice(0, index);
};

/** {@code "<locale>_<ticket>_<word count>"} entries, comma separated, as the submission service writes them. */
export const parseTargets = targetTicket => (targetTicket || '')
    .split(',')
    .map(entry => entry.trim())
    .filter(Boolean)
    .map(entry => {
        const remaining = after(entry, '_');
        const words = Number.parseInt(after(remaining, '_'), 10);
        return {locale: before(entry, '_'), ticket: before(remaining, '_'), words: Number.isNaN(words) ? null : words};
    });

/** The jContent screen showing the translated node: its page, else the content folder holding it. */
export const jcontentPath = (refNode, siteKey) => {
    if (!refNode) {
        return null;
    }

    const sitePath = `/sites/${siteKey}`;
    const page = refNode.isPage ? refNode : [...(refNode.ancestors || [])].reverse().find(ancestor => ancestor.isPage);
    if (page) {
        return `pages${page.path.slice(sitePath.length)}`;
    }

    const parentPath = refNode.parent?.path || refNode.path;
    return `content-folders${parentPath.slice(sitePath.length)}`;
};

export const toRow = (node, siteKey) => {
    const props = Object.fromEntries((node.properties || []).map(property => [property.name, property]));
    const value = name => props[name]?.value ?? null;
    const targets = parseTargets(value('targetTicket'));
    const wordCounts = targets.map(target => target.words).filter(words => words !== null);
    const error = value('gblError');
    const state = value('gblSubmitState');
    const refNode = node.targetNode?.refNode || null;
    return {
        id: node.uuid,
        name: value('name') || '',
        ticket: value('submissionTicket') || '',
        page: refNode?.displayName || '',
        pagePath: refNode?.path || '',
        jcontentPath: jcontentPath(refNode, siteKey),
        source: after(value('sourceLanguage'), MAPPING_SEPARATOR),
        targets: (props.targetLanguage?.values || []).map(target => after(target, MAPPING_SEPARATOR)),
        targetDetails: targets,
        words: wordCounts.length > 0 ? wordCounts.reduce((sum, words) => sum + words, 0) : null,
        contentCount: value('gblContentCount') === null ? null : Number(value('gblContentCount')),
        created: value('jcr:created') || '',
        createdBy: value('jcr:createdBy') || '',
        dueDate: value('dueDate') || '',
        status: error ? 'error' : (STATES[state] || state || 'created'),
        error: error || '',
        skipped: value('skipTranslated') === 'true' && Number(value('gblContentCount')) === 0
    };
};

/** Requests of the site, newest first; requests that skipped every translated content and found none are left out, as before. */
export const toRows = (nodes, siteKey) => (nodes || [])
    .map(node => toRow(node, siteKey))
    .filter(row => !row.skipped)
    .sort((a, b) => b.created.localeCompare(a.created));

/** Rows the cleanup action deletes: created on or before midnight, {@code days} days ago. */
export const olderThan = (rows, days, now = new Date()) => {
    const limit = new Date(now);
    limit.setDate(limit.getDate() - days);
    limit.setHours(0, 0, 0, 0);
    return rows.filter(row => row.created && new Date(row.created) <= limit);
};

export const matches = (row, search, statusLabel) => {
    const text = search.trim().toLowerCase();
    if (!text) {
        return true;
    }

    return [row.name, row.ticket, row.page, row.pagePath, row.createdBy, row.source, ...row.targets, statusLabel]
        .some(field => (field || '').toLowerCase().includes(text));
};
