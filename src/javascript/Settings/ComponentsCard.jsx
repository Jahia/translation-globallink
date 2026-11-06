import React, {useMemo, useState} from 'react';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import {Banner, ListSelector, RadioGroup, RadioItem, Typography} from '@jahia/moonstone';
import {MODULE_NAME} from '../constants';
import styles from './SettingsPanel.module.css';

const optionShape = PropTypes.shape({label: PropTypes.string, value: PropTypes.string});

/** The content types sent to GlobalLink, chosen among the editorial types or all types. */
export const ComponentsCard = ({data, values, error, onChange}) => {
    const {t} = useTranslation(MODULE_NAME);
    const [scope, setScope] = useState('editorial');

    // The stored choice stays listed whatever the scope, so switching scope never drops a selected type
    const options = useMemo(() => {
        const byValue = new Map();
        [...data.components.selected, ...data.components[scope]].forEach(option => byValue.set(option.value, option));
        return [...byValue.values()].sort((a, b) => a.label.localeCompare(b.label));
    }, [data, scope]);

    return (
        <section className={styles.card} aria-labelledby="gbl-components-title">
            <Typography id="gbl-components-title" component="h2" variant="heading">{t('settings.components.title')}</Typography>
            <Typography>{t('settings.components.help')}</Typography>
            <RadioGroup name="gbl-components-scope"
                        className={styles.scope}
                        value={scope}
                        onChange={(event, value) => setScope(value)}
            >
                <RadioItem id="gbl-scope-editorial" value="editorial" label={t('settings.components.editorial')}/>
                <RadioItem id="gbl-scope-all" value="all" label={t('settings.components.all')}/>
            </RadioGroup>
            {/* ListSelector replaces its own classes with a className: size it from a wrapper */}
            <div className={styles.listSelector}>
                <ListSelector options={options}
                              values={values}
                              label={{
                                  leftListTitle: t('settings.components.available'),
                                  rightListTitle: t('settings.components.selected'),
                                  addAllTitle: t('settings.components.addAll'),
                                  removeAllTitle: t('settings.components.removeAll'),
                                  selected: t('settings.components.count', {value: `${values.length}`})
                              }}
                              onChange={onChange}/>
            </div>
            {error && (
                <div role="alert">
                    <Banner variant="danger" title={error}>{t('settings.components.help')}</Banner>
                </div>
            )}
        </section>
    );
};

ComponentsCard.propTypes = {
    data: PropTypes.shape({
        components: PropTypes.shape({
            selected: PropTypes.arrayOf(optionShape).isRequired,
            editorial: PropTypes.arrayOf(optionShape).isRequired,
            all: PropTypes.arrayOf(optionShape).isRequired
        }).isRequired
    }).isRequired,
    values: PropTypes.arrayOf(PropTypes.string).isRequired,
    error: PropTypes.string,
    onChange: PropTypes.func.isRequired
};
