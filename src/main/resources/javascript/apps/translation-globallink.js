window.jahia.uiExtender.registry.add('callback', 'translation-Globallink', {
    targets: ['jahiaApp-init:3'],
    callback: () => Promise.all([
        window.jahia.uiExtender.registry.addOrReplace('action', 'createTranslation', {
            ...window.jahia.uiExtender.registry.get('action', 'createContent'),
            showOnNodeTypes: [],
            hideOnNodeTypes: []
        }, {
            buttonIcon: window.jahia.moonstone.toIconComponent(`<svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                                    <g clip-path="url(#clip0)">
                                                                        <path fill-rule="evenodd" clip-rule="evenodd" d="M5.14749 1.02954L4.12931 1.02954V3.11503L2.93165 1.81104L2.21169 2.59492L3.40896 3.8985L1.49399 3.8985L1.49399 5.02042L4.12931 5.02042H5.14749H5.15941V3.8985H5.14749L5.14749 1.02954ZM5.95419 7.04129V5.91939H5.96647H9.61957V7.04138L7.70466 7.04129L8.90187 8.34495L8.18199 9.12878L6.98466 7.82513V9.9103H5.96647L5.95419 7.04129Z" fill="black"/>
                                                                        <path d="M6.97213 2.9868V1.02953L5.95402 1.02953L5.95402 3.8985L5.95386 5.02042H6.97213H9.61924V3.8985L7.80463 3.8985C7.80463 3.8985 10.2857 1.14286 12 0C10.2857 0.571429 6.97213 2.9868 6.97213 2.9868Z" fill="black"/>
                                                                        <path d="M4.12922 7.9349V9.9103H5.15963V5.91939H4.12922L1.49399 5.91939L1.49399 7.02798H3.29229C3.29229 7.02798 0.571429 10.2857 0 12C1.14286 10.2857 4.12922 7.9349 4.12922 7.9349Z" fill="black"/>
                                                                    </g>
                                                                    <defs>
                                                                        <clipPath id="clip0">
                                                                            <rect width="12" height="12" fill="white"/>
                                                                        </clipPath>
                                                                    </defs>
                                                                </svg>`),
            nodeTypes: ['gblnt:globalLinkProject'],
            openEditor: true,
            buttonLabel: 'jahia-translation-globallink:label.requestATranslation',
            targets: ['contentActions:3.10'],
            showOnNodeTypes: ['jnt:page'],
            hideOnNodeTypes: ['jnt:contentFolder', 'jnt:content']
        })
    ])
});

window.jahia.i18n.loadNamespaces('jahia-translation-globallink');

window.jahia.uiExtender.registry.add('adminRoute', 'translation-globallink-settings', {
    targets: ['administration-sites:110'],
    icon: window.jahia.moonstone.toIconComponent('GlobalLink'),
    label: 'jahia-translation-globallink:label.settings',
    isSelectable: true,
    requiredPermission: 'adminGlobalLinkTranslation',
    requireModuleInstalledOnSite: 'jahia-translation-globallink',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-settings.html'
});


window.jahia.uiExtender.registry.add('adminRoute', 'translation-globallink-requests', {
    targets: ['jcontent:23'],
    icon: window.jahia.moonstone.toIconComponent('GlobalLink'),
    label: 'jahia-translation-globallink:label.requests',
    isSelectable: true,
    requiredPermission: 'adminGlobalLinkTranslation',
    requireModuleInstalledOnSite: 'jahia-translation-globallink',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-requests.html'
});
console.debug('%c GLobalLink translation is activated', 'color: #3c8cba');

