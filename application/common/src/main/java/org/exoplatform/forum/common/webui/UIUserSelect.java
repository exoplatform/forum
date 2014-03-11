/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.UserHelper.FilterType;
import org.exoplatform.forum.common.UserHelper.UserFilter;
import org.exoplatform.forum.common.utils.UserListAccess;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.organization.account.UIGroupSelector;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "system:/groovy/webui/organization/account/UIUserSelector.gtmpl",
  events = {
    @EventConfig(listeners = UIUserSelect.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.AddUserActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.SearchActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.SearchGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.SelectGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.FindGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.ShowPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelect.CloseActionListener.class, phase = Phase.DECODE)
  }
)
  
public class UIUserSelect extends BaseUIForm implements UIPopupComponent  {

  public static final String   ID_GROUP_SELECTOR = "UIPopupGroupSelector";
  public static final String   FIELD_KEYWORD     = "QuickSearch";
  public static final String   FIELD_FILTER      = "filter";
  public static final String   FIELD_GROUP       = "group";
  public static final String   USER_NAME         = FilterType.USER_NAME.getName();
  public static final String   LAST_NAME         = FilterType.LAST_NAME.getName();
  public static final String   FIRST_NAME        = FilterType.FIRST_NAME.getName();
  public static final String   EMAIL             = FilterType.EMAIL.getName();

  protected Map<String, User>  userData_          = new HashMap<String, User>();

  private boolean              isShowSearch_     = false;
  private boolean              isShowSearchGroup = false;
  private boolean              isShowSearchUser  = true;
  private boolean              isMultiSelect     = true;

  private String               selectedUsers;
  private String               permisionType;
  private String               selectedGroupId    = null;
  private int                  numberUserDisplay  = 10; 

  private List<User>           itemOfCurrentPage            = new ArrayList<User>();

