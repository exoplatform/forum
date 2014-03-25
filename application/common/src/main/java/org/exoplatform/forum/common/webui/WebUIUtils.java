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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

public class WebUIUtils {
  private static Log LOG = ExoLogger.getLogger(WebUIUtils.class);
  private static final String SCRIPT = "<script src=\"/forumResources/syntaxhighlighter/Scripts/SCRIPT_NAME\" type=\"text/javascript\"></script>";

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
  
  static public String addScriptSyntaxhighlighter() {
    StringBuilder scripts = new StringBuilder();
    //
    scripts.append(SCRIPT.replace("SCRIPT_NAME", "shCore.js"))
           .append(SCRIPT.replace("SCRIPT_NAME", "shAutoloader.js"))
           .append(SCRIPT.replace("SCRIPT_NAME", "shLegacy.js"))
           .append(SCRIPT.replace("SCRIPT_NAME", "load_syntaxhighlighter.js"));
    //
    addScripts(new String[] { "SyntaxHighlighter.initLoader()", "SyntaxHighlighter.all()", "dp.SyntaxHighlighter.HighlightAll('code')" });
    //
    return scripts.toString();
  }
  
}
