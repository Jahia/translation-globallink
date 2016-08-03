<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="gbl" uri="http://jahia.com/translation/globallink/1.0" %>

<fmt:message key="gbl.settings.success" var="success"/>
<fmt:message key="gbl.settings.error" var="error"/>

<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<template:addResources type="css" resources="bootstrap.min.css"/>
<template:addResources type="css" resources="multi-select.css"/>
<template:addResources type="javascript" resources="bootstrap.min.js"/>
<template:addResources type="javascript" resources="jquery.multi-select.js"/>
<template:addResources type="javascript" resources="jquery.quicksearch.js"/>

<template:addResources>
    <script type="text/javascript">
        $(document).ready(function () {
            $('#success').hide();
            $('#error').hide();
            var $componentSelection = $('#componentSelection');
            var options  = $.makeArray($componentSelection.children('option'));
            options.sort(function (a, b){
                return $(a).html().localeCompare($(b).html());
            });
            $componentSelection.empty();
            $.each(options, function(){
                $componentSelection.append(this);
            });
            $componentSelection.multiSelect({
                selectableHeader: "<input type='text' class='search-input form-control' autocomplete='off' placeholder='Search Components'>",
                selectionHeader: "<input type='text' class='search-input form-control' autocomplete='off' placeholder='Search Components'>",
                selectableFooter: "<div class='custom-label'><fmt:message key="gbl.settings.componentlist.selectable" /></div>",
                selectionFooter: "<div class='custom-label'><fmt:message key="gbl.settings.componentlist.selected" /></div>",
                afterInit: function (ms) {
                    var that = this,
                            $selectableSearch = that.$selectableUl.prev(),
                            $selectionSearch = that.$selectionUl.prev(),
                            selectableSearchString = '#' + that.$container.attr('id') + ' .ms-elem-selectable:not(.ms-selected)',
                            selectionSearchString = '#' + that.$container.attr('id') + ' .ms-elem-selection.ms-selected';

                    that.qs1 = $selectableSearch.quicksearch(selectableSearchString)
                            .on('keydown', function (e) {
                                if (e.which === 40) {
                                    that.$selectableUl.focus();
                                    return false;
                                }
                            });

                    that.qs2 = $selectionSearch.quicksearch(selectionSearchString)
                            .on('keydown', function (e) {
                                if (e.which == 40) {
                                    that.$selectionUl.focus();
                                    return false;
                                }
                            });
                },
                afterSelect: function () {
                    this.qs1.cache();
                    this.qs2.cache();
                },
                afterDeselect: function () {
                    this.qs1.cache();
                    this.qs2.cache();
                }
            });

            $('#activateProvider').on('click', function () {
                if ($(this).prop("checked")) {
                    $('#globalLinkActivatedSubmit').val("true");
                } else {
                    $('#globalLinkActivatedSubmit').val("false");
                }
            });
        });
    </script>

</template:addResources>


<template:addResources>
    <style type="text/css">
        .form-control {
            height: 30px !important;
        }

        .globallink-heading {
            color: #0088cc;
            font-size: 20px;
        }
    </style>
</template:addResources>

<c:set var="site" value="${renderContext.mainResource.node.resolveSite}"/>
<c:if test="${not empty site.properties['j:globalLinkActivated']}">
    <c:set var="isActive" value="${site.properties['j:globalLinkActivated'].boolean}"/>
</c:if>

