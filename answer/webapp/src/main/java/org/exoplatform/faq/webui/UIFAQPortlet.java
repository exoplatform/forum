/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 ***************************************************************************/
package org.exoplatform.faq.webui;

import javax.portlet.PortletMode;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.popup.UIFAQSettingForm;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.common.router.ExoRouter;
import org.exoplatform.social.common.router.ExoRouter.Route;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template = "app:/templates/faq/webui/UIFAQPortlet.gtmpl"
)
public class UIFAQPortlet extends UIPortletApplication {
  private final static String SLASH     = "/".intern();
  
  private final static String SPACE_PRETTY_NAME = "spacePrettyName";

  private boolean             isInSpace         = false;

  private String              pathOfCateSpace   = null;

  public UIFAQPortlet() throws Exception {
    addChild(UIViewer.class, null, null);
  }

  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext portletReqContext = (PortletRequestContext) context;
    if (portletReqContext.getApplicationMode() == PortletMode.VIEW) {
      removeChild(UIFAQSettingForm.class);
      UIViewer uiViewer = getChild(UIViewer.class);
      if (uiViewer == null) {
        uiViewer = addChild(UIViewer.class, null, null).setRendered(true);
      }
      if (FAQUtils.isFieldEmpty(context.getRequestParameter(OBJECTID)) && 
                       !context.getParentAppRequestContext().useAjax() && StringUtils.EMPTY.equals(uiViewer.getPath())) {
        uiViewer.setPath(getPathOfCateSpace());
      }
    } else if (portletReqContext.getApplicationMode() == PortletMode.EDIT) {
      removeChild(UIViewer.class);
      if (getChild(UIFAQSettingForm.class) == null) {
        UIFAQSettingForm settingForm = addChild(UIFAQSettingForm.class, null, null);
        settingForm.defaulValue();
      }
    }
    super.processRender(app, context);
  }
  
  private Space getSpace() {
    PortalRequestContext plcontext = Util.getPortalRequestContext();
    String requestPath = plcontext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
    Route route = ExoRouter.route(requestPath);
    if (route == null) {
      return null;
    }
    //
    String spacePrettyName = route.localArgs.get(SPACE_PRETTY_NAME);

    if (spacePrettyName != null) {
      SpaceService sService = getApplicationComponent(SpaceService.class);
      Space space = sService.getSpaceByPrettyName(spacePrettyName);
      return space;
    }
    return null;
  }
  
  public String getPathOfCateSpace() {
    if (FAQUtils.isFieldEmpty(pathOfCateSpace)) {
      Space space = getSpace();
      if (space != null) {
        isInSpace = true;
        pathOfCateSpace = buildPathOfSpace(space.getPrettyName());
      } else {
        isInSpace = false;
        pathOfCateSpace = Utils.CATEGORY_HOME;
      }
    }
    return pathOfCateSpace;
  }
  
  public String getDisplaySpaceName() {
    Space space = getSpace();
    if (space != null) {
      isInSpace = true;
      pathOfCateSpace = buildPathOfSpace(space.getPrettyName());
      return space.getDisplayName();
    }
    pathOfCateSpace = Utils.CATEGORY_HOME;
    return CommonUtils.AMP_SPACE;
  }
  
  public String buildPathOfSpace(String spaceName) {
    StringBuilder sb = new StringBuilder();
    sb.append(Utils.CATEGORY_HOME).append(SLASH).append(Utils.CATE_SPACE_ID_PREFIX).append(spaceName);
    return sb.toString();
  }

  public boolean isInSpace() {
    return isInSpace;
  }
}
