/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common.webui;

import java.text.MessageFormat;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.router.ExoRouter;
import org.exoplatform.social.common.router.ExoRouter.Route;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

public class WebUIUtils {
  private static Log LOG = ExoLogger.getLogger(WebUIUtils.class);
  private static final String SCRIPT_PATTERN = 
      "<script src=\"/forumResources/syntaxhighlighter/Scripts/{0}\" id=\"script_{1}_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>";

  public static String getRemoteIP() {
    String remoteAddr = "";
    try {
      PortalRequestContext context = Util.getPortalRequestContext();
      remoteAddr = ((HttpServletRequest)context.getRequest()).getRemoteAddr() ;
    } catch (Exception e) { 
      LOG.error("Can not get remote IP", e);
    }
    return remoteAddr;
  } 
  
  public String getLabel(String key) throws Exception {
    return getLabel(null, key);
  }

  public static String getLabel(String componentId, String label) {
    if (!StringUtils.isEmpty(componentId)) {
      label = componentId.concat(".label.").concat(label);
    }
    try {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      return res.getString(label);
    } catch (MissingResourceException e) {
      return label;
    }
  }

  public static String getLabelEscapedJavaScript(String componentId, String label) {
    return StringEscapeUtils.escapeJavaScript(getLabel(componentId, label));
  }
  
  static public RequireJS addScripts(String module, String alias) {
    return addScripts(module, alias, "");
  }

  static public RequireJS addScripts(String[] scripts) {
    return addScripts(null, null, scripts);
  }

  static public RequireJS addScripts(String module, String alias, String... scripts) {
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    RequireJS requireJS;
    if (CommonUtils.isEmpty(module) == false) {
      if (CommonUtils.isEmpty(alias) == false) {
        requireJS = pContext.getJavascriptManager().require("SHARED/" + module, alias);
      } else {
        requireJS = pContext.getJavascriptManager().require("SHARED/" + module);
      }
    } else {
      requireJS = pContext.getJavascriptManager().getRequireJS();
    }
    if(scripts != null) {
      String script;
      for (int i = 0; i < scripts.length; i++) {
        script = scripts[i];
        if (CommonUtils.isEmpty(script) == false) {
          requireJS.addScripts(script + ";");
        }
      }
    }
    return requireJS;
  }
  
  /**
   * Build the script tag HTML
   * 
   * @param scriptName
   * @param index
   * @return The script tag
   */
  private static String makeScript(String scriptName, int index) {
    return MessageFormat.format(SCRIPT_PATTERN, scriptName, index);
  }
  
  /**
   * Attach the list javaScript files of SyntaxHighlighter
   * @param languageOption The list of files name javaScript
   * @return The list files attach for SyntaxHighlighter
   */
  static public String attachJSSyntaxHighlighter(List<String> languageOption) {
    StringBuilder scripts = new StringBuilder();
    if(languageOption != null && languageOption.size() > 0) {
      int index = 0;
      //Attach javaScript core of SyntaxHighlighter
      scripts.append(makeScript("shCore.js", (index++)))
             .append(makeScript("shAutoloader.js", (index++)));
      // Attach javaScript by language code (ex: java, php, script, html ...) 
      for (String lang : languageOption) {
        scripts.append(makeScript("shBrush" + lang + ".js", (index++)));
      }
      // Attach javaScript loader of SyntaxHighlighter
      scripts.append(makeScript("shLegacy.js", (index++)))
             .append(makeScript("load_syntaxhighlighter.js", (index++)));
      // Execute method javaScript to process SyntaxHighlighter
      try {
        String script = "setTimeout(function() {try {SyntaxHighlighter.initLoader();SyntaxHighlighter.all();" +
                        "dp.SyntaxHighlighter.HighlightAll('code');}catch(err){if(window.console && SyntaxHighlighter.config.strings.isAlert) {window.console.log(err);}}}, 500);";
        addScripts(new String[] { script });
      } catch (Exception e) {
        LOG.debug("Add JavaScript for using SyntaxHighlighter is unsuccessfully.", e);
      }
    }
    //
    return scripts.toString();
  }

  public static Space getSpaceByContext() {
    //
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    if (!pcontext.getSiteType().equals(SiteType.GROUP) ||
        !pcontext.getSiteName().startsWith(SpaceUtils.SPACE_GROUP)) {
      return null;
    }
    String requestPath = pcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
    Route route = ExoRouter.route(requestPath);

    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (route == null) {
      String groupId = pcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_SITE_NAME);
      return spaceService.getSpaceByGroupId(groupId);
    }

    //
    String spacePrettyName = route.localArgs.get("spacePrettyName");
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    if (space == null) {
      String groupId = String.format("%s/%s", SpaceUtils.SPACE_GROUP, spacePrettyName);
      space = spaceService.getSpaceByGroupId(groupId);
    }

    return space;
  }
  
}
