<%@ page import="org.jahia.services.content.decorator.JCRSiteNode"%>
    <%@ page import="org.jahia.services.sites.JahiaSitesService"%>
    <%@ page import="static org.jahia.translation.globallink.common.GlobalLinkConstants.NODE_NAME_PROJECT_REQUESTS"%><%@ page import="javax.jcr.RepositoryException"%><%@ page import="org.slf4j.LoggerFactory"%><%@ page import="org.jahia.translation.globallink.action.GlobalLinkConfigAction"%>
    <%@ page language="java" contentType="text/javascript" %>
    <%
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
    %>
    window.globallinkFolder = {};
<%
try {
    for (JCRSiteNode site : JahiaSitesService.getInstance().getSitesNodeList()) {
%>
    window.globallinkFolder['<%=site.getName()%>'] = {};
    window.globallinkFolder['<%=site.getName()%>'].uuid = '<%=site.hasNode(NODE_NAME_PROJECT_REQUESTS) ? site.getNode(NODE_NAME_PROJECT_REQUESTS).getIdentifier() : ""%>'
    window.globallinkFolder['<%=site.getName()%>'].path = '<%=site.hasNode(NODE_NAME_PROJECT_REQUESTS) ? site.getNode(NODE_NAME_PROJECT_REQUESTS).getPath() : ""%>'
<%
}} catch (Exception e) {
    LoggerFactory.getLogger("translation-globallink.jsp").error("Unable to init folders IDs", e);
}%>