<div class="container">
    <div class="row">
        <div class="col-md-12">
            <form id="updateSiteForm" action="<c:url value='${url.base}${site.path}.globalLinkConfig.do'/>"
                  method="post">
                <div class="col-md-4">
                    <h1 class="globallink-heading"><fmt:message key="gbl.settings.title"/></h1>
                    <div class="alert alert-success" id="success">
                        <strong></strong>
                    </div>
                    <div class="alert alert-danger" id="error">
                        <strong></strong>
                    </div>

                    <input type="hidden" name="jcrRedirectTo"
                           value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
                    <input type="hidden" name="jcrNewNodeOutputFormat"
                           value="<c:url value='${renderContext.mainResource.template}.html'/>">

                    <div class="row">
                        <div class="col-md-12">

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblUsername"><fmt:message key="gbl.settings.enable"/></label>
                                </div>
                                <div class="col-md-4">
                                    <input type="checkbox" name="globalLinkActivated"
                                           class="checkbox-inline" style="margin: 0px; padding: 0px;"
                                           id="activateProvider"
                                    ${not empty isActive && isActive ? 'checked="checked"' : ''}
                                           value="${isActive ? 'true' : ''}"/>
                                </div>
                            </fieldset>
                            <input type="hidden" name="j:globalLinkActivated" id="globalLinkActivatedSubmit" value=""/>


                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblUsername"><fmt:message key="gbl.settings.username"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="John Doe" required
                                           id="gblUsername" name="j:globalLinkUsername"
                                           value="${site.properties['j:globalLinkUsername'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblPassword"><fmt:message key="gbl.settings.password"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="password" class="form-control" placeholder="**********" required
                                           id="gblPassword" name="j:globalLinkPassword"
                                           value="${site.properties['j:globalLinkPassword'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblUrl"><fmt:message key="gbl.settings.url"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control"
                                           placeholder="http://example.com" required id="gblUrl"
                                           name="j:globalLinkUrl" value="${site.properties['j:globalLinkUrl'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblUserAgent"><fmt:message key="gbl.settings.useragent"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="Jahia" required
                                           id="gblUserAgent" name="j:globalLinkUserAgent"
                                           value="${site.properties['j:globalLinkUserAgent'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblProject"><fmt:message key="gbl.settings.project"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="JAH000000" required
                                           id="gblProject" name="j:globalLinkProject"
                                           value="${site.properties['j:globalLinkProject'].string}">
                                </div>
                            </fieldset>


                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="gblSubmissionPrefix"><fmt:message
                                            key="gbl.settings.submissionprefix"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="Jahia" required
                                           id="gblSubmissionPrefix" name="j:globalLinkSubmissionPrefix"
                                           value="${site.properties['j:globalLinkSubmissionPrefix'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="fileFormat"><fmt:message key="gbl.settings.fileformat"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="Jahia_XML" required
                                           id="fileFormat" name="j:globalLinkfileFormat"
                                           value="${site.properties['j:globalLinkfileFormat'].string}">
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="fileFormat"><fmt:message key="gbl.settings.documentlocation"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control"
                                           placeholder="Don't type anything in this field."
                                           id="globalLinkfileLocation" name="j:globalLinkfileLocation"
                                           value="${site.properties['j:globalLinkfileLocation'].string}">
                                    <p><fmt:message key="gbl.settings.documentlocation.detail"/></p>
                                </div>
                            </fieldset>

                            <fieldset class="form-group">
                                <div class="col-md-3">
                                    <label for="globalLinkInterval"><fmt:message
                                            key="gbl.settings.submissioninterval"/></label>
                                </div>
                                <div class="col-md-9">
                                    <input type="text" class="form-control" placeholder="10" required
                                           id="globalLinkInterval" name="j:globalLinkInterval"
                                           value="${site.properties['j:globalLinkInterval'].long}">
                                    <p><fmt:message key="gbl.settings.submissioninterval.detail"/></p>
                                </div>
                            </fieldset>

                        </div>
                    </div>
                </div>

                <div class="col-md-5">
                    <h1><fmt:message key="gbl.settings.componentlist"/></h1>
                    <select name="j:componentsList" id="componentSelection" multiple size="30">
                        <c:forEach items="${site.properties['j:componentsList']}" var="component">
                            <option value="${component.string}" selected>${fn:substringBefore(component.string, '-')}
                            </option>
                        </c:forEach>
                        <c:forEach
                                items="${gbl:componentList(renderContext.mainResource.node, renderContext.request.locale, script, site.properties['j:componentsList'])}"
                                var="component">
                            <option value="${component.value}-${component.key}">${component.value}
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3">
                    <c:set var="directions" value="${gbl:projectInfo(renderContext.mainResource.node)}"/>
                    <c:if test="${directions != 'NS'}">
                        <div style="padding: 0px 10px;" class="data">
                            <h1><fmt:message key="gbl.settings.projectdirections"/></h1>

                            <c:choose>
                                <c:when test="${directions eq 'NA'}">
                                    <div class="alert alert-danger">
                                        <strong>
                                            <fmt:message key="gbl.settings.project.fail"/>
                                        </strong>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <ul>
                                        <c:forTokens items="${directions}" delims="," var="direction">
                                            <li><p>${direction}</p></li>
                                        </c:forTokens>
                                    </ul>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </div>
                <form>
                    <div class="col-md-12">
                        <input type="submit" name="updateSiteButton" id="updateSiteButton"
                               class="btn btn-primary btn-sm"
                               value="<fmt:message key='gbl.label.save'/>"/>
                    </div>
                </form>
            </form>
        </div>
    </div>
</div>
