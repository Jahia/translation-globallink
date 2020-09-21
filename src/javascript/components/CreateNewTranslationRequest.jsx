import React from 'react';
import * as PropTypes from 'prop-types';
import {useSelector} from 'react-redux';
import {useNodeChecks} from '@jahia/data-helper';

export const CreateNewTranslationRequest = ({context, render: Render, loading: Loading, ...otherProps}) => {
    const {language, siteKey, uilang} = useSelector(state => ({language: state.language, siteKey: state.site, uilang: state.uilang}));

    const res = useNodeChecks(
        {path: context.path, language: language},
        {
            requireModuleInstalledOnSite: ['jahia-translation-globallink'],
            getPrimaryNodeType: true

        }
    );

    if (Loading && (res.loading)) {
        return <Loading context={context}/>;
    }

    if (res.error) {
        console.error('Error while loading globallink translations.com', res.error);
        return <Render context={{...context, isVisible: false}}/>;
    }

    if (res.checksResult && !window.globallinkFolderId[siteKey]) {
        console.log('No Translations globalling config found for site ', siteKey);
    }

    // Use onClick to open content editor using the URL.
    // URL : const onClick = () => history.push(`/content-editor/${language}/create/${nodeInfo.node.uuid}/gblnt:globalLinkProject`);
    // Use onClick to open the modal window
    const onClick = () => window.CE_API.create(res.node.uuid, context.path, siteKey, language, uilang, ['gblnt:globalLinkProject'], [], false);

    // In case context is available
    if (context) {
        return (
            <Render context={{
            ...context,
            isVisible: res.checksResult && Boolean(window.globallinkFolderId[siteKey]),
            onClick: onClick
        }}/>
        );
    }

    // In case no context (new Action framework props propagation).
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
    context: PropTypes.object.isRequired,
    render: PropTypes.func.isRequired,
    loading: PropTypes.func
};
