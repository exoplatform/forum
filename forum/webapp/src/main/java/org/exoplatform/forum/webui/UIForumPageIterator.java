/***************************************************************************
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.forum.webui;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.webui.popup.UIListTopicOld;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
   template = "app:/templates/forum/webui/UIForumPageIterator.gtmpl",
   events = {
     @EventConfig(listeners = UIForumPageIterator.GoPageActionListener.class)
   }
)
public class UIForumPageIterator extends UIContainer {
  
  private int         page                 = 1;

  private int         endTabPage           = 0;

  private int         beginTabPage         = 0;

  private boolean     isUpdateListTopicOld = false;

  private int pageSize = 0;

  private int currentPage = 0;

  private int available = 0;

  private int availablePage = 0;
  
  public UIForumPageIterator() throws Exception {
  }

  public void initPage(int pageSize, int currentPage, int available, int availablePage) {
    this.pageSize = pageSize;
    this.currentPage = currentPage;
    this.available = available;
    this.availablePage = availablePage;
  }
  
  public void setUpdateListTopicOld(boolean updateTwoTime) {
    this.isUpdateListTopicOld = updateTwoTime;
  }

  public boolean getUpdateListTopicOld() {
    return isUpdateListTopicOld;
  }

  protected List<String> getTotalpage() throws Exception {
    if (this.page > availablePage)
      this.page = availablePage;
    long page = this.page;
    if (page <= 3) {
      beginTabPage = 1;
      if (availablePage <= 7)
        endTabPage = availablePage;
      else
        endTabPage = 7;
    } else {
      if (availablePage > (page + 3)) {
        endTabPage = (int) (page + 3);
        beginTabPage = (int) (page - 3);
      } else {
        endTabPage = availablePage;
        if (availablePage > 7)
          beginTabPage = availablePage - 6;
        else
          beginTabPage = 1;
      }
    }
    List<String> temp = new ArrayList<String>();
    for (int i = beginTabPage; i <= endTabPage; i++) {
      temp.add(ForumUtils.EMPTY_STR + i);
    }
    return temp;
  }

  public List<Integer> getInfoPage() throws Exception {
    List<Integer> temp = new ArrayList<Integer>();
    try {
      temp.add(pageSize);
      temp.add(currentPage);
      temp.add(available);
      temp.add(availablePage);
    } catch (Exception e) {
      temp.add(1);
      temp.add(1);
      temp.add(1);
      temp.add(1);
    }
    return temp;
  }

  public void setSelectPage(long page) {
    this.page = (int) page;
  }

  public int getPageSelected() {
    return this.page;
  }

  static public class GoPageActionListener extends EventListener<UIForumPageIterator> {
    public void execute(Event<UIForumPageIterator> event) throws Exception {
      UIForumPageIterator pageIterator = event.getSource();
      if ((UIComponent) pageIterator.getParent() instanceof UITopicDetail) {
        UITopicDetail topicDetail = pageIterator.getParent();
        topicDetail.setIdPostView("top");
      }
      String stateClick = event.getRequestContext().getRequestParameter(OBJECTID).trim();
      int presentPage = pageIterator.page;
      if (stateClick.equalsIgnoreCase("next")) {
        if (presentPage < pageIterator.availablePage) {
          pageIterator.page = presentPage + 1;
          if (pageIterator.getUpdateListTopicOld()) {
            UIListTopicOld listTopicOld = pageIterator.getParent();
            listTopicOld.setIsUpdate(true);
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(pageIterator.getParent());
        }
      } else if (stateClick.equalsIgnoreCase("previous")) {
        if (presentPage > 1) {
          pageIterator.page = presentPage - 1;
          if (pageIterator.getUpdateListTopicOld()) {
            UIListTopicOld listTopicOld = pageIterator.getParent();
            listTopicOld.setIsUpdate(true);
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(pageIterator.getParent());
        }
      } else if (stateClick.equalsIgnoreCase("last")) {
        if (presentPage != pageIterator.availablePage) {
          pageIterator.page = pageIterator.availablePage;
          if (pageIterator.getUpdateListTopicOld()) {
            UIListTopicOld listTopicOld = pageIterator.getParent();
            listTopicOld.setIsUpdate(true);
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(pageIterator.getParent());
        }
      } else if (stateClick.equalsIgnoreCase("first")) {
        if (presentPage != 1) {
          pageIterator.page = 1;
          if (pageIterator.getUpdateListTopicOld()) {
            UIListTopicOld listTopicOld = pageIterator.getParent();
            listTopicOld.setIsUpdate(true);
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(pageIterator.getParent());
        }
      } else {
        int temp = Integer.parseInt(stateClick);
        if (temp > 0 && temp <= pageIterator.availablePage && temp != presentPage) {
          pageIterator.page = temp;
          if (pageIterator.getUpdateListTopicOld()) {
            UIListTopicOld listTopicOld = pageIterator.getParent();
            listTopicOld.setIsUpdate(true);
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(pageIterator.getParent());
        }
      }
    }
  }
}
