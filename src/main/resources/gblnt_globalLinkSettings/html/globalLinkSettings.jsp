<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="gbl" uri="http://jahia.com/translation/globallink/1.0" %>

<fmt:message key="gbl.settings.success" var="success"/>
<fmt:message key="gbl.settings.error" var="error"/>

<template:addResources type="javascript" resources="jquery.min.js,jquery.form.min.js"/>
<template:addResources type="css" resources="gblbootstrap.min.css"/>
<template:addResources type="css" resources="multi-select.css"/>
<template:addResources type="javascript" resources="gblbootstrap.min.js"/>
<template:addResources type="javascript" resources="jquery.multi-select.js"/>
<template:addResources type="javascript" resources="jquery.quicksearch.js"/>
<c:set var="directions" value="${gbl:projectInfo(renderContext.mainResource.node)}"/>

<c:set var="site" value="${renderContext.mainResource.node.resolveSite}"/>
<c:if test="${not empty site.properties['j:globalLinkActivated']}">
    <c:set var="isActive" value="${site.properties['j:globalLinkActivated'].boolean}"/>
</c:if>

<template:addResources>
    <script type="text/javascript">
        function showHideError($componentSelection, init) {
            var options = $.makeArray($componentSelection.children('option:selected'));
            if (options.length == 0) {
                if (!init) {
                    $(".ms-selection").addClass("ms-error");
                }
                $("#updateSiteButton").prop("disabled", true);
            } else {
                $(".ms-selection").removeClass("ms-error");
                $("#updateSiteButton").prop("disabled", false);
            }
        }
        function checkMappings() {
            var $updateSiteButton = $("#updateSiteButton");
            var $mappingSelector  = $(".mappingSelector");
            if ($mappingSelector.length > 0) {
                $updateSiteButton.prop("disabled", true);
                $mappingSelector.each(function () {
                    var $this = $(this);
                    if ($this.val() != "") {
                        $('#' + $this.data('siteLocale') + 'hid').val($this.val());
                        $updateSiteButton.prop("disabled", false);
                    }
                })
            }
        }
        $(document).ready(function () {
            var $componentSelection = $('#componentSelection');
            var options             = $.makeArray($componentSelection.children('option'));
            options.sort(function (a, b) {
                return $(a).html().localeCompare($(b).html());
            });
            $componentSelection.empty();
            $.each(options, function () {
                $componentSelection.append(this);
            });
            $componentSelection.multiSelect({
                selectableHeader: "<input type='text' class='search-input form-control' autocomplete='off' placeholder='<fmt:message key="gbl.settings.componentlist.searchComponents"/>'>",
                selectionHeader : "<input type='text' class='search-input form-control' autocomplete='off' placeholder='<fmt:message key="gbl.settings.componentlist.searchComponents"/>'>",
                selectableFooter: "<div class='custom-label'><fmt:message key="gbl.settings.componentlist.selectable" /></div>",
                selectionFooter : "<div class='custom-label'><fmt:message key="gbl.settings.componentlist.selected" /><span class='glyphicon glyphicon-asterisk'></span></div>",
                afterInit       : function (ms) {
                    var that                   = this,
                        $selectableSearch      = that.$selectableUl.prev(),
                        $selectionSearch       = that.$selectionUl.prev(),
                        selectableSearchString = '#' + that.$container.attr('id') + ' .ms-elem-selectable:not(.ms-selected)',
                        selectionSearchString  = '#' + that.$container.attr('id') + ' .ms-elem-selection.ms-selected';

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
                    showHideError($componentSelection, true)
                },
                afterSelect     : function () {
                    this.qs1.cache();
                    this.qs2.cache();
                    showHideError($componentSelection, false);
                },
                afterDeselect   : function () {
                    this.qs1.cache();
                    this.qs2.cache();
                    showHideError($componentSelection, false)
                }
            });

            $('#activateProvider').on('click', function () {
                if ($(this).prop("checked")) {
                    $('#globalLinkActivatedSubmit').val("true");
                } else {
                    $('#globalLinkActivatedSubmit').val("false");
                }
            });

            $("input[name=componentsType]").on("click", function () {
                var componentType = $("input[name=componentsType]:checked").val();
                $.get("<c:url value="${url.basePreview}${currentResource.node.path}.json"/>", {
                    "componentType": componentType,
                    "identifier"   : "${renderContext.mainResource.node.identifier}"
                }, function (result) {
                    var $componentSelection = $('#componentSelection');
                    var options             = $.makeArray($componentSelection.children('option:selected'));
                    var selected            = [];
                    $componentSelection.empty();
                    $.each(options, function () {
                        $componentSelection.append(this);
                        selected.push($(this).val());
                    });
                    $.each(result, function () {
                        var optionValue = this.value + "-" + this.key;
                        if (selected.indexOf(optionValue) == -1) {
                            var option = $("<option value='" + optionValue + "'>" + this.value + "</option>");
                            $componentSelection.append(option);
                        }
                    });
                    options = $.makeArray($componentSelection.children('option'));
                    options.sort(function (a, b) {
                        return $(a).html().localeCompare($(b).html());
                    });
                    $componentSelection.empty();
                    $.each(options, function () {
                        $componentSelection.append(this);
                    });
                    $componentSelection.multiSelect('refresh');
                }, "json");
            });

            checkMappings();
        });

        const saveTranslationSettings = () => {
            var data = $('#updateSiteForm').serialize();
            $.ajax({
                url: '<c:url value="${url.base}${site.path}.globalLinkConfig.do"/>',
                type: 'POST',
                dataType : "json",
                data: data
            }).done(function () {
                    top.location.reload();
                }
            ).fail(err => {
            console.log('an error occurred', err);
            });
            return false;
        }
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

