import React from 'react';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import {Chip, DataTable, Dropdown, Typography} from '@jahia/moonstone';
import {MODULE_NAME} from '../constants';
import styles from './SettingsPanel.module.css';

/** Language directions of the GlobalLink connector, and the GlobalLink locale of each site language. */
export const LanguagesCard = ({data, mappings, onChangeMapping}) => {
    const {t} = useTranslation(MODULE_NAME);
    const connectionOk = data.connection.state === 'OK';
    const localeOptions = [{label: t('settings.mapping.none'), value: ''}, ...data.globalLinkLocales];
    const rows = data.siteLanguages.map(siteLanguage => ({
        id: siteLanguage.value,
        language: siteLanguage.label,
        mapping: mappings[siteLanguage.value] || ''
    }));

    return (
        <section className={styles.card} aria-labelledby="gbl-languages-title">
            <Typography id="gbl-languages-title" component="h2" variant="heading">{t('settings.languages.title')}</Typography>
            {connectionOk ? (
                <>
                    <div>
                        <Typography weight="bold">{t('settings.languages.directions')}</Typography>
                        <div className={styles.chips}>
                            {data.connection.directions.length === 0 && <Typography>{t('settings.languages.noDirection')}</Typography>}
                            {data.connection.directions.map(direction => (
                                <Chip key={`${direction.source}-${direction.target}`}
                                      color="accent"
                                      label={`${direction.source} → ${direction.target}`}/>
                            ))}
                        </div>
                    </div>
                    <Typography>{t('settings.languages.mappingHelp')}</Typography>
                    <div className={styles.tableBox}>
                        <DataTable className={styles.mappingTable}
                                   primaryKey="id"
                                   data={rows}
                                   enablePagination={false}
                                   columns={[
                                       {key: 'language', label: t('settings.languages.siteLanguage'), width: '40%'},
                                       {
                                           key: 'mapping',
                                           label: t('settings.languages.globalLinkLocale'),
                                           width: '60%',
                                           render: ({id, value}) => (
                                               <Dropdown className={styles.dropdown}
                                                         data={localeOptions}
                                                         value={value}
                                                         hasSearch={localeOptions.length > 8}
                                                         onChange={(event, item) => onChangeMapping(id, item?.value || '')}/>
                                           )
                                       }
                                   ]}/>
                    </div>
                </>
            ) : (
                <Typography>{t('settings.languages.unavailable')}</Typography>
            )}
        </section>
    );
};

LanguagesCard.propTypes = {
    data: PropTypes.shape({
        connection: PropTypes.shape({
            state: PropTypes.string.isRequired,
            directions: PropTypes.arrayOf(PropTypes.shape({source: PropTypes.string, target: PropTypes.string})).isRequired
        }).isRequired,
        globalLinkLocales: PropTypes.arrayOf(PropTypes.shape({label: PropTypes.string, value: PropTypes.string})).isRequired,
        siteLanguages: PropTypes.arrayOf(PropTypes.shape({label: PropTypes.string, value: PropTypes.string})).isRequired
    }).isRequired,
    mappings: PropTypes.objectOf(PropTypes.string).isRequired,
    onChangeMapping: PropTypes.func.isRequired
};