  public UIPageIterator<User>  uiIterator_;
  public UIUserSelect() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_KEYWORD, FIELD_KEYWORD, null));
    addUIFormInput(new UIFormSelectBox(FIELD_FILTER, FIELD_FILTER, getFilters()));
    addUIFormInput(new UIFormStringInput(FIELD_GROUP, FIELD_GROUP, null));
    isShowSearch_ = true;
    //
    uiIterator_ = new UIPageIterator<User>();
    uiIterator_.setId("UISelectUserPage");
    //
    defaultUserList();
    //
    addChild(UIPopupWindow.class, null, ID_GROUP_SELECTOR).setWindowSize(540, 0);
    
    setActions(new String[] { "Add", "Close" });
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (CommonUtils.isEmpty(getKeyword()) && !CommonUtils.isEmpty(selectedGroupId) && uiIterator_ != null) {
      ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter("", null).setGroupId(selectedGroupId));
      uiIterator_.setListAccess(new UserListAccess(listAccess), numberUserDisplay);
    }
    //
    initResult();
    super.processRender(context);
  }

  private void defaultUserList() throws Exception {
    ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter("", null));
    uiIterator_.setListAccess(new UserListAccess(listAccess), numberUserDisplay);
  }

  private void initResult() throws Exception {
    this.itemOfCurrentPage = uiIterator_.getCurrentPageData();
    if (isMultiSelect()) {
      UICheckBoxInput checkBox;
      for (User user : itemOfCurrentPage) {
        checkBox = getUICheckBoxInput(user.getUserName());
        if (checkBox == null) {
          checkBox = new UICheckBoxInput(user.getUserName(), user.getUserName(), false);
          addUIFormInput(checkBox);
        }
        //
        checkBox.setChecked(uiIterator_.isSelectedItem(user.getUserName()));
      }
    }
    //
    if (isShowSearchGroup) {
      UIPopupWindow uiPopup = getChild(UIPopupWindow.class);
      if (uiPopup.getUIComponent() == null) {
        UIGroupSelector uiGroup = createUIComponent(UIGroupSelector.class, null, null);
        uiPopup.setUIComponent(uiGroup);
        uiGroup.setId("GroupSelector");
        uiGroup.getChild(UITree.class).setId("TreeGroupSelector");
        uiGroup.getChild(UIBreadcumbs.class).setId("BreadcumbsGroupSelector");
      }
    }
  }

  protected List<User> getData() {
    return itemOfCurrentPage;
  }

  public String getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(String selectedUsers) {
    this.selectedUsers = selectedUsers;
  }

  public void setMultiSelect(boolean isMultiSelect) {
    this.isMultiSelect = isMultiSelect;
  }

  public boolean isMultiSelect() {
    return isMultiSelect;
  }

  protected boolean getMulti() {
    return isMultiSelect;
  }

  public UIPageIterator<User> getUIPageIterator() {
    return uiIterator_;
  }

  public int getAvailablePage() throws Exception {
    return uiIterator_.getAvailablePage();
  }

  public int getCurrentPage() throws Exception {
    return uiIterator_.getCurrentPage();
  }
  
  public void setNumberUserDisplay(int numberUserDisplay) {
    this.numberUserDisplay = numberUserDisplay;
  }

  public String getSelectedGroupId() {
    return selectedGroupId;
  }

  public void setSpaceGroupId(String spaceGroupId) {
    this.selectedGroupId = spaceGroupId;
  }

  public String getPermisionType() {
    return permisionType;
  }

  public void setPermisionType(String permisionType) {
    this.permisionType = permisionType;
  }

  private List<SelectItemOption<String>> getFilters() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(getLabel(USER_NAME), USER_NAME));
    options.add(new SelectItemOption<String>(getLabel(LAST_NAME), LAST_NAME));
    options.add(new SelectItemOption<String>(getLabel(FIRST_NAME), FIRST_NAME));
    options.add(new SelectItemOption<String>(getLabel(EMAIL), EMAIL));
    return options;
  }

  public void setShowSearch(boolean isShowSearch) {
    this.isShowSearch_ = isShowSearch;
  }

  public boolean isShowSearch() {
    return isShowSearch_;
  }

  public void setShowSearchGroup(boolean isShowSearchGroup) {
    this.isShowSearchGroup = isShowSearchGroup;
  }

  public boolean isShowSearchGroup() {
    return isShowSearchGroup;
  }

  public void setShowSearchUser(boolean isShowSearchUser) {
    this.isShowSearchUser = isShowSearchUser;
  }

  public boolean isShowSearchUser() {
    return isShowSearchUser;
  }

  public String getFilterGroup() {
    return getUIStringInput(FIELD_GROUP).getValue();
  }

  public void setFilterGroup(String finterGroupId) {
    getUIStringInput(FIELD_GROUP).setValue(finterGroupId);
  }

  public static class AddActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiForm = event.getSource();

      uiForm.setSelectedItem();

      // get item from selected item map
      Set<String> items = uiForm.uiIterator_.getSelectedItems();
      if (items.size() == 0) {
        uiForm.warning("UIUserSelect.msg.user-required");
        return;
      }
      String result = items.toString().replace("[", "").replace("]", "").replaceAll(" ", "");

      uiForm.setSelectedUsers(result);
      
      uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
    }
  }

  public static class AddUserActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiForm = event.getSource();
      String userName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiForm.setSelectedUsers(userName);
      uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
    }
  }

  protected void updateCurrentPage(int page) throws Exception {
    uiIterator_.setCurrentPage(page);
  }

  public void setKeyword(String value) {
    getUIStringInput(FIELD_KEYWORD).setValue(value);
  }

  public String getKeyword() {
    return getUIStringInput(FIELD_KEYWORD).getValue();
  }

  private void setSelectedItem() throws Exception {
    for (User user : itemOfCurrentPage) {
      UICheckBoxInput input = getUICheckBoxInput(user.getUserName());
      if (input != null) {
        uiIterator_.setSelectedItem(user.getUserName(), input.isChecked());
      }
    }
  }

  public static class SelectGroupActionListener extends EventListener<UIGroupSelector> {
    public void execute(Event<UIGroupSelector> event) throws Exception {
      UIGroupSelector uiSelectGroupForm = event.getSource();
      UIUserSelect uiSelectUserForm = uiSelectGroupForm.<UIComponent> getParent().getParent();
      uiSelectUserForm.selectedGroupId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiSelectUserForm.setFilterGroup(uiSelectUserForm.selectedGroupId);
      uiSelectUserForm.setKeyword("");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
    }
  }

  public static class FindGroupActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiSelectUserForm = event.getSource();
      String groupId = uiSelectUserForm.getFilterGroup();
      if (!CommonUtils.isEmpty(groupId)) {
        if (UserHelper.getOrganizationService().getGroupHandler().findGroupById(groupId) != null) {
          uiSelectUserForm.selectedGroupId = groupId;
        }
      } else {
        //
        uiSelectUserForm.defaultUserList();
      }

      uiSelectUserForm.setKeyword("");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
    }
  }

  static public class SearchActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiForm = event.getSource();
      String type = uiForm.getUIFormSelectBox(FIELD_FILTER).getValue();
      FilterType filterType = FilterType.getType(type);
      //
      if (filterType == null) {
        return;
      }
      //
      String keyword = uiForm.getKeyword();
      String groupId = uiForm.getSelectedGroupId();
      
      UserFilter userFilter = new UserFilter(keyword, filterType);
      uiForm.uiIterator_.setListAccess(new UserListAccess(UserHelper.searchUser(userFilter.setGroupId(groupId))), uiForm.numberUserDisplay);
      //
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }
  
  public static class CloseActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiForm = event.getSource();
      uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
    }
  }

  public static class SearchGroupActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiForm = event.getSource();
      uiForm.getChild(UIPopupWindow.class).setShow(true);
    }
  }

  public static class ShowPageActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiSelectUserForm = event.getSource();
      uiSelectUserForm.setSelectedItem();

      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiSelectUserForm.updateCurrentPage(page);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
    }
  }

}