<div class="row" style="margin: 0">
    <div class="col-md-12" style="margin: 0">
        <img src="<c:url value='/modules/jahia-translation-globallink/img/globalLink.png'/>" width="100px"
             style="margin: 0">
    </div>
</div>
<div class="row">
    <div class="col-md-12">
        <form id="updateSiteForm" class="horizontal" onsubmit="return saveTranslationSettings();">
            <div class="col-md-4">

                <c:choose>
                    <c:when test="${(not empty site.properties['status'].string) and (not (site.properties['status'].string eq 'OK'))}">
                        <div class="alert alert-danger" id="error">
                            <fmt:message key="gbl.settings.connectorNameIssue"> ""
                                <fmt:param value="${site.properties['j:globalLinkConnectorName'].string}"/>
                                <fmt:param value="${site.properties['status'].string}"/>
                            </fmt:message>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:choose>
                            <c:when test="${directions eq 'NA'}">
                                <div style="padding:4px; font-weight:bold; background: red; color: white;"><fmt:message key="gbl.settings.configNOK"/></div>
                            </c:when>
                            <c:otherwise>
                                <div style="padding:4px; font-weight:bold; background: green; color: white;"><fmt:message key="gbl.settings.configOK"/></div>
                            </c:otherwise>
                        </c:choose>
                    </c:otherwise>
                </c:choose>

                <h1 class="globallink-heading"><fmt:message key="gbl.settings.title"/></h1>
                <%--<c:if test="${not empty site.properties['status']}">--%>
                    <%--<c:choose>--%>
                        <%--<c:when test="${site.properties['status'].string eq 'OK' }">--%>
                            <%--<div class="alert alert-success" id="success">--%>
                                <%--<strong><fmt:message key="gbl.settings.success"/></strong>--%>
                            <%--</div>--%>
                        <%--</c:when>--%>
                        <%--<c:otherwise>--%>
                            <%--<div class="alert alert-danger" id="error">--%>
                                <%--<strong>${site.properties['status'].string}</strong>--%>
                            <%--</div>--%>
                        <%--</c:otherwise>--%>
                    <%--</c:choose>--%>
                <%--</c:if>--%>

                <input type="hidden" name="jcrRedirectTo"
                       value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
                <input type="hidden" name="jcrNewNodeOutputFormat"
                       value="<c:url value='${renderContext.mainResource.template}.html'/>">
                <input type="hidden" name="j:globalLinkUserAgent" value="jahia-translation-globallink">

                <div class="row">
                    <div class="col-md-12">

                        <fieldset class="form-group">
                            <div class="col-md-3">
                                <label for="globalLinkActivated"><fmt:message key="gbl.settings.enable"/></label>
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
                                <p><fmt:message key="gbl.settings.username.detail"/></p>
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
                                <p><fmt:message key="gbl.settings.password.detail"/></p>
                            </div>
                        </fieldset>

                        <fieldset class="form-group">
                            <div class="col-md-3">
                                <label for="gblUrl"><fmt:message key="gbl.settings.url"/></label>
                            </div>
                            <div class="col-md-9">
                                <input type="text" class="form-control"
                                       placeholder="https://connect.translations.com/api/v2/" required id="gblUrl"
                                       name="j:globalLinkUrl" value="${site.properties['j:globalLinkUrl'].string}">
                                <p><fmt:message key="gbl.settings.url.detail"/></p>
                            </div>
                        </fieldset>

                        <fieldset class="form-group">
                            <div class="col-md-3">
                                <label for="gblProject"><fmt:message key="gbl.settings.connectorName"/></label>
                            </div>
                            <div class="col-md-9">
                                <input type="text" class="form-control" placeholder="FS Jahia" required
                                       id="gblProject" name="j:globalLinkConnectorName"
                                       value="${site.properties['j:globalLinkConnectorName'].string}">
                                <p><fmt:message key="gbl.settings.connectorName.detail"/></p>
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
                                <p><fmt:message key="gbl.settings.submissionprefix.detail"/></p>
                            </div>
                        </fieldset>

                        <fieldset class="form-group">
                            <div class="col-md-3">
                                <label for="fileFormat"><fmt:message key="gbl.settings.fileformat"/></label>
                            </div>
                            <div class="col-md-9">
                                <input type="text" class="form-control" placeholder="Jahia_XML" required
                                       id="fileFormat" name="j:globalLinkfileFormat"
                                       value="${(not empty site.properties['j:globalLinkfileFormat'].string) ? site.properties['j:globalLinkfileFormat'].string : 'Jahia_XML'}">
                                <p><fmt:message key="gbl.settings.fileformat.detail"/></p>
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
                                <input type="text" class="form-control" placeholder="2" required
                                       id="globalLinkInterval" name="j:globalLinkInterval"
                                       value="${site.properties['j:globalLinkInterval'].long}">
                                <p><fmt:message key="gbl.settings.submissioninterval.detail"/></p>
                            </div>
                        </fieldset>

                    </div>
                </div>
            </div>

            <div class="col-md-4">
                <h3><fmt:message key="gbl.settings.componentlist"/></h3>
                <h6><fmt:message key="gbl.settings.componentlist.info"/></h6>
                <label class="radio-inline">
                    <input type="radio" name="componentsType" value="all"><fmt:message
                        key="gbl.settings.components.all"/>
                </label>
                <label class="radio-inline">
                    <input type="radio" name="componentsType" value="editorial" checked><fmt:message
                        key="gbl.settings.components.editorial"/>
                </label>
                <fieldset class="form-group">
                    <select name="j:componentsList" id="componentSelection" multiple size="30">
                        <c:forEach items="${site.properties['j:componentsList']}" var="component">
                            <option value="${component.string}"
                                    selected>${fn:substringBefore(component.string, '-')}
                            </option>
                        </c:forEach>
                        <c:forEach
                                items="${gbl:componentList(renderContext.mainResource.node, renderContext.request.locale, script, site.properties['j:componentsList'])}"
                                var="component">
                            <option value="${component.value}-${component.key}">${component.value}
                            </option>
                        </c:forEach>
                    </select>
                </fieldset>
            </div>
            <div class="col-md-4">
                <c:if test="${directions != 'NS'}">
                    <div style="padding: 0px 10px;" class="data">
                        <div class="well">
                            <h2><fmt:message key="gbl.settings.projectdirections"/></h2>

                            <c:choose>
                                <c:when test="${directions eq 'NA'}">
                                    <div class="alert alert-danger">
                                        <strong>
                                            <fmt:message key="gbl.settings.project.fail"/>
                                        </strong>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <ul class="list-inline">
                                        <c:forTokens items="${directions}" delims="," var="direction">
                                            <li><span class="label label-success">${direction}</span></li>
                                        </c:forTokens>
                                    </ul>
                                    <div class="row">
                                        <div class="col-md-12">
                                            <h2><fmt:message key="gbl.settings.projectdirections.mappings"/></h2>
                                        </div>
                                    </div>

                                    <jsp:useBean id="mappings" class="java.util.LinkedHashMap"/>
                                    <jsp:useBean id="existingMappings" class="java.util.LinkedHashMap"/>
                                    <jcr:nodeProperty node="${site}" name="j:languageMappings" var="existingOnes"/>
                                    <c:forEach items="${existingOnes}" var="targetLanguage">
                                        <c:set target="${existingMappings}"
                                               property="${targetLanguage.string}"
                                               value="${fn:substringBefore(targetLanguage.string,'###')}"/>
                                    </c:forEach>
                                    <c:forEach items="${jcr:getChildrenOfType(site, 'gblnt:globalLinkSourceLanguage')}"
                                               var="sourceLanguage">
                                        <c:set target="${mappings}"
                                               property="${fn:substringBefore(sourceLanguage.name,'-gblSource')}"
                                               value="${gbl:displayLocale(sourceLanguage.name, renderContext.UILocale)}"/>
                                        <jcr:nodeProperty node="${sourceLanguage}" name="targetLanguages" var="targetLanguages"/>
                                        <c:forEach items="${targetLanguages}" var="targetLanguage">
                                            <c:set target="${mappings}" property="${targetLanguage.string}"
                                                   value="${gbl:displayLocale(targetLanguage.string, renderContext.UILocale)}"/>
                                        </c:forEach>
                                    </c:forEach>
                                    <c:forEach items="${renderContext.site.languagesAsLocales}" var="siteLocale">
                                        <div class="row" style="padding-top: 5px">
                                            <div class="col-md-12">
                                                <div class="form-group">
                                                    <input type="hidden" name="j:languageMappings" id="${siteLocale}hid">
                                                    <label for="targetMapping${siteLocale}"
                                                           class="col-sm-4 control-label">${functions:displayLocaleNameWith(siteLocale, renderContext.UILocale)}</label>
                                                    <div class="col-sm-8">
                                                        <select name="targetMapping${siteLocale}"
                                                                onchange="$('#${siteLocale}hid').val($(this).val());checkMappings();"
                                                                class="mappingSelector form-control"
                                                                data-site-locale="${siteLocale}">
                                                            <option value="">-----</option>
                                                            <c:forEach items="${mappings}" var="mapping">
                                                                <c:set var="key">${siteLocale}###${mapping.key}</c:set>
                                                                <option value="${key}"
                                                                        <c:if test="${existingMappings[key] eq siteLocale}">selected</c:if>>${mapping.value}</option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </div>

                    </div>
                </c:if>
            </div>
            <div class="col-md-12">
                <input type="submit" name="updateSiteButton" id="updateSiteButton"
                       class="btn btn-primary btn-sm"
                       value="<fmt:message key='gbl.label.save'/>" disabled/>
            </div>
        </form>
    </div>
</div>
