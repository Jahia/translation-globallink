import org.jahia.services.content.nodetypes.ExtendedNodeType
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition
import org.jahia.services.content.nodetypes.NodeTypeRegistry
import org.jahia.services.templates.ComponentRegistry
import org.jahia.translation.globallink.util.GlobalLinkUtil

import javax.jcr.Node
import javax.jcr.PropertyType

def siteNode = renderContext.mainResource.node.session.getNodeByIdentifier(renderContext.request.parameterMap['identifier'][0])
def mapTypes = ComponentRegistry.getComponentTypes(siteNode, null,
        GlobalLinkUtil.getExcludedComponents(siteNode.hasProperty('j:componentsList')?siteNode.getProperty('j:componentsList').getValues():null), renderContext.mainResourceLocale)

def componentType = (renderContext.request.parameterMap['componentType'][0] == "all") ? "nt:base":"jmix:editorialContent";

def filteredMap = [:];

mapTypes.each { key, value ->
    ExtendedNodeType extendedNodeType = NodeTypeRegistry.getInstance().getNodeType(key);
    if(extendedNodeType.isNodeType(componentType)) {
            Map<String, ExtendedPropertyDefinition> definitions = extendedNodeType.getPropertyDefinitionsAsMap();
            for (Map.Entry<String, ExtendedPropertyDefinition> definitionEntry : definitions.entrySet()) {
                ExtendedPropertyDefinition value1 = definitionEntry.getValue();
                if (!value1.isProtected() && value1.isInternationalized() && value1.getRequiredType() == PropertyType.STRING) {
                    filteredMap[key] = value;
                    break;
                }
            }
        }
};
print "["
filteredMap.eachWithIndex { key, value, idx ->
    print "{\"key\":\""+key+"\",\"value\":\""+value+"\"}"+(idx<(filteredMap.size()-1)?",":"")
}
print "]"