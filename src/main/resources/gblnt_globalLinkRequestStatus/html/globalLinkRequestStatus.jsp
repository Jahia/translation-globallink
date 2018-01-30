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
         sql="select * from [gblnt:globalLinkProject] where isdescendantnode(['${site.path}']) order by [jcr:created] desc"/>

<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<template:addResources type="css" resources="gblbootstrap.min.css"/>
<template:addResources type="css" resources="datatables.min.css"/>
<template:addResources type="javascript" resources="gblbootstrap.min.js"/>
<template:addResources type="javascript" resources="moment.min.js"/>
<template:addResources type="javascript" resources="datatables.min.js"/>
<template:addResources type="javascript" resources="datetime-moment.js"/>
<div class="row">
    <div class="col-md-6">
        <img src="<c:url value='/modules/jahia-translation-globallink/img/globalLink.png'/>" width="100px"
        >
    </div>
    <div class="col-md-6 pull-right" style="margin-top: 15px;margin-bottom: 10px">
        <form id="cleanSubmissionForm" action="<c:url value='${url.base}${site.path}.globalLinkSubmissionClean.do'/>"
              method="post" class="form-inline">
            <input type="hidden" name="jcrRedirectTo"
                   value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
            <input type="hidden" name="jcrNewNodeOutputFormat"
                   value="<c:url value='${renderContext.mainResource.template}.html'/>">
            <div class="form-group">
                <label for="daysOld"><fmt:message key="gbl.settings.clean.submission"/></label>
                <div class="input-group input-group-sm">
                    <input type="text" class="form-control" required
                           id="daysOld" name="daysOld"
                           value="30">
                    <div class="input-group-addon">days</div>
                </div>
            </div>
            <button type="submit"
                    class="btn btn-primary btn-sm"><fmt:message key='gbl.label.clean'/></button>
        </form>
    </div>
</div>

<div class="row">
    <div class="col-lg-12">
        <table class="table table-striped nowrap small" id="request-list">
            <thead>
            <tr>
                <th>Ticket</th>
                <th><fmt:message key="request.page"/></th>
                <th><fmt:message key="request.date"/></th>
                <th><fmt:message key="request.date.submitted"/></th>
                <th class="nowrap text-center" data-orderable="false"><fmt:message key="request.language"/></th>
                <th><fmt:message key="request.tickets"/></th>
                <th data-orderable="false" class="text-left"><fmt:message key="request.target"/></th>
                <th><fmt:message key="request.status"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${gblRequests.nodes}" var="gblRequest" varStatus="index">
                <c:choose>
                    <c:when test="${gblRequest.properties['skipTranslated'].boolean && gblRequest.properties['gblContentCount'].long == 0}">
                    </c:when>
                    <c:otherwise>
                        <tr><td data-order="${gblRequest.properties['submissionTicket'].string}"><small title="${gblRequest.properties['submissionTicket'].string}">${functions:abbreviate(gblRequest.properties['submissionTicket'].string,16,16,'...')}</small></td>
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
                            <td>${gbl:displayLocale(fn:substringAfter(gblRequest.properties['sourceLanguage'].string,"###"), renderContext.UILocale)}&nbsp;->&nbsp;
                                <ul class="list-unstyled">
                                        <c:forEach items="${gblRequest.properties['targetLanguage']}" var="lan">
                                        <li>${gbl:displayLocale(fn:substringAfter(lan.string,"###"), renderContext.UILocale)}</li>
                                        </c:forEach>
                                </ul>
                            </td>
                            <td>
                                    ${gblRequest.properties['name'].string}
                            </td>

                            <td>
                                <dl class="">
                                    <c:forTokens items="${gblRequest.properties['targetTicket'].string}" delims=","
                                                 var="targetData" varStatus="counter">
                                        <c:set var="targetLocale" value="${fn:substringBefore(targetData, '_')}"/>
                                        <c:set var="remaining" value="${fn:substringAfter(targetData, '_')}"/>
                                        <c:set var="wordCount" value="${fn:substringAfter(remaining, '_')}"/>
                                        <c:set var="ticket" value="${fn:substringBefore(remaining, '_')}"/>

                                        <dt><fmt:message key="request.target.language"/></dt>
                                        <dd>${gbl:displayLocale(targetLocale,renderContext.UILocale)}</dd>

                                        <dt><fmt:message key="request.target.wordcount"/></dt>
                                        <dd>${wordCount}</dd>

                                    </c:forTokens>
                                    <c:if test="${not empty gblRequest.properties['gblContentCount'].long}">
                                        <dt><fmt:message key="request.target.contentcount"/></dt>
                                        <dd>${gblRequest.properties['gblContentCount'].long}</dd>
                                    </c:if>
                                </dl>
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
<div class="row">
    <div class="col-sm-12"></div>
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
            paging        : false,
            scrollX       : true,
            scroller      : false,
            autoWidth     : false,
            dom           : "<'row'<'col-sm-9'><'col-sm-3'f>><'row'<'col-lg-12'tr>><'row'<'col-sm-12'>>",
            order         : [[3, 'desc']],
            scrollY       : <c:choose><c:when test="${index.count lt 5}">true
            </c:when><c:otherwise>"500px"</c:otherwise></c:choose>
        });
    });
</script>