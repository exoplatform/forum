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
import java.util.Arrays;
import java.util.List;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.forum.common.webui.UIFormScrollSelectBox;
import org.exoplatform.forum.common.webui.UIForumFilter;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

public class UICreateForm extends BaseUIForm {
  public static final String LOCATION_SELECT_BOX  = "location";

  public static final String FORUM_SELECT_BOX     = "forumId";

  private static final String INTRANER            = "intranet";

  public boolean              isStepOne            = true;

  public boolean              hasForumIntranet     = true;

  public boolean              hasForum             = true;

  public boolean              hasNext              = true;

  public String                parStatus            = "";

  public String                categoryIdOfSpaces   = "";

  public String                currentIntranet      = INTRANER;

  public CategoryFilter        onlyOneForum         = null;
  

  public List<String> allPortalNames       = new ArrayList<String>();

  public enum ACTION_TYPE {
    CREATE_POLL, CREATE_TOPIC
  }

  public void setParStatus(String parStatus) {
    this.parStatus = parStatus;
  }

  public String getParStatus() {
    return parStatus;
  }
  
  public boolean hasForum() {
    return hasForum;
  }

  public boolean hasNext() {
    return hasNext;
  }

  public UICreateForm() throws Exception {
    hasNext = true;
    isStepOne = true;
    currentIntranet = getIntranerSite();
    List<SelectItemOption<String>> list = new ArrayList<SelectItemOption<String>>();
    allPortalNames = CreateUtils.getAllPortalNames();
    
    String currentUser = UserHelper.getCurrentUser();
    ForumService forumService = getApplicationComponent(ForumService.class);
    
    List<CategoryFilter> filters = forumService.filterForumByName("_", currentUser, 2);
    int categoriesSize = filters.size();
    if(categoriesSize == 1 && filters.get(0).getForumFilters().size() == 1) {
      onlyOneForum = filters.get(0);      
    }
    
    hasForumIntranet = (categoriesSize > 0);
    if(hasForumIntranet) {
      list.add(new SelectItemOption<String>(currentIntranet, currentIntranet));
    }
    
    Category categoryIncludedSpace = forumService.getCategoryIncludedSpace();
    if(categoryIncludedSpace != null) {
      List<String> groupAndMembershipInfos = UserHelper.getAllGroupAndMembershipOfUser(null);
      this.categoryIdOfSpaces = categoryIncludedSpace.getId();
      ForumFilter filter = new ForumFilter(categoryIdOfSpaces, true);
      filter.userId(currentUser);
      List<Forum> forums = forumService.getForums(filter);

      SpaceService spaceService = getApplicationComponent(SpaceService.class);
      List<Space> spaces = spaceService.getLastAccessedSpace(currentUser, null, 0, forums.size());
      Forum forum;
      for (Space space : spaces) {
        forum = getForum(forums, space.getPrettyName(), groupAndMembershipInfos);
        if (forum != null) {
          list.add(new SelectItemOption<String>(space.getDisplayName(), forum.getId()));
        }
      }
    }
    
    if(list.size() > 0) {
      UIFormScrollSelectBox formSelectBox = new UIFormScrollSelectBox(LOCATION_SELECT_BOX, LOCATION_SELECT_BOX, list);
      if(hasForumIntranet) {
        formSelectBox.setValue(currentIntranet);
      }
      formSelectBox.setOnChange("OnChangeLocal");
      addUIFormInput(formSelectBox);
      hasForum = true;
      setActions(new String[]{"Next","Cancel"});
    } else {
      hasForum = false;
      setActions(new String[]{"Cancel"});
    }
  }
  
  private Forum getForum(List<Forum> forums, String spacePrettyName, List<String> listOfCanviewrs) {
    for (Forum forum : forums) {
      if(forum.getId().equals(Utils.FORUM_SPACE_ID_PREFIX + spacePrettyName)) {
        if(forum.getIsLock() || 
            (forum.getCreateTopicRole() != null && Utils.hasPermission(listOfCanviewrs, Arrays.asList(forum.getCreateTopicRole())) == false)) {
          return null;
        }
        return forum;
      }
    }
    return null;
  }

  public String getIntranerSite() {
    String portalName = CreateUtils.getCurrentPortalName();
    if (portalName.equalsIgnoreCase(SiteType.GROUP.name())) {
      return INTRANER;
    }
    return portalName;
  }
  
