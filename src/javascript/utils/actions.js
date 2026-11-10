// Calls the module's Jahia Actions (*.do) on a site node.
// XMLHttpRequest, not fetch: jContent pages load the CSRF guard script, which adds its token to every
// XMLHttpRequest, so the write actions need no token handling here.

export class ActionError extends Error {
    constructor(status) {
        super(`Action failed with status ${status}`);
        this.status = status;
    }
}

const contextPath = () => globalThis.contextJsParameters?.contextPath || '';

export const actionUrl = (siteKey, language, action) =>
    `${contextPath()}/cms/render/default/${encodeURIComponent(language)}/sites/${encodeURIComponent(siteKey)}.${action}.do`;

const toForm = params => {
    const form = new URLSearchParams();
    Object.entries(params || {}).forEach(([name, value]) => {
        (Array.isArray(value) ? value : [value]).forEach(item => form.append(name, item ?? ''));
    });
    return form.toString();
};

export const callAction = ({siteKey, language, action, method = 'GET', params}) => new Promise((resolve, reject) => {
    const body = toForm(params);
    const url = actionUrl(siteKey, language, action) + (method === 'GET' && body ? `?${body}` : '');
    const xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    if (method !== 'GET') {
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8');
    }

    xhr.onload = () => {
        // A refused CSRF token or permission ends on Jahia's error page, followed by the browser
        if (xhr.status < 200 || xhr.status >= 300 || /\/error\.html/.test(xhr.responseURL || '')) {
            reject(new ActionError(xhr.status));
            return;
        }

        try {
            resolve(xhr.responseText ? JSON.parse(xhr.responseText) : {});
        } catch {
            reject(new ActionError(xhr.status));
        }
    };

    xhr.onerror = () => reject(new ActionError(0));
    xhr.send(method === 'GET' ? null : body);
});
