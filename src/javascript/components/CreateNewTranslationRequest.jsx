import React from 'react';
import * as PropTypes from 'prop-types';
import {useSelector} from 'react-redux';
import {useNodeChecks} from '@jahia/data-helper';

export const CreateNewTranslationRequest = ({render: Render, loading: Loading, ...otherProps}) => {
    const {language, siteKey, uilang} = useSelector(state => ({language: state.language, siteKey: state.site, uilang: state.uilang}));

    const res = useNodeChecks(
        {path: otherProps.path, language: language},
        {
            requireModuleInstalledOnSite: ['jahia-translation-globallink'],
            getPrimaryNodeType: true

        }
    );

    if (Loading && (res.loading)) {
        return <Loading {...otherProps}/>;
    }

    if (res.error) {
        console.error('Error while loading globallink translations.com', res.error);
        return <Render {...otherProps} isVisible={false}/>;
    }

    if (res.checksResult && !window.globallinkFolderId[siteKey]) {
        console.log('No Translations globalling config found for site ', siteKey);
    }

    // Use onClick to open content editor using the URL.
    // URL : const onClick = () => history.push(`/content-editor/${language}/create/${nodeInfo.node.uuid}/gblnt:globalLinkProject`);
    // Use onClick to open the modal window
    const onClick = () => window.CE_API.create(res.node.uuid, otherProps.path, siteKey, language, uilang, ['gblnt:globalLinkProject'], [], false);

    return (
        <Render
            isVisible={res.checksResult && Boolean(window.globallinkFolderId[siteKey])}
            onClick={onClick}
            {...otherProps}
        />
    );
};

CreateNewTranslationRequest.defaultProps = {
    loading: undefined
};

CreateNewTranslationRequest.propTypes = {
    render: PropTypes.func.isRequired,
    loading: PropTypes.func
};
