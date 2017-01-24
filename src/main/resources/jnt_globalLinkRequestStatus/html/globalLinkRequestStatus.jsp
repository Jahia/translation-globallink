<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="gbl" uri="http://jahia.com/translation/globallink/1.0" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<c:set var="site" value="${renderContext.mainResource.node.resolveSite}"/>
<jcr:sql var="gblRequests"
         sql="select * from [jnt:globalLinkProject] where isdescendantnode(['${site.path}']) order by [jcr:created] desc"/>

<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<template:addResources type="css" resources="bootstrap.min.css"/>
<template:addResources type="css" resources="datatables.min.css"/>
<template:addResources type="javascript" resources="bootstrap.min.js"/>
<template:addResources type="javascript" resources="moment.min.js"/>
<template:addResources type="javascript" resources="datatables.min.js"/>
<template:addResources type="javascript" resources="datetime-moment.js"/>
<div class="row" style="margin: 0">
    <div class="col-md-12" style="margin: 0">
        <img src="<c:url value='/modules/jahia-translation-globallink/img/globalLink.png'/>" width="100px" style="margin: 0">
    </div>
</div>
<div class="row">
    <div class="col-lg-12">
        <table class="table table-striped nowrap" id="request-list">
            <thead>
            <tr>
                <th><fmt:message key="request.page"/></th>
                <th><fmt:message key="request.date"/></th>
                <th><fmt:message key="request.date.submitted"/></th>
                <th class="nowrap" data-orderable="false"><fmt:message key="request.language"/></th>
                <th><fmt:message key="request.tickets"/></th>
                <th data-orderable="false"><fmt:message key="request.target"/></th>
                <th><fmt:message key="request.status"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${gblRequests.nodes}" var="gblRequest" varStatus="index">
                <c:choose>
                    <c:when test="${gblRequest.properties['skipTranslated'].boolean && gblRequest.properties['gblContentCount'].long == 0}">
                    </c:when>
                    <c:otherwise>
                        <tr>
                            <td>
                                <a href="<c:url value="${url.baseEdit}${gblRequest.parent.path}.html"/>"
                                   target="_blank">
                                        ${gblRequest.parent.displayableName}
                                </a>
                            </td>
                            <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm"
                                                value="${gblRequest.properties['dueDate'].time}"/></td>
                            <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm"
                                                value="${gblRequest.properties['jcr:created'].time}"/></td>
                            <td>${gbl:displayLocale(gblRequest.properties['sourceLanguage'].string, renderContext.UILocale)}&nbsp;->
                                <ul class="list-unstyled">
                                    <c:forEach items="${gblRequest.properties['targetLanguage']}" var="lan">
                                        <li>${gbl:displayLocale(lan.string, renderContext.UILocale)}</li>
                                    </c:forEach>
                                </ul>
                            </td>
                            <td>
                                    ${gblRequest.properties['name'].string}
                            </td>

                            <td>

                                <c:forTokens items="${gblRequest.properties['targetTicket'].string}" delims=","
                                             var="targetData" varStatus="counter">
                                    <c:set var="targetLocale" value="${fn:substringBefore(targetData, '_')}"/>
                                    <c:set var="remaining" value="${fn:substringAfter(targetData, '_')}"/>
                                    <c:set var="wordCount" value="${fn:substringAfter(remaining, '_')}"/>
                                    <c:set var="ticket" value="${fn:substringBefore(remaining, '_')}"/>
                                    <dl class="dl-horizontal">
                                        <dt><fmt:message key="request.target.language"/></dt>
                                        <dd>${gbl:displayLocale(fn:replace(targetLocale,"-","_"),renderContext.UILocale)}</dd>

                                        <dt><fmt:message key="request.target.wordcount"/></dt>
                                        <dd>${wordCount}</dd>
                                    </dl>
                                </c:forTokens>
                                <c:if test="${not empty gblRequest.properties['gblContentCount'].long}">
                                    <dl class="dl-horizontal">
                                        <dt><fmt:message key="request.target.contentcount"/></dt>
                                        <dd>${gblRequest.properties['gblContentCount'].long}</dd>
                                    </dl>
                                </c:if>

                            </td>

                            <td>
                                <c:choose>
                                    <c:when test="${not empty gblRequest.properties['gblError']}">
                                        <div style="color: #FA5858;">${gblRequest.properties['gblError'].string}</div>
                                    </c:when>
                                    <c:when test="${empty gblRequest.properties['gblError'] && not empty gblRequest.properties['gblSubmitState']}">
                                        <fmt:message key="${ gblRequest.properties['gblSubmitState'].string }"/>
                                    </c:when>
                                    <c:otherwise>
                                        <fmt:message key="request.created"/>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<script>
    $(document).ready(function () {
        moment.locale('${renderContext.UILocale.language}');
        $.fn.dataTable.moment('yyyy-MM-dd HH:mm');
        $.fn.dataTable.moment('yyyy-MM-dd HH:mm');
        $('#request-list').DataTable({
            serverSide    : false,
            deferRender   : true,
            processing    : false,
            responsive    : {details: false},
            scrollCollapse: true,
            paging        : true,
            scrollX       : true,
            scroller      : false,
            autoWidth     : false,
            dom           : "<'row'<'col-sm-6'l><'col-sm-3'i><'col-sm-3'f>><'row'<'col-lg-12'tr>><'row'<'col-sm-6'><'col-sm-3'i>>",
            order         : [[2, 'desc']],
            scrollY       : <c:choose><c:when test="${index.count lt 5}">true</c:when><c:otherwise>"500px"</c:otherwise></c:choose>
        });
    });
</script>