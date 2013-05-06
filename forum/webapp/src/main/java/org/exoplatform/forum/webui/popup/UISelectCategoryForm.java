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
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.List;

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
    template = "app:/templates/forum/webui/popup/UISelectCategoryForm.gtmpl",
    events = {
      @EventConfig(listeners = UISelectCategoryForm.AddActionListener.class), 
      @EventConfig(listeners = UISelectCategoryForm.CancelActionListener.class,phase = Phase.DECODE)
    }
  )
public class UISelectCategoryForm extends BaseForumForm implements UIPopupComponent {
  private List<String> listIdIsSelected = new ArrayList<String>();

  List<ForumLinkData>  forumLinks       = new ArrayList<ForumLinkData>();

  public UISelectCategoryForm() {
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void setSelectCateId(List<String> listIdIsSelected) throws Exception {
    this.listIdIsSelected = listIdIsSelected;
    String cateQuery = new StringBuffer("[(@").append(Utils.EXO_ID)
        .append("!='").append(Utils.CATEGORY_SPACE_ID_PREFIX).append("')]").toString();
    forumLinks = getForumService().getAllLink(cateQuery, ForumUtils.EMPTY_STR);
    String cateId;
    for (ForumLinkData forumLink : forumLinks) {
      if (forumLink.getType().equals(Utils.CATEGORY)) {
        cateId = forumLink.getId();
        UICheckBoxInput checkbox = getUICheckBoxInput(cateId);
        if (checkbox == null) {
          checkbox = new UICheckBoxInput(cateId, cateId, false);
          addUIFormInput(checkbox);
        }
        checkbox.setChecked(getCheckedCategory(cateId));
      }
    }
  }

  protected List<ForumLinkData> getForumLinks() throws Exception {
    return forumLinks;
  }

  protected boolean getCheckedCategory(String cateId) {
    return listIdIsSelected.contains(cateId) ? true : false;
  }
  
  private String getNameForumLinkData(String id) throws Exception {
    for (ForumLinkData linkData : forumLinks) {
      if (linkData.getId().equals(id))
        return linkData.getName();
    }
    return null;
  }

  static public class AddActionListener extends EventListener<UISelectCategoryForm> {
    public void execute(Event<UISelectCategoryForm> event) throws Exception {
      UISelectCategoryForm uiForm = event.getSource();
      List<String> listIdSelected = new ArrayList<String>();
      List<UIComponent> children = uiForm.getChildren();
      for (UIComponent child : children) {
        if (child instanceof UICheckBoxInput) {
          if (((UICheckBoxInput) child).isChecked()) {
            listIdSelected.add(uiForm.getNameForumLinkData(child.getId()) + "(" + child.getId());
          }
        }
      }
      UIModeratorManagementForm managementForm = uiForm.getAncestorOfType(UIForumPortlet.class).findFirstComponentOfType(UIModeratorManagementForm.class);
      managementForm.setModCateValues(listIdSelected);
      event.getRequestContext().addUIComponentToUpdateByAjax(managementForm);
      uiForm.cancelChildPopupAction();
    }
  }

  static public class CancelActionListener extends EventListener<UISelectCategoryForm> {
    public void execute(Event<UISelectCategoryForm> event) throws Exception {
      event.getSource().cancelChildPopupAction();
    }
  }
}
