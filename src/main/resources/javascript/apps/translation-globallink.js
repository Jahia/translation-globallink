window.jahia.uiExtender.registry.add('callback', 'translation-Globallink', {
    targets: ['jahiaApp-init:3'],
    callback: () => Promise.all([
        window.jahia.uiExtender.registry.addOrReplace('action', 'createTranslation', {
            ...window.jahia.uiExtender.registry.get('action', 'createContent'),
            showOnNodeTypes: [],
            hideOnNodeTypes: []
        }, {
            buttonIcon: window.jahia.moonstone.toIconComponent('GlobalLink'),
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

