import React, {useMemo, useState} from 'react';
import {useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {useNotifications} from '@jahia/react-material';
import {
    Banner,
    Button,
    Chip,
    DataTable,
    Delete,
    Download,
    EmptyData,
    Header,
    LayoutContent,
    Loader,
    Modal,
    ModalBody,
    ModalFooter,
    ModalHeader,
    NumberInput,
    Reload,
    SearchInput,
    Typography,
    Visibility
} from '@jahia/moonstone';
import {MODULE_NAME} from '../constants';
import {callAction} from '../utils/actions';
import {download, exportFileName, toCsv} from '../utils/exports';
import {LogoTitle} from '../GlobalLinkLogo/GlobalLinkLogo';
import {useSiteContext} from '../utils/useSiteContext';
import {matches, olderThan, REQUESTS_QUERY, STATUS_COLORS, toRows} from './requests';
import styles from './RequestsApp.module.css';

const DEFAULT_CLEAN_DAYS = '30';
const contextPath = () => globalThis.contextJsParameters?.contextPath || '';

const formatDate = (iso, locale) => {
    if (!iso) {
        return '';
    }

    const date = new Date(iso);
    return Number.isNaN(date.getTime()) ? iso : date.toLocaleString(locale, {dateStyle: 'medium', timeStyle: 'short'});
};

const byDate = key => (a, b) => (a[key] || '').localeCompare(b[key] || '');

export const RequestsApp = () => {
    const {t} = useTranslation(MODULE_NAME);
    const {siteKey, language, uilang} = useSiteContext();
    const notifications = useNotifications();
    const notify = message => notifications?.notify?.(message, ['closeButton']);
    const [search, setSearch] = useState('');
    const [cleanDays, setCleanDays] = useState(DEFAULT_CLEAN_DAYS);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [cleaning, setCleaning] = useState(false);

    const sitePath = `/sites/${siteKey}`;
    const {data, loading, error, refetch, networkStatus} = useQuery(REQUESTS_QUERY, {
        variables: {sitePath, language},
        fetchPolicy: 'no-cache',
        errorPolicy: 'all',
        notifyOnNetworkStatusChange: true
    });

    const statusLabel = status => t(`requests.status.${status}`, {defaultValue: status});
    const rows = useMemo(() => toRows(data?.jcr?.nodesByCriteria?.nodes, siteKey), [data, siteKey]);
    const visibleRows = rows.filter(row => matches(row, search, statusLabel(row.status)));
    const days = Number.parseInt(cleanDays, 10);
    const daysValid = Number.isInteger(days) && days >= 0;
    const toClean = daysValid ? olderThan(rows, days) : [];
    const firstLoad = loading && !data;
    const reloading = networkStatus === 4;

    const exportCsv = () => {
        const header = ['name', 'ticket', 'page', 'pagePath', 'source', 'targets', 'targetDetails', 'words', 'contentCount', 'created', 'createdBy', 'dueDate', 'status', 'error'];
        const lines = rows.map(row => [
            row.name,
            row.ticket,
            row.page,
            row.pagePath,
            row.source,
            row.targets.join(' '),
            row.targetDetails.map(target => `${target.locale}:${target.ticket}:${target.words ?? ''}`).join(' '),
            row.words,
            row.contentCount,
            row.created,
            row.createdBy,
            row.dueDate,
            statusLabel(row.status),
            row.error
        ]);
        download(toCsv(header, lines), 'text/csv;charset=utf-8', exportFileName('globallink-requests', siteKey, language, 'csv'));
    };

    const exportJson = () => {
        const requests = rows.map(({jcontentPath, skipped, ...row}) => row);
        const report = {generatedAt: new Date().toISOString(), site: siteKey, language, requests};
        download(JSON.stringify(report, null, 2), 'application/json', exportFileName('globallink-requests', siteKey, language, 'json'));
    };

    const clean = async () => {
        setCleaning(true);
        try {
            const result = await callAction({siteKey, language, action: 'globalLinkSubmissionClean', method: 'POST', params: {daysOld: String(days)}});
            notify(t('requests.clean.done', {value: `${result.deleted ?? 0}`}));
        } catch {
            notify(t('requests.clean.failed'));
        }

        setCleaning(false);
        setConfirmOpen(false);
        refetch();
    };

    const openDetails = row => globalThis.CE_API?.edit?.({uuid: row.id, site: siteKey, lang: language, uilang});

    const columns = [
        {
            key: 'name',
            label: t('requests.columns.name'),
            width: '20%',
            isSortable: true,
            sortFn: (a, b) => a.name.localeCompare(b.name),
            render: ({data: row}) => (
                <div className={styles.cellText}>
                    <Typography weight="bold">{row.name || '-'}</Typography>
                    {row.ticket && <Typography variant="caption" title={row.ticket}>{row.ticket}</Typography>}
                </div>
            )
        },
        {
            key: 'page',
            label: t('requests.columns.page'),
            width: '16%',
            isSortable: true,
            sortFn: (a, b) => a.page.localeCompare(b.page),
            render: ({data: row}) => (
                <div className={styles.cellText}>
                    {row.jcontentPath ? (
                        <a className={styles.link} href={`${contextPath()}/jahia/jcontent/${encodeURIComponent(siteKey)}/${encodeURIComponent(language)}/${row.jcontentPath}`}>
                            {row.page || row.pagePath}
                        </a>
                    ) : <Typography>{t('requests.missingPage')}</Typography>}
                </div>
            )
        },
        {
            key: 'source',
            label: t('requests.columns.languages'),
            width: '16%',
            render: ({data: row}) => (
                <div className={styles.languages}>
                    <Typography>{row.source}</Typography>
                    <Typography aria-hidden="true">→</Typography>
                    {row.targets.map(target => <Chip key={target} label={target}/>)}
                </div>
            )
        },
        {
            key: 'created',
            label: t('requests.columns.created'),
            width: '13%',
            isSortable: true,
            sortFn: byDate('created'),
            render: ({data: row}) => (
                <div className={styles.cellText}>
                    <Typography>{formatDate(row.created, uilang)}</Typography>
                    <Typography variant="caption">{row.createdBy}</Typography>
                </div>
            )
        },
        {
            key: 'dueDate',
            label: t('requests.columns.dueDate'),
            width: '10%',
            isSortable: true,
            sortFn: byDate('dueDate'),
            render: ({value}) => <div className={styles.cellText}><Typography>{formatDate(value, uilang)}</Typography></div>
        },
        {
            key: 'words',
            label: t('requests.columns.volume'),
            width: '9%',
            render: ({data: row}) => (
                <div className={styles.cellText}>
                    <Typography>{row.words === null ? '-' : t('requests.words', {value: `${row.words}`})}</Typography>
                    {row.contentCount !== null && <Typography variant="caption">{t('requests.contents', {value: `${row.contentCount}`})}</Typography>}
                </div>
            )
        },
        {
            key: 'status',
            label: t('requests.columns.status'),
            width: '11%',
            isSortable: true,
            sortFn: (a, b) => statusLabel(a.status).localeCompare(statusLabel(b.status)),
            render: ({data: row}) => (
                <div className={styles.cellText}>
                    <span title={row.error || undefined}><Chip color={STATUS_COLORS[row.status] || 'default'} label={statusLabel(row.status)}/></span>
                    {row.error && <Typography variant="caption" className={styles.errorText}>{row.error}</Typography>}
                </div>
            )
        },
        {
            key: 'id',
            label: '',
            width: '5%',
            align: 'right',
            render: ({data: row}) => (
                <Button variant="ghost"
                        icon={<Visibility/>}
                        aria-label={t('requests.view', {name: row.name})}
                        title={t('requests.view', {name: row.name})}
                        onClick={() => openDetails(row)}/>
            )
        }
    ];

    const header = (
        <Header title={<LogoTitle>{t('requests.title')}</LogoTitle>}
                mainActions={[
                    <Button key="reload" size="big" variant="ghost" icon={<Reload/>} label={t('common.reload')} isLoading={reloading} onClick={() => refetch()}/>,
                    <Button key="csv" size="big" variant="outlined" icon={<Download/>} label={t('common.exportCsv')} isDisabled={!data || rows.length === 0} onClick={exportCsv}/>,
                    <Button key="json" size="big" variant="outlined" icon={<Download/>} label={t('common.exportJson')} isDisabled={!data || rows.length === 0} onClick={exportJson}/>
                ]}/>
    );

    return (
        <div className={styles.root} data-gbl-role="requests">
            <LayoutContent hasPadding header={header}>
                <div className={styles.content}>
                    {error && (
                        <Banner variant="danger" title={t('requests.loadError')}>{error.message}</Banner>
                    )}
                    <section className={styles.card} aria-label={t('requests.title')}>
                        <div className={styles.toolbar}>
                            <div className={styles.searchGroup}>
                                <div className={styles.search}>
                                    <SearchInput value={search}
                                                 placeholder={t('requests.search')}
                                                 aria-label={t('requests.search')}
                                                 onChange={event => setSearch(event.target.value)}
                                                 onClear={() => setSearch('')}/>
                                </div>
                                <Typography role="status">
                                    {search ? t('requests.countFiltered', {shown: `${visibleRows.length}`, total: `${rows.length}`}) : t('requests.count', {total: `${rows.length}`})}
                                </Typography>
                            </div>
                            <div className={styles.clean}>
                                <label htmlFor="gbl-clean-days"><Typography>{t('requests.clean.label')}</Typography></label>
                                <NumberInput id="gbl-clean-days"
                                             className={styles.days}
                                             value={cleanDays}
                                             min={0}
                                             onChange={event => setCleanDays(event.target.value)}/>
                                <Typography>{t('requests.clean.days')}</Typography>
                                <Button variant="outlined"
                                        color="danger"
                                        icon={<Delete/>}
                                        label={t('requests.clean.button')}
                                        isDisabled={!daysValid || toClean.length === 0}
                                        onClick={() => setConfirmOpen(true)}/>
                            </div>
                        </div>
                        {firstLoad && <div className={styles.loading}><Loader size="big"/></div>}
                        {!firstLoad && rows.length === 0 && (
                            <EmptyData title={t('requests.empty.title')} message={t('requests.empty.message')}/>
                        )}
                        {!firstLoad && rows.length > 0 && (
                            <div className={styles.tableBox}>
                                <DataTable enableSorting
                                           enablePagination
                                           className={styles.table}
                                           primaryKey="id"
                                           data={visibleRows}
                                           columns={columns}
                                           defaultSortBy="created"
                                           defaultSortDirection="descending"
                                           defaultItemsPerPage={25}
                                           itemsPerPageOptions={[10, 25, 50, 100]}
                                           i18n={{itemsPerPage: t('common.itemsPerPage'), of: t('common.of')}}/>
                            </div>
                        )}
                    </section>
                </div>
            </LayoutContent>
            <Modal isOpen={confirmOpen} size="medium" onOpenChange={open => !cleaning && setConfirmOpen(open)}>
                <div>
                    <ModalHeader title={t('requests.clean.confirmTitle')}/>
                    <ModalBody>
                        <Typography>{t('requests.clean.confirmMessage', {value: `${toClean.length}`, days: `${days}`})}</Typography>
                    </ModalBody>
                    <ModalFooter>
                        <Button label={t('common.cancel')} isDisabled={cleaning} onClick={() => setConfirmOpen(false)}/>
                        <Button color="danger" icon={<Delete/>} label={t('requests.clean.confirmButton')} isLoading={cleaning} onClick={clean}/>
                    </ModalFooter>
                </div>
            </Modal>
        </div>
    );
};

export default RequestsApp;
