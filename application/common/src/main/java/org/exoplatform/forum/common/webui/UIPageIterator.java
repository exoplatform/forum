/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.forum.common.utils.AbstractListAccess;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
  template = "system:/groovy/webui/core/UIPageIterator.gtmpl",
  events = @EventConfig(listeners = UIPageIterator.ShowPageActionListener.class)
)
public class UIPageIterator<E> extends UIComponent {
  private AbstractListAccess<E>    pageList_ = null;

  private Set<String> selectedItems = new HashSet<String>();

  public UIPageIterator() {
  }

  public void setListAccess(AbstractListAccess<E> pageList, int pageSize) throws Exception {
    pageList_ = pageList;
    //
    setUserPerPage(pageSize);
  }

  public AbstractListAccess<E> getListAccess() {
    return pageList_;
  }

  public int getAvailablePage() throws Exception {
    return pageList_.getTotalPages();
  }

  public int getCurrentPage() throws Exception {
    return pageList_.getCurrentPage();
  }

  public List<E> getCurrentPageData() throws Exception {
    E[] items = pageList_.load(getCurrentPage());
    return Arrays.asList(items);
  }

  public int getAvailable() throws Exception {
    return pageList_.getSize();
  }

  public int getFrom() {
    return pageList_.getFrom();
  }

  public int getTo() {
    return pageList_.getTo();
  }

  public E getObjectInPage(int index) throws Exception {
    return getCurrentPageData().get(index);
  }

  public void setCurrentPage(int page) throws Exception {
    pageList_.setCurrentPage(page);
  }

  public void setUserPerPage(int userPerPage) throws Exception {
    pageList_.initialize(userPerPage, getCurrentPage());
  }

  public void setSelectedItem(String key, boolean value) {
    if (value == false && this.selectedItems.contains(key)) {
      selectedItems.remove(key);
    } else if (value) {
      selectedItems.add(key);
    }
  }

  public Set<String> getSelectedItems() {
    return selectedItems;
  }

  public boolean isSelectedItem(String key) {
    return selectedItems.contains(key);
  }

  public static class ShowPageActionListener extends EventListener<UIPageIterator<?>> {
    public void execute(Event<UIPageIterator<?>> event) throws Exception {
      UIPageIterator<?> uiPageIterator = event.getSource();
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiPageIterator.setCurrentPage(page);
      UIComponent parent = uiPageIterator.getParent();
      if (parent == null) {
        return;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(parent);
      parent.broadcast(event, event.getExecutionPhase());
    }
  }
}
