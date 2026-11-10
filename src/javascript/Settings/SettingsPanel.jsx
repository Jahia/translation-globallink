import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useNodeChecks} from '@jahia/data-helper';
import {useNotifications} from '@jahia/react-material';
import {Banner, Button, Chip, Field, Header, Input, LayoutContent, Loader, Reload, Save, Switch, Typography} from '@jahia/moonstone';
import {MODULE_NAME, PERMISSION} from '../constants';
import {callAction} from '../utils/actions';
import {LogoTitle} from '../GlobalLinkLogo/GlobalLinkLogo';
import {useSiteContext} from '../utils/useSiteContext';
import {ComponentsCard} from './ComponentsCard';
import {LanguagesCard} from './LanguagesCard';
import styles from './SettingsPanel.module.css';

const MAPPING_SEPARATOR = '###';
const TEXT_FIELDS = ['username', 'url', 'connectorName', 'submissionPrefix', 'fileFormat', 'fileLocation', 'interval'];
const REQUIRED_FIELDS = new Set(['username', 'url', 'connectorName', 'submissionPrefix', 'fileFormat', 'interval']);
const PLACEHOLDERS = {
    url: 'https://connect.translations.com/api/v2/',
    fileFormat: 'Jahia_XML',
    interval: '2'
};

const toForm = settings => ({
    activated: Boolean(settings.activated),
    username: settings.username || '',
    password: '',
    url: settings.url || '',
    connectorName: settings.connectorName || '',
    submissionPrefix: settings.submissionPrefix || '',
    fileFormat: settings.fileFormat || 'Jahia_XML',
    fileLocation: settings.fileLocation || '',
    interval: settings.interval || ''
});

const snapshot = (form, components, mappings) => JSON.stringify({form, components: [...components].sort((a, b) => a.localeCompare(b)), mappings});

const validate = (form, passwordSet, components) => {
    const errors = {};
    REQUIRED_FIELDS.forEach(field => {
        if (!form[field].trim()) {
            errors[field] = 'required';
        }
    });
    if (!passwordSet && !form.password) {
        errors.password = 'required';
    }

    if (form.interval.trim() && !/^[1-9]\d*$/.test(form.interval.trim())) {
        errors.interval = 'interval';
    }

    if (components.length === 0) {
        errors.components = 'components';
    }

    return errors;
};

const connectionChip = (data, t) => {
    if (!data.settings.activated) {
        return <Chip label={t('settings.state.disabled')}/>;
    }

    switch (data.connection.state) {
        case 'OK':
            return <Chip color="success" label={t('settings.state.connected')}/>;
        case 'NOT_CONFIGURED':
            return <Chip color="warning" label={t('settings.state.notConfigured')}/>;
        default:
            return <Chip color="danger" label={t('settings.state.error')}/>;
    }
};

