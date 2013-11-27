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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.answer.rendering.RenderHelper;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.popup.UIViewUserProfile;
import org.exoplatform.faq.service.CategoryInfo;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.utils.TimeConvertUtils;

@ComponentConfig(
    events = { 
        @EventConfig(listeners = UIViewer.ViewProfileActionListener.class),
        @EventConfig(listeners = UIViewer.ChangePathActionListener.class)
    }
)
public class UIViewer extends UIContainer {
  private FAQService   fAqService;

  private String       path         = Utils.CATEGORY_HOME;

  protected boolean    useAjax      = false;

  private Map<String, String> dataLinks = new HashMap<String, String>();

  private RenderHelper renderHelper = new RenderHelper();
  
  private static String PORTLET_URL = null;

  public UIViewer() {
    fAqService = CommonUtils.getComponent(FAQService.class);
  }

  public String getPath() {
    return path;
  }

  public boolean isInSpace() {
    return getAncestorOfType(UIFAQPortlet.class).isInSpace();
  }

  public void setPath(String path) {
    this.path = path;
  }

  protected List<String> arrangeList(List<String> list) {
    List<String> newList = new ArrayList<String>();
    if (list.isEmpty() || list.size() == 0) {
      newList.add("<img src=\"/faq/skin/DefaultSkin/webui/background/HomeIcon.gif\" alt=\"" + Utils.CATEGORY_HOME + "\"/>");
    } else {
      for (int i = (list.size() - 1); i >= 0; i--) {
        if (i == (list.size() - 1)) {
          newList.add("<img src=\"/faq/skin/DefaultSkin/webui/background/HomeIcon.gif\" alt=\"" + list.get(i) + "\"/>");
        } else {
          newList.add(list.get(i));
        }
      }
    }
    return newList;
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return new FAQResourceResolver();
  }

  public String getTemplate() {
    return "FAQViewerTemplate";
  }

  protected CategoryInfo getCategoryInfo() throws Exception {
    useAjax = FAQUtils.getUseAjaxFAQPortlet();
    if (isInSpace()) {
      path = getAncestorOfType(UIFAQPortlet.class).getPathOfCateSpace();
    }
    return fAqService.getCategoryInfo(path, FAQUtils.getCategoriesIdFAQPortlet());
  }
  
  protected String getDisplaySpaceName() {
    return getAncestorOfType(UIFAQPortlet.class).getDisplaySpaceName();
  }
  
  protected String render(String s) {
    Question question = new Question();
    question.setDetail(s);
    return renderHelper.renderQuestion(question);
  }

  protected String getURL(String path) {
    String id = path.substring(path.lastIndexOf("/") + 1);
    dataLinks.put(id, path);
    if(PORTLET_URL == null) {
      PORTLET_URL = FAQUtils.getPortletURI();
    }
    return new StringBuffer(PORTLET_URL).append("/?categoryId=").append(id).toString();
  }
  
  public void setCategoryId(String categoryId) throws Exception {
    String path = dataLinks.get(categoryId);
    if (FAQUtils.isFieldEmpty(path)) {
      this.path = fAqService.getCategoryById(categoryId).getPath();
    } else {
      this.path = path;
    }
  }
  
  protected String convertXTimeAgo(Date date) {
    return TimeConvertUtils.convertXTimeAgo(date, "EEE, MMM dd, yyyy", TimeConvertUtils.DAY);
  }

  static public class ChangePathActionListener extends EventListener<UIViewer> {
    public void execute(Event<UIViewer> event) throws Exception {
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      UIViewer viewer = event.getSource();
      viewer.setPath(path);
      event.getRequestContext().addUIComponentToUpdateByAjax(viewer);
    }
  }

  static public class ViewProfileActionListener extends EventListener<UIViewer> {
    public void execute(Event<UIViewer> event) throws Exception {
      UIViewer viewer = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      userId = CommonUtils.decodeSpecialCharToHTMLnumber(userId);
      User user = UserHelper.getUserByUserId(userId);
      UIFAQPortlet portlet = viewer.getParent();
      if (user != null) {
        UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
        UIPopupContainer popupContainer = popupAction.createUIComponent(UIPopupContainer.class, null, null);
        UIViewUserProfile viewUserProfile = popupContainer.addChild(UIViewUserProfile.class, null, null);
        popupContainer.setId("ViewUserProfile");
        viewUserProfile.setUser(user);
        popupAction.activate(popupContainer, 680, 350);
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
      } else {
        portlet.addMessage(new ApplicationMessage("UIViewer.msg.user-is-not-exist",
                            new String[] { (userId.contains(Utils.DELETED)) ? userId.substring(0, userId.indexOf(Utils.DELETED)) : userId },
                            ApplicationMessage.WARNING));
        return;
      }
    }
  }

}
