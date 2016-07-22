package org.jahia.translation.globallink.action;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jahia.translation.globallink.common.GlobalLinkConstants.*;

import com.globallink.api.GLExchange;
import com.globallink.api.model.LanguageDirection;
import com.globallink.api.model.Project;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.translation.globallink.dto.GlobalLinkConfigurationDTO;
import org.jahia.translation.globallink.util.GlobalLinkUtil;
import org.jahia.translation.globallink.util.JCRUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Jahia Action to process and save Global Link Site configurations.
 *
 * @author Aashish.Kocchar, WebitUp
 */
public class GlobalLinkConfigAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLinkConfigAction.class);

    /**
     * Saves the GlobalLink configurations to the site node.
     */
    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map,
                                  URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(SC_OK, request.getRequestURL().toString(), new JSONObject());
        try {
            this.saveGlobalLinkConfigs(request, renderContext, jcrSessionWrapper);
            actionResult.getJson().put("message", "gbl.settings.success");
        } catch (Exception ex) {
            LOGGER.error("Exception while saving GlobalLink settings!!", ex);
            actionResult.getJson().put("message", "gbl.settings.error");
        }
        return actionResult;
    }

    /**
     * Save global link settings for a site.
     *
     * @param request
     * @param renderContext
     * @param sessionWrapper
     * @throws RepositoryException
     */
    private void saveGlobalLinkConfigs(HttpServletRequest request, RenderContext renderContext, JCRSessionWrapper sessionWrapper)
            throws RepositoryException {
        JCRSiteNode siteNode = renderContext.getSite();
        LOGGER.info("Saving configuration for Site: {}", siteNode.getTitle());
        if (!Arrays.asList(siteNode.getMixinNodeTypes()).contains(GBL_MIXIN_TYPE)) {
            siteNode.addMixin(GBL_MIXIN_TYPE);
            sessionWrapper.save();
        }
        switch (getRequestParameter(request, GBL_PROPERTY_ENABLE)) {
		case "true":
			siteNode.setProperty(GBL_PROPERTY_ENABLE, true);
			break;
        case "false":
            siteNode.setProperty(GBL_PROPERTY_ENABLE, false);
            break;
		default:
			break;
		}
        siteNode.setProperty(GBL_PROPERTY_USERNAME, getRequestParameter(request, GBL_PROPERTY_USERNAME));
        siteNode.setProperty(GBL_PROPERTY_PASSWORD, getRequestParameter(request, GBL_PROPERTY_PASSWORD));
        siteNode.setProperty(GBL_PROPERTY_URL, getRequestParameter(request, GBL_PROPERTY_URL));
        siteNode.setProperty(GBL_PROPERTY_AGENT, getRequestParameter(request, GBL_PROPERTY_AGENT));
        siteNode.setProperty(GBL_PROPERTY_PROJECT, getRequestParameter(request, GBL_PROPERTY_PROJECT));
        siteNode.setProperty(GBL_PROPERTY_PREFIX, getRequestParameter(request, GBL_PROPERTY_PREFIX));
        siteNode.setProperty(GBL_PROPERTY_FORMAT, getRequestParameter(request, GBL_PROPERTY_FORMAT));
        siteNode.setProperty(GBL_PROPERTY_DOC_LOCATION, getRequestParameter(request, GBL_PROPERTY_DOC_LOCATION));
        if (!getRequestParameter(request, GBL_PROPERTY_INTERVAL).equals(StringUtils.EMPTY)) {
            siteNode.setProperty(GBL_PROPERTY_INTERVAL, Long.valueOf(getRequestParameter(request, GBL_PROPERTY_INTERVAL)));
        }
        siteNode.setProperty(GBL_PROPERTY_COMPONENTS, getMultiRequestparameter(request, GBL_PROPERTY_COMPONENTS));
        //Change by cedric to save even in case of errors
        sessionWrapper.save();
        this.getLanguageDirections(siteNode);
        sessionWrapper.save();
    }

    /**
     * Check for null pointer and return request parameter value.
     *
     * @return parameter value
     */
    private String getRequestParameter(HttpServletRequest request, String parameterName) {
        if (request.getParameter(parameterName) != null) {
            return request.getParameter(parameterName);
        }
        return StringUtils.EMPTY;
    }

    /**
     * Check for null pointer and return array of string for a request parameter.
     *
     * @return parameter value
     */
    private String[] getMultiRequestparameter(HttpServletRequest request, String parameterName) {
        if (request.getParameter(parameterName) != null) {
            return request.getParameterMap().get(parameterName);
        }
        return null;
    }

    private void getLanguageDirections(JCRSiteNode siteNode) throws RepositoryException {
        List<JCRSiteNode> siteNodes = new ArrayList<>();
        siteNodes.add(siteNode);
        List<GlobalLinkConfigurationDTO> configs = JCRUtil.getConfigurationList(siteNodes);
        Set<String> languages = siteNode.getLanguages();
        languages.forEach(lang -> {
            LOGGER.info("Site Lang: "+ GlobalLinkUtil.getFullLocale(lang));
        });
        if (configs.size() > 0) {
            GLExchange glExchange = GlobalLinkUtil.getGLExchangeClient(configs.get(0));
            Project project = glExchange.getProject(configs.get(0).getProjectName());
            LanguageDirection[] directions = project.getLanguageDirections();
            for (LanguageDirection direction : directions) {
                String sourceLang = direction.sourceLanguage.replace("-","_");
                String targetLang = direction.targetLanguage.replace("-","_");
                LOGGER.info("Checking for lang: "+ GlobalLinkUtil.getShortLocale(targetLang));
                if (languages.contains(targetLang)) {
                    if (!siteNode.hasNode(targetLang + "-target")) {
                        JCRNodeWrapper targetNode = siteNode.addNode(targetLang + "-target",
                                GBL_PROPERTY_TARGET_DIRECTIONS);
                    }
                }
                if (languages.contains(sourceLang)) {
                    if (!siteNode.hasNode(sourceLang + "-source")) {
                        JCRNodeWrapper sourceNode = siteNode.addNode(sourceLang + "-source",
            }
        }
    }
}