export const SettingsPanel = () => {
    const {t} = useTranslation(MODULE_NAME);
    const {siteKey, language, uilang} = useSiteContext();
    const notifications = useNotifications();
    const notify = message => notifications?.notify?.(message, ['closeButton']);

    // The route option only hides the menu entry: a direct URL or a site switch still renders this panel
    const {checksResult, loading: checking} = useNodeChecks(
        {path: `/sites/${siteKey}`},
        {requireModuleInstalledOnSite: [MODULE_NAME], requiredSitePermission: [PERMISSION]}
    );

    const [data, setData] = useState(null);
    const [loadFailed, setLoadFailed] = useState(false);
    const [form, setForm] = useState(null);
    const [components, setComponents] = useState([]);
    const [mappings, setMappings] = useState({});
    const [initial, setInitial] = useState('');
    const [busy, setBusy] = useState(null);
    const [showErrors, setShowErrors] = useState(false);

    const load = useCallback(async () => {
        setLoadFailed(false);
        try {
            const result = await callAction({siteKey, language, action: 'globalLinkSettingsData', params: {uilang}});
            const nextForm = toForm(result.settings);
            const nextComponents = result.components.selected.map(option => option.value);
            setData(result);
            setForm(nextForm);
            setComponents(nextComponents);
            setMappings(result.languageMappings);
            setInitial(snapshot(nextForm, nextComponents, result.languageMappings));
            setShowErrors(false);
        } catch {
            setLoadFailed(true);
        }
    }, [siteKey, language, uilang]);

    useEffect(() => {
        if (checksResult) {
            load();
        }
    }, [checksResult, load]);

    const errors = useMemo(() => (form && data ? validate(form, data.settings.passwordSet, components) : {}), [form, data, components]);
    const isDirty = Boolean(form) && snapshot(form, components, mappings) !== initial;
    const hasErrors = Object.keys(errors).length > 0;

    const title = t('settings.title');

    if (checking) {
        return null;
    }

    if (!checksResult) {
        return (
            <LayoutContent hasPadding header={<Header title={<LogoTitle>{title}</LogoTitle>}/>}>
                <div className={styles.card} role="status">
                    <Typography>{t('settings.notEnabled')}</Typography>
                </div>
            </LayoutContent>
        );
    }

    if (loadFailed) {
        return (
            <LayoutContent hasPadding header={<Header title={<LogoTitle>{title}</LogoTitle>} mainActions={[<Button key="retry" size="big" icon={<Reload/>} label={t('common.retry')} onClick={load}/>]}/>}>
                <div className={styles.card}>
                    <Banner variant="danger" title={t('settings.loadError.title')}>{t('settings.loadError.message')}</Banner>
                </div>
            </LayoutContent>
        );
    }

    if (!data || !form) {
        return (
            <LayoutContent hasPadding header={<Header title={<LogoTitle>{title}</LogoTitle>}/>}>
                <div className={styles.loading}><Loader size="big"/></div>
            </LayoutContent>
        );
    }

    const setField = (field, value) => setForm(previous => ({...previous, [field]: value}));
    const fieldError = field => (showErrors && errors[field] ? t(`settings.errors.${errors[field]}`) : undefined);

    const save = async () => {
        if (hasErrors) {
            setShowErrors(true);
            notify(t('settings.errors.summary'));
            return;
        }

        setBusy('save');
        const mapped = data.siteLanguages
            .filter(siteLanguage => mappings[siteLanguage.value])
            .map(siteLanguage => `${siteLanguage.value}${MAPPING_SEPARATOR}${mappings[siteLanguage.value]}`);
        try {
            const result = await callAction({
                siteKey,
                language,
                action: 'globalLinkConfig',
                method: 'POST',
                params: {
                    'j:globalLinkActivated': String(form.activated),
                    'j:globalLinkUsername': form.username.trim(),
                    'j:globalLinkPassword': form.password,
                    'j:globalLinkUrl': form.url.trim(),
                    'j:globalLinkUserAgent': MODULE_NAME,
                    'j:globalLinkConnectorName': form.connectorName.trim(),
                    'j:globalLinkSubmissionPrefix': form.submissionPrefix.trim(),
                    'j:globalLinkfileFormat': form.fileFormat.trim(),
                    'j:globalLinkfileLocation': form.fileLocation.trim(),
                    'j:globalLinkInterval': form.interval.trim(),
                    'j:componentsList': components,
                    // An empty value clears the stored mapping: an absent parameter leaves it as it was
                    'j:languageMappings': mapped.length > 0 ? mapped : ['']
                }
            });
            notify(result.message === 'gbl.settings.success' ? t('settings.saved') : t('settings.savedWithError'));
        } catch {
            notify(t('settings.saveFailed'));
        }

        await load();
        setBusy(null);
    };

    const refreshLanguages = async () => {
        setBusy('refresh');
        try {
            const result = await callAction({siteKey, language, action: 'globalLinkRefreshLanguages', method: 'POST'});
            notify(result.message === 'gbl.settings.success' ? t('settings.languagesRefreshed') : t('settings.refreshFailed'));
        } catch {
            notify(t('settings.refreshFailed'));
        }

        await load();
        setBusy(null);
    };

    const connectionOk = data.connection.state === 'OK';
    const connectorIssue = data.status && data.status !== 'OK';

    const mainActions = [
        connectionOk && (
            <Button key="refresh"
                    size="big"
                    variant="outlined"
                    icon={<Reload/>}
                    label={t('settings.refreshLanguages')}
                    isDisabled={Boolean(busy) || isDirty}
                    isLoading={busy === 'refresh'}
                    onClick={refreshLanguages}/>
        ),
        <Button key="save"
                size="big"
                color="accent"
                icon={<Save/>}
                label={t('common.save')}
                isDisabled={Boolean(busy) || !isDirty}
                isLoading={busy === 'save'}
                onClick={save}/>
    ].filter(Boolean);

    const textField = field => {
        const id = `gbl-${field}`;
        return (
            <Field key={field}
                   id={`${id}-field`}
                   label={t(`settings.fields.${field}.label`) + (REQUIRED_FIELDS.has(field) ? ' *' : '')}
                   helper={t(`settings.fields.${field}.helper`)}
                   hasError={Boolean(fieldError(field))}
                   errorMessage={fieldError(field)}
            >
                {/* Moonstone's Field label points at no control: name the input itself */}
                <Input id={id}
                       aria-label={t(`settings.fields.${field}.label`)}
                       value={form[field]}
                       placeholder={PLACEHOLDERS[field]}
                       onChange={event => setField(field, event.target.value)}/>
            </Field>
        );
    };

    return (
        <div className={styles.root} data-gbl-role="settings">
            <LayoutContent hasPadding
                           header={<Header title={<LogoTitle>{title}</LogoTitle>} status={connectionChip(data, t)} mainActions={mainActions}/>}
            >
                <div className={styles.content}>
                    {connectorIssue && (
                        <Banner variant="danger" title={t('settings.banner.connectorTitle')}>
                            {t('settings.banner.connector', {connector: form.connectorName, available: data.status})}
                        </Banner>
                    )}
                    {!connectorIssue && data.settings.activated && ['ERROR', 'INVALID'].includes(data.connection.state) && (
                        <Banner variant="danger" title={t('settings.banner.connectionTitle')}>
                            {data.connection.message || t('settings.banner.connection')}
                        </Banner>
                    )}

                    <section className={styles.card} aria-labelledby="gbl-connection-title">
                        <Typography id="gbl-connection-title" component="h2" variant="heading">{t('settings.connection.title')}</Typography>
                        <div className={styles.switchRow}>
                            <Switch id="gbl-activated"
                                    checked={form.activated}
                                    onChange={(event, value, checked) => setField('activated', checked)}/>
                            <label htmlFor="gbl-activated" className={styles.switchLabel}>
                                <Typography weight="bold">{t('settings.fields.activated.label')}</Typography>
                                <Typography variant="caption">{t('settings.fields.activated.helper')}</Typography>
                            </label>
                        </div>
                        <div className={styles.grid}>
                            {textField('username')}
                            <Field id="gbl-password-field"
                                   label={t('settings.fields.password.label') + (data.settings.passwordSet ? '' : ' *')}
                                   helper={t(data.settings.passwordSet ? 'settings.fields.password.helperStored' : 'settings.fields.password.helper')}
                                   hasError={Boolean(fieldError('password'))}
                                   errorMessage={fieldError('password')}
                            >
                                <Input id="gbl-password"
                                       aria-label={t('settings.fields.password.label')}
                                       type="password"
                                       autoComplete="new-password"
                                       value={form.password}
                                       placeholder={data.settings.passwordSet ? '••••••••' : ''}
                                       onChange={event => setField('password', event.target.value)}/>
                            </Field>
                            {TEXT_FIELDS.filter(field => field !== 'username').map(textField)}
                        </div>
                    </section>

                    <LanguagesCard data={data} mappings={mappings} onChangeMapping={(id, value) => setMappings(previous => ({...previous, [id]: value}))}/>

                    <ComponentsCard data={data} values={components} error={fieldError('components')} onChange={setComponents}/>
                </div>
            </LayoutContent>
        </div>
    );
};

export default SettingsPanel;
