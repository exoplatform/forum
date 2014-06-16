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

import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.popup.UIFAQSettingForm;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template = "app:/templates/faq/webui/UIFAQPortlet.gtmpl"
)
public class UIFAQPortlet extends UIPortletApplication {
  private final static String SLASH             = "/";

  private final static String SPACE_PRETTY_NAME = "spacePrettyName";

  private final static String CATEGORY_ID       = "categoryId";

  public UIFAQPortlet() throws Exception {
    addChild(UIViewer.class, null, null);
    UIPopupAction uiPopup = addChild(UIPopupAction.class, null, null);
    uiPopup.setId("UIFAQPopupAction");
    uiPopup.getChild(UIPopupWindow.class).setId("UIFAQPopupWindow");
  }

  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext portletReqContext = (PortletRequestContext) context;
    if (portletReqContext.getApplicationMode() == PortletMode.VIEW) {
      removeChild(UIFAQSettingForm.class);
      UIViewer uiViewer = getChild(UIViewer.class);
      if (uiViewer == null) {
        uiViewer = addChild(UIViewer.class, null, null).setRendered(true);
      }
      //
      renderPortletByURL(uiViewer);

    } else if (portletReqContext.getApplicationMode() == PortletMode.EDIT) {
      removeChild(UIViewer.class);
      if (getChild(UIFAQSettingForm.class) == null) {
        UIFAQSettingForm settingForm = addChild(UIFAQSettingForm.class, null, null);
        settingForm.defaulValue();
      }
    }
    super.processRender(app, context);
  }
  
  public void renderPortletByURL(UIViewer uiViewer) throws Exception {
    try {
      PortalRequestContext portalContext = Util.getPortalRequestContext();
      String categoryId = portalContext.getRequestParameter(CATEGORY_ID);
      if (FAQUtils.isFieldEmpty(portalContext.getRequestParameter(OBJECTID)) && 
            FAQUtils.isFieldEmpty(categoryId) && portalContext.useAjax() == false &&
                FAQUtils.isFieldEmpty(uiViewer.getPath())) {
        //
        uiViewer.setPath(getPathOfCateSpace());
      } else if (FAQUtils.isFieldEmpty(categoryId) == false) {
        uiViewer.setCategoryId(categoryId);
      } else if (FAQUtils.isFieldEmpty(uiViewer.getPath())) {
        uiViewer.setPath(Utils.CATEGORY_HOME);
      }
    } catch (Exception e) {
      log.error("can not render the selected category", e);
    }
  }
  
  private Space getSpace() {
    return WebUIUtils.getSpaceByContext();
  }
  
  public String getPathOfCateSpace() {
    Space space = getSpace();
    if (space != null) {
      return buildPathOfSpace(space.getGroupId());
    } else {
      return Utils.CATEGORY_HOME;
    }
  }
  
  public String getDisplaySpaceName() {
    Space space = getSpace();
    if (space != null) {
      return space.getDisplayName();
    }
    return CommonUtils.AMP_SPACE;
  }
  
  public String buildPathOfSpace(String spaceGroupId) {
    String groupId = spaceGroupId.replaceAll(SpaceUtils.SPACE_GROUP + SLASH, CommonUtils.EMPTY_STR);
    StringBuilder sb = new StringBuilder();
    sb.append(Utils.CATEGORY_HOME).append(SLASH).append(Utils.CATE_SPACE_ID_PREFIX).append(groupId);
    return sb.toString();
  }

  public boolean isInSpace() {
    return (getSpace() != null);
  }
}
