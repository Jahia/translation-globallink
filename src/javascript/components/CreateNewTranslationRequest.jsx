import React from 'react';
import * as PropTypes from 'prop-types';
import {useSelector} from 'react-redux';
import {useNodeChecks} from '@jahia/data-helper';

export const CreateNewTranslationRequest = ({render: Render, loading: Loading, ...otherProps}) => {
    const {language, siteKey} = useSelector(state => ({language: state.language, siteKey: state.site}));

    const res = useNodeChecks(
        {path: otherProps.path, language: language},
        {
            requireModuleInstalledOnSite: ['jahia-translation-globallink'],
            getPrimaryNodeType: true,
            showOnNodeTypes: otherProps.showOnNodeTypes
        }
    );

    if (Loading && (res.loading)) {
        return <Loading {...otherProps}/>;
    }

    if (res.error) {
        console.error('Error while loading globallink translations.com', res.error);
        return <Render {...otherProps} isVisible={false}/>;
    }

    if (res.checksResult && !window.globallinkFolder[siteKey]) {
        console.log('No Translations globalling config found for site ', siteKey);
    }

    // Use onClick to open content editor using the URL.
    // URL : const onClick = () => history.push(`/content-editor/${language}/create/${nodeInfo.node.uuid}/gblnt:globalLinkProject`);
    // Use onClick to open the modal window
    const onClick = () => window.CE_API.create({
        targetNodeId: res.node.uuid,
        path: window.globallinkFolder[siteKey].path,
        site: siteKey,
        uilang: window.contextJsParameters.uilang,
        lang: language,
        nodeTypes: ['gblnt:globalLinkProject'],
        includeSubTypes: false,
        isFullscreen: false,
        keepSectionsState: true,
        configName: 'gblnt:globalLinkProject'
    });

    return (
        <Render
            isVisible={res.checksResult && Boolean(window.globallinkFolder[siteKey])}
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
