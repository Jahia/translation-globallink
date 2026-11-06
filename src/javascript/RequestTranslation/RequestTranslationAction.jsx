import React from 'react';
import PropTypes from 'prop-types';
import {gql, useQuery} from '@apollo/client';
import {useNodeChecks} from '@jahia/data-helper';
import {MODULE_NAME, PROJECT_TYPE, REQUESTS_FOLDER} from '../constants';
import {useSiteContext} from '../utils/useSiteContext';

const REQUEST_FOLDER_QUERY = gql`
    query GlobalLinkRequestFolder($sitePath: String!, $folderName: String!) {
        jcr {
            nodeByPath(path: $sitePath) {
                uuid
                workspace
                activated: property(name: "j:globalLinkActivated") {
                    value
                }
                folder: children(names: [$folderName]) {
                    nodes {
                        uuid
                        workspace
                        path
                    }
                }
            }
        }
    }
`;

/**
 * "Request a translation" on a page: opens Content Editor on a new GlobalLink request for that page.
 * Shown on sites where the module is installed and GlobalLink is switched on in Site Settings.
 */
export const RequestTranslationAction = ({path, render: Render, loading: Loading, ...otherProps}) => {
    const {siteKey, language, uilang} = useSiteContext();
    const checks = useNodeChecks(
        {path, language},
        {requireModuleInstalledOnSite: [MODULE_NAME], showOnNodeTypes: ['jnt:page']}
    );
    const {data, loading} = useQuery(REQUEST_FOLDER_QUERY, {
        variables: {sitePath: `/sites/${siteKey}`, folderName: REQUESTS_FOLDER},
        skip: !checks.checksResult
    });

    if (checks.loading || loading) {
        return Loading ? <Loading {...otherProps}/> : null;
    }

    const site = data?.jcr?.nodeByPath;
    const folder = site?.folder?.nodes?.[0];
    const activated = site?.activated?.value === 'true';
    if (checks.error || !checks.checksResult || !folder || !activated) {
        return null;
    }

    const onClick = () => globalThis.CE_API?.create?.({
        targetNodeId: checks.node.uuid,
        path: folder.path,
        site: siteKey,
        uilang,
        lang: language,
        nodeTypes: [PROJECT_TYPE],
        includeSubTypes: false,
        isFullscreen: false,
        keepSectionsState: true,
        configName: PROJECT_TYPE
    });

    return <Render {...otherProps} path={path} onClick={onClick}/>;
};

RequestTranslationAction.propTypes = {
    path: PropTypes.string.isRequired,
    render: PropTypes.func.isRequired,
    loading: PropTypes.func
};