  public static void nextAction(UICreateForm uiForm, ACTION_TYPE type, WebuiRequestContext context) throws Exception {
    if (uiForm.isStepOne && uiForm.hasForumIntranet) {
      uiForm.isStepOne = false;
      if(uiForm.onlyOneForum == null){
        UIForumFilter forumFilter = uiForm.getUIForumFilter(FORUM_SELECT_BOX);
        if (forumFilter == null) {
          forumFilter = new UIForumFilter(FORUM_SELECT_BOX, FORUM_SELECT_BOX);
          forumFilter.setOnChange("OnChangeFilter");
          uiForm.addUIFormInput(forumFilter);
        }
        forumFilter.setRendered(true);
        uiForm.hasNext = false;
        context.addUIComponentToUpdateByAjax(uiForm);
      } else {
        nextAction(uiForm, type, context);
      }
    } else {
      String location = uiForm.getUIFormScrollSelectBox(LOCATION_SELECT_BOX).getValue();
      String subUrl = null;
      if (uiForm.currentIntranet.equals(location)) {
        if (uiForm.onlyOneForum == null) {
          UIForumFilter forumFilter = uiForm.getUIForumFilter(FORUM_SELECT_BOX);

          String categoryId = forumFilter.getCategoryId();
          String forumId = forumFilter.getForumId();

          if (!CommonUtils.isEmpty(forumId)) {
            subUrl = urlBuilder(categoryId, forumId, type, location);
          } else {
            uiForm.warning("UICreateList.label.RequireSelectForum");
          }

        } else {
          String categoryId = uiForm.onlyOneForum.getCategoryId();
          String forumId = uiForm.onlyOneForum.getForumFilters().get(0).getForumId();
          subUrl = urlBuilder(categoryId, forumId, type, location);
          uiForm.onlyOneForum = null;
        }

      } else {
        subUrl = urlBuilder(uiForm.categoryIdOfSpaces, location, type, null);
      }

      if (!CommonUtils.isEmpty(subUrl)) {
        
        uiForm.log.info(uiForm.getId() + "::sendRedirect =" + subUrl);

        PortalRequestContext pContext = Util.getPortalRequestContext();
        pContext.getJavascriptManager().getRequireJS().addScripts("(function(){ window.location.href = '" + subUrl + "';})();");
        uiForm.isStepOne = true;
        if (uiForm.getChildById(FORUM_SELECT_BOX) != null) {
          uiForm.removeChildById(FORUM_SELECT_BOX);
        }
        context.addUIComponentToUpdateByAjax(uiForm);

        UIContainer container = uiForm.getParent();
        List<UIComponent> uilist = container.getChildren();
        List<String> lisID = new ArrayList<String>();
        if (uilist.size() != 0) {
          for (UIComponent uIComponent : uilist) {
            lisID.add(uIComponent.getId());
          }
          for (String id : lisID) {
            container.removeChildById(id);
          }
        }
        context.addUIComponentToUpdateByAjax(container);
      }
    }
  }
  
  private static String urlBuilder(String categoryId, String forumId, ACTION_TYPE type, String siteName) {
    String urlBuilder = CreateUtils.buildLink(categoryId, forumId, null, siteName);
    if (!CommonUtils.isEmpty(urlBuilder)) {
      String actionType = (type.equals(ACTION_TYPE.CREATE_TOPIC)) ? "?hasCreateTopic=true" : "?hasCreatePoll=true";
      urlBuilder += actionType;
    }
    return urlBuilder;
  }
  
  static public class OnChangeFilterActionListener extends EventListener<UICreateForm> {

    public void execute(Event<UICreateForm> event) throws Exception {
      UICreateForm createForm = event.getSource();
      createForm.isStepOne = false;
      createForm.hasNext = true;
      event.getRequestContext().addUIComponentToUpdateByAjax(createForm);
    }
  }

  static public class CancelActionListener extends EventListener<UICreateForm> {
    
    public void execute(Event<UICreateForm> event) throws Exception {
      UICreateForm createForm = event.getSource();
      createForm.isStepOne = true;
      if (createForm.getChildById(FORUM_SELECT_BOX) != null) {
        createForm.removeChildById(FORUM_SELECT_BOX);
      }
      WebuiRequestContext ctx = event.getRequestContext();
      Event<UIComponent> cancelEvent = createForm.getParent().createEvent("Cancel", Event.Phase.DECODE, ctx);
      if (cancelEvent != null) {
        cancelEvent.broadcast();
      }
      
    }
  }

}
