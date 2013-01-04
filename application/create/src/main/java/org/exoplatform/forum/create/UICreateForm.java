/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.forum.create;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Jan 4, 2013  
 */
public class UICreateForm extends BaseUIForm{
  public static final String LOCALTION_SELEXT_BOX = "location";

  public static final String FORUM_SELEXT_BOX     = "forumId";

  public boolean             isStepOne             = true;

  public enum ACTION_TYPE {
    CREATE_POLL, CREATE_TOPIC
  }

  public UICreateForm() {
    isStepOne = true;
    List<SelectItemOption<String>> list = new ArrayList<SelectItemOption<String>>();
    List<String> allPortalNames = Utils.getAllPortalNames();
    for (String portalName : allPortalNames) {
      list.add(new SelectItemOption<String>(portalName, portalName));
    }
    UIFormSelectBox formSelectBox = new UIFormSelectBox(LOCALTION_SELEXT_BOX, LOCALTION_SELEXT_BOX, list);
    formSelectBox.setValue(Utils.getCurrentPortalName());
    addUIFormInput(formSelectBox);
  }
  
  public static void nextAction(UICreateForm uiForm, ACTION_TYPE type, WebuiRequestContext context) throws Exception {
    if (uiForm.isStepOne) {
      uiForm.isStepOne = false;
      UIForumFilter forumFilter = new UIForumFilter(FORUM_SELEXT_BOX, FORUM_SELEXT_BOX);
      uiForm.addUIFormInput(forumFilter);
      context.addUIComponentToUpdateByAjax(uiForm);
    } else {
      String forumId = ((UIForumFilter) uiForm.getUIInput(FORUM_SELEXT_BOX)).getValue();
      if (!CommonUtils.isEmpty(forumId)) {
        String portalName = uiForm.getUIFormSelectBox(LOCALTION_SELEXT_BOX).getValue();

        String containerName = uiForm.getApplicationComponent(ExoContainerContext.class).getPortalContainerName();
        PortalRequestContext pContext = Util.getPortalRequestContext();
        String fullUrl = ((HttpServletRequest) pContext.getRequest()).getRequestURL().toString();
        String subUrl = fullUrl.substring(0, fullUrl.indexOf(containerName) + containerName.length());
        subUrl += CommonUtils.SLASH + portalName + "/forum/forum/" + forumId + "?hasCreateTopic=true";
        
        String actionType = (type.equals(ACTION_TYPE.CREATE_TOPIC)) ? "?hasCreateTopic=true" :"?hasCreatePoll=true";
        subUrl += actionType;

        System.out.println("\n\n\n=========> subUrl: " + subUrl);
        pContext.getJavascriptManager().getRequireJS().addScripts("(function(){ window.location.href = '" + subUrl + "';})();");
        context.addUIComponentToUpdateByAjax(uiForm);

        Event<UIComponent> cancelEvent = uiForm.createEvent("Cancel", Event.Phase.DECODE, pContext);
        if (cancelEvent != null) {
          cancelEvent.broadcast();
        }

      } else {
        uiForm.warning("RequireSelectForum");
      }
    }
  }
  
  static public class CancelActionListener extends EventListener<UICreateForm> {

    public void execute(Event<UICreateForm> event) throws Exception {
      UICreateForm createForm = event.getSource();
      createForm.isStepOne = true;
      if (createForm.getChildById(FORUM_SELEXT_BOX) != null) {
        createForm.removeChildById(FORUM_SELEXT_BOX);
      }
      WebuiRequestContext ctx = event.getRequestContext();
      Event<UIComponent> cancelEvent = createForm.getParent().createEvent("Cancel", Event.Phase.DECODE, ctx);
      if (cancelEvent != null) {
        cancelEvent.broadcast();
      }

    }
  }

}
