<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr"%>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search"%>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions"%>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib"%>
<%@ taglib prefix="gbl" uri="http://jahia.com/translation/globallink/1.0"%>

<c:set var="site" value="${renderContext.mainResource.node.resolveSite}" />
<jcr:sql var="gblRequests" sql="select * from [jnt:globalLinkProject] where isdescendantnode(['${site.path}']) order by [jcr:created] desc" />

<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js" />
<template:addResources type="css" resources="bootstrap.min.css" />
<template:addResources type="css" resources="datatables.min.css" />
<template:addResources type="javascript" resources="bootstrap.min.js" />
<template:addResources type="javascript" resources="datatables.min.js" />

<table class="table table-striped" id="request-list">
	<thead>
		<tr>
			<th>#</th>
			<th><fmt:message key="request.page" /></th>
			<th><fmt:message key="request.date" /></th>
			<th><fmt:message key="request.language" /></th>
			<th><fmt:message key="request.tickets" /></th>
			<th><fmt:message key="request.target" /></th>
			<th><fmt:message key="request.status" /></th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${gblRequests.nodes}" var="gblRequest" varStatus="index">
		    <c:choose>
		        <c:when test="${gblRequest.properties['skipTranslated'].boolean && gblRequest.properties['gblContentCount'].long == 0}">
		        </c:when>
		        <c:otherwise>
		            <tr>
                        <td>${index.index+1}</td>
                        <td>
                            <a href="${url.base}${gblRequest.parent.path}.html">
                                ${not empty gblRequest.parent.properties['jcr:title'].string ? gblRequest.parent.properties['jcr:title'].string : gblRequest.parent.name }
                            </a>
                            <br/>
                            <span><fmt:message key="request.page.requestid" /></span>
                            ${gblRequest.properties['gblRequestId'].string}
                        </td>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss" value="${gblRequest.properties['jcr:created'].time}"/></td>
                        <td>

                        <c:set var="sourceLang" value="${gbl:getNodeByUuid(gblRequest.properties['sourceLanguage'].string)}" />
                            ${sourceLang.properties['jcr:title'].string} ->
                            <c:forEach items="${gblRequest.properties['targetLanguage']}" var="lan">
                                <c:set var="targetLang" value="${gbl:getNodeByUuid(lan.string)}" />
                                ${targetLang.properties['jcr:title'].string} ${" "}
                            </c:forEach>
                        </td>
                        <td>
                           <c:if test="${not empty gblRequest.properties['uploadTicket'].string}">
                            <p>
                            <strong><fmt:message key="request.tickets.upload" /></strong> ${gblRequest.properties['uploadTicket'].string}
                            </p>
                           </c:if>

                           <c:if test="${not empty gblRequest.properties['submissionTicket'].string}">
                            <p><strong><fmt:message key="request.tickets.submit" /></strong> ${gblRequest.properties['submissionTicket'].string}</p>
                           </c:if>
                        </td>

                        <td>
                            <c:forTokens items="${gblRequest.properties['targetTicket'].string}" delims=","
                            var="targetData" varStatus="counter">
                                <c:set var="targetLocale" value="${fn:substringBefore(targetData, '_')}" />
                                <c:set var="remaining" value="${fn:substringAfter(targetData, '_')}" />
                                <c:set var="wordCount" value="${fn:substringAfter(remaining, '_')}" />
                                <c:set var="ticket" value="${fn:substringBefore(remaining, '_')}" />

                                <p><strong><fmt:message key="request.target.language" /> </strong> ${targetLocale}</p>
                                <p><strong><fmt:message key="request.target.wordcount" /> </strong> ${wordCount}</p>
                                <p><strong><fmt:message key="request.target.id" /> </strong> ${ticket}</p></br>
                            </c:forTokens>
                            <c:if test="${not empty gblRequest.properties['gblContentCount'].long}">
                                <p><strong><fmt:message key="request.target.contentcount" /> </strong> ${gblRequest.properties['gblContentCount'].long}</p>
                            </c:if>
                        </td>

                        <td>
                            <c:choose>
                                <c:when test="${not empty gblRequest.properties['gblError']}">
                                    <div style="color: #FA5858;">${gblRequest.properties['gblError'].string}</div>
                                </c:when>
                                <c:when test="${empty gblRequest.properties['gblError'] && not empty gblRequest.properties['gblSubmitState']}">
                                    <fmt:message key="${ gblRequest.properties['gblSubmitState'].string }" />
                                </c:when>
                                <c:otherwise>
                                    <fmt:message key="request.created" />
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
		        </c:otherwise>
		    </c:choose>
		</c:forEach>
	</tbody>
</table>
	
<script>
    $(document).ready(function() {
        $('#request-list').DataTable();
    });
</script>