/***************************************************************************
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.service.ForumLinkData;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.input.UICheckBoxInput;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/forum/webui/popup/UISelectItemForumForm.gtmpl",
  events = {
    @EventConfig(listeners = UISelectItemForum.AddActionListener.class), 
    @EventConfig(listeners = UISelectItemForum.CancelActionListener.class,phase = Phase.DECODE)
  }
)
public class UISelectItemForum extends BaseForumForm implements UIPopupComponent {
  List<ForumLinkData>                      forumLinks       = null;

  private Map<String, List<ForumLinkData>> mapListForum     = new HashMap<String, List<ForumLinkData>>();

  private List<String>                     listIdIsSelected = new ArrayList<String>();

  public UISelectItemForum() {
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void initSelectForum(List<String> listIdIsSelected, String userId) throws Exception {
    this.listIdIsSelected = listIdIsSelected;
    forumLinks = new ArrayList<ForumLinkData>();
    String cateQuery = new StringBuffer("[(@").append(Utils.EXO_ID)
                        .append("!='").append(Utils.CATEGORY_SPACE_ID_PREFIX).append("')]").toString();
    forumLinks.addAll(getForumService().getAllLink(cateQuery, ForumUtils.EMPTY_STR));
    
    //
    List<String> groupIds = Utils.getGroupSpaceOfUser(userProfile.getUserId());
    String strQuryForum = new StringBuffer("[").append(Utils.buildQueryForumInSpaceOfUser(userId, groupIds))
                                               .append("]").toString();
    if (ForumUtils.isEmpty(strQuryForum) == false) {
      cateQuery = cateQuery.replace("!=", "=");
      forumLinks.addAll(getForumService().getAllLink(cateQuery, strQuryForum));
    }
    
    //
    initCheckboxInput();
  }
  
  private void initCheckboxInput() {
    mapListForum.clear();
    List<ForumLinkData> linkForum;
    String cateId;
    for (ForumLinkData forumLink : forumLinks) {
      if (forumLink.getType().equals(Utils.CATEGORY)) {
        cateId = forumLink.getId();
        linkForum = new ArrayList<ForumLinkData>();
        for (ForumLinkData forumlist : forumLinks) {
          if (forumlist.getType().equals(Utils.FORUM) && forumlist.getPath().indexOf(cateId) >= 0) {
            linkForum.add(forumlist);
            String inputId = forumlist.getPath().replace("/", "");
            UICheckBoxInput checkbox = getUICheckBoxInput(inputId);
            if (checkbox == null) {
              checkbox = new UICheckBoxInput(inputId, inputId, false);
              addUIFormInput(checkbox);
            }
            checkbox.setChecked(getCheckedForum(forumlist.getId()));
          }
        }
        mapListForum.put(cateId, linkForum);
      }
    }
  }
  

  protected List<ForumLinkData> getForumLinks() throws Exception {
    return forumLinks;
  }

  protected boolean getCheckedForum(String forumId) {
    return listIdIsSelected.contains(forumId) ? true : false;
  }

  protected List<ForumLinkData> getForums(String categoryId) {
    List<ForumLinkData> forumLinkDatas = mapListForum.get(categoryId);
    return (forumLinkDatas != null) ? forumLinkDatas : new ArrayList<ForumLinkData>();
  }

  private ForumLinkData getForumLinkData(String id) throws Exception {
    for (ForumLinkData linkData : forumLinks) {
      if (linkData.getPath().replace("/", "").equals(id))
        return linkData;
    }
    return null;
  }

  static public class AddActionListener extends EventListener<UISelectItemForum> {
    public void execute(Event<UISelectItemForum> event) throws Exception {
      UISelectItemForum uiForm = event.getSource();
      List<String> listIdSelected = new ArrayList<String>();
      List<UIComponent> children = uiForm.getChildren();
      for (UIComponent child : children) {
        if (child instanceof UICheckBoxInput) {
          if (((UICheckBoxInput) child).isChecked()) {
            ForumLinkData linkData = uiForm.getForumLinkData(child.getId());
            if(linkData != null){
              listIdSelected.add(linkData.getName() + "(" + linkData.getPath());
            }
          }
        }
      }
      UIModeratorManagementForm managementForm = uiForm.getAncestorOfType(UIForumPortlet.class).findFirstComponentOfType(UIModeratorManagementForm.class);
      managementForm.setModForunValues(listIdSelected);
      event.getRequestContext().addUIComponentToUpdateByAjax(managementForm);
      uiForm.cancelChildPopupAction();
    }
  }

  static public class CancelActionListener extends EventListener<UISelectItemForum> {
    public void execute(Event<UISelectItemForum> event) throws Exception {
      event.getSource().cancelChildPopupAction();
    }
  }
}
