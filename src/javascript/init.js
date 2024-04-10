import React from 'react';
import {registry} from '@jahia/ui-extender';
import {GlobalLink} from '@jahia/moonstone';
import i18next from 'i18next';
import {CreateNewTranslationRequest} from './components/CreateNewTranslationRequest';

import {useCreateFormDefinition, useContentEditorConfigContext} from '@jahia/jcontent';

const MODULE_NAME = 'jahia-translation-globallink';
export default function () {
    registry.add('callback', 'translation-globallink', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces(MODULE_NAME);
            register();
            console.log('%c Jahia GlobalLink Translation Connector is activated', 'color: #3c8cba');
        }
    });
}

const register = () => {
    registry.addOrReplace('action', 'createTranslation', {
        buttonIcon: <GlobalLink/>,
        buttonLabel: `${MODULE_NAME}:label.requestATranslation`,
        showOnNodeTypes: ['jnt:page'],
        targets: ['contentActions:3.10'],
        component: CreateNewTranslationRequest
    });

    registry.add('adminRoute', 'translation-globallink-settings', {
        targets: ['administration-sites:110'],
        icon: <GlobalLink/>,
        label: `${MODULE_NAME}:label.settings`,
        isSelectable: true,
        requiredPermission: 'adminGlobalLinkTranslation',
        requireModuleInstalledOnSite: MODULE_NAME,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-settings.html'
    });

    registry.add('content-editor-config', 'gblnt:globalLinkProject', {
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
            if (editorContext.nodeTypeName === 'gblnt:globalLinkProject' && currentValue === 'dummyTarget') {
                const {setFieldValue} = editorContext.formik;
                setFieldValue('gblnt:globalLinkProject_targetNode', editorContext.nodeData.uuid, false);
            }
        }
    });

    registry.add('adminRoute', 'translation-globallink-requests', {
        targets: ['jcontent:23'],
        icon: <GlobalLink/>,
        label: `${MODULE_NAME}:label.requests`,
        isSelectable: true,
        requiredPermission: 'adminGlobalLinkTranslation',
        requireModuleInstalledOnSite: MODULE_NAME,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-requests.html'
    });
};
