window.jahia.i18n.loadNamespaces('jahia-translation-globallink');

window.jahia.uiExtender.registry.add('adminRoute', 'translation-globallink', {
    targets: ['jcontent:23'],
    icon: window.jahia.moonstone.toIconComponent('GlobalLink'),
    label: 'jahia-translation-globallink:label.title',
    isSelectable: false,
    requiredPermission: 'adminGlobalLinkTranslation',
    requireModuleInstalledOnSite: 'jahia-translation-globallink',
});

window.jahia.uiExtender.registry.add('adminRoute', 'translation-globallink-settings', {
    targets: ['jcontent-translation-globallink:1'],
    icon: window.jahia.moonstone.toIconComponent('GlobalLink'),
    label: 'jahia-translation-globallink:label.settings',
    isSelectable: true,
    requiredPermission: 'adminGlobalLinkTranslation',
    requireModuleInstalledOnSite: 'jahia-translation-globallink',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-settings.html'
});


window.jahia.uiExtender.registry.add('adminRoute', 'translation-globallink-requests', {
    targets: ['jcontent-translation-globallink:2'],
    icon: window.jahia.moonstone.toIconComponent('GlobalLink'),
    label: 'jahia-translation-globallink:label.requests',
    isSelectable: true,
    requiredPermission: 'adminGlobalLinkTranslation',
    requireModuleInstalledOnSite: 'jahia-translation-globallink',
    iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.globallink-translation-requests.html'
});
