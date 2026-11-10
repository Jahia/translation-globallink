import React, {lazy, Suspense} from 'react';
import PropTypes from 'prop-types';
import {registry} from '@jahia/ui-extender';
import {GlobalLink, LayoutContent, Loader} from '@jahia/moonstone';
import i18next from 'i18next';
import {useCreateFormDefinition, useContentEditorConfigContext} from '@jahia/jcontent';
import {MODULE_NAME, PERMISSION, PROJECT_TYPE} from './constants';
import {RequestTranslationAction} from './RequestTranslation/RequestTranslationAction';
import {useSiteContext} from './utils/useSiteContext';

// Screens load on first display only: jContent's start stays light for every editor
const SettingsPanel = lazy(() => import('./Settings/SettingsPanel'));
const RequestsApp = lazy(() => import('./Requests/RequestsApp'));

const Lazy = ({children}) => (
    <Suspense fallback={<LayoutContent isLoading content={<Loader size="big"/>}/>}>{children}</Suspense>
);

Lazy.propTypes = {
    children: PropTypes.node.isRequired
};

// Both screens remount with fresh state when the site changes
const SiteSettings = () => {
    const {siteKey} = useSiteContext();
    return <Lazy><SettingsPanel key={siteKey}/></Lazy>;
};

const Requests = () => {
    const {siteKey} = useSiteContext();
    return <Lazy><RequestsApp key={siteKey}/></Lazy>;
};

const register = () => {
    registry.addOrReplace('action', 'createTranslation', {
        buttonIcon: <GlobalLink/>,
        buttonLabel: `${MODULE_NAME}:label.requestATranslation`,
        targets: ['accordionContentActions:12', 'contentItemActions:12', 'contentItemContextActions:12'],
        component: RequestTranslationAction
    });

    registry.add('adminRoute', 'translation-globallink-settings', {
        targets: ['administration-sites:110'],
        icon: <GlobalLink/>,
        label: `${MODULE_NAME}:label.settings`,
        isSelectable: true,
        requiredPermission: PERMISSION,
        requireModuleInstalledOnSite: MODULE_NAME,
        render: () => <SiteSettings/>
    });

    registry.add('adminRoute', 'translation-globallink-requests', {
        targets: ['jcontent:23'],
        icon: <GlobalLink/>,
        label: `${MODULE_NAME}:label.requests`,
        isSelectable: true,
        requiredPermission: PERMISSION,
        requireModuleInstalledOnSite: MODULE_NAME,
        render: () => <Requests/>
    });

    registry.add('content-editor-config', PROJECT_TYPE, {
        useFormDefinition: () => {
            const {data, refetch, loading, error} = useCreateFormDefinition();
            const contentEditorConfigContext = useContentEditorConfigContext();

            if (data && contentEditorConfigContext.targetNodeId) {
                data.initialValues['gblnt:globalLinkProject_targetNode'] = contentEditorConfigContext.targetNodeId;
            }

            return {data, refetch, loading, error};
        }
    });

    registry.add('selectorType.onChange', 'globalLink', {
        // 'ContentPicker' is for content editor v3 compatibility.
        targets: ['Picker', 'ContentPicker'],
        onChange: (previousValue, currentValue, field, editorContext) => {
            if (editorContext.nodeTypeName === PROJECT_TYPE && currentValue === 'dummyTarget') {
                const {setFieldValue} = editorContext.formik;
                setFieldValue('gblnt:globalLinkProject_targetNode', editorContext.nodeData.uuid, false);
            }
        }
    });
};

export default function () {
    registry.add('callback', 'translation-globallink', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces(MODULE_NAME);
            register();
        }
    });
}
