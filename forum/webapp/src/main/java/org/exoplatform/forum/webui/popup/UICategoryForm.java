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
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIGroupSelector;
import org.exoplatform.forum.common.webui.UIPermissionPanel;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.common.webui.UISelector;
import org.exoplatform.forum.common.webui.UIUserSelect;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIBreadcumbs;
import org.exoplatform.forum.webui.UICategory;
import org.exoplatform.forum.webui.UICategoryContainer;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.PositiveNumberFormatValidator;

@ComponentConfigs ( {
        @ComponentConfig(
            lifecycle = UIFormLifecycle.class,
            template = "app:/templates/forum/webui/popup/UICategoryForm.gtmpl",
            events = {
              @EventConfig(listeners = UICategoryForm.SaveActionListener.class), 
              @EventConfig(listeners = UICategoryForm.AddPrivateActionListener.class, phase=Phase.DECODE),
              @EventConfig(listeners = UICategoryForm.CancelActionListener.class, phase=Phase.DECODE),
              @EventConfig(listeners = UICategoryForm.AddValuesUserActionListener.class, phase=Phase.DECODE),
              @EventConfig(listeners = UICategoryForm.SelectTabActionListener.class, phase=Phase.DECODE)
            }
        )
      ,
        @ComponentConfig(
             id = "UICategoryUserPopupWindow",
             type = UIPopupWindow.class,
             template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
             events = {
               @EventConfig(listeners = UICategoryForm.ClosePopupActionListener.class, name = "ClosePopup")  ,
               @EventConfig(listeners = UICategoryForm.AddActionListener.class, name = "Add", phase = Phase.DECODE),
               @EventConfig(listeners = UICategoryForm.CloseActionListener.class, name = "Close", phase = Phase.DECODE)
             }
        )
    }
)
public class UICategoryForm extends BaseForumForm implements UIPopupComponent, UISelector {
  public static final String CATEGORY_DETAIL_TAB          = "DetailTab";

  public static final String FIELD_CATEGORYTITLE_INPUT    = "CategoryTitle";

  public static final String FIELD_CATEGORYORDER_INPUT    = "CategoryOrder";

  public static final String FIELD_DESCRIPTION_INPUT      = "Description";

  public static final String FIELD_USERPRIVATE_MULTIVALUE = "UserPrivate";
  
  public static final String PERMISSION_TAB      = "PermissionTab";

  public static final String MODERAROR   = "moderators";

  public static final String VIEWER      = "Viewer";

  public static final String POSTABLE    = "Postable";

  public static final String TOPICABLE   = "Topicable";

  public static final String USER_SELECTOR_POPUPWINDOW    = "UICategoryUserPopupWindow";

  private String             categoryId                   = ForumUtils.EMPTY_STR;

  private int                id                           = 0;

  private boolean            isDoubleClickSubmit          = false;

  public UICategoryForm() throws Exception {
    isDoubleClickSubmit = false;
    UIFormInputWithActions detailTab = new UIFormInputWithActions(CATEGORY_DETAIL_TAB);

    UIFormStringInput categoryTitle = new UIFormStringInput(FIELD_CATEGORYTITLE_INPUT, FIELD_CATEGORYTITLE_INPUT, null);
    categoryTitle.addValidator(MandatoryValidator.class);
    UIFormStringInput categoryOrder = new UIFormStringInput(FIELD_CATEGORYORDER_INPUT, FIELD_CATEGORYORDER_INPUT, "0");
    categoryOrder.addValidator(PositiveNumberFormatValidator.class);
    UIFormTextAreaInput description = new UIFormTextAreaInput(FIELD_DESCRIPTION_INPUT, FIELD_DESCRIPTION_INPUT, null);

    UIFormTextAreaInput userPrivate = new UIFormTextAreaInput(FIELD_USERPRIVATE_MULTIVALUE, FIELD_USERPRIVATE_MULTIVALUE, null);

    detailTab.addUIFormInput(categoryTitle);
    detailTab.addUIFormInput(categoryOrder);
    detailTab.addUIFormInput(userPrivate);
    detailTab.addUIFormInput(description);

    String[] strings = new String[] { "SelectUser", "SelectMemberShip", "SelectGroup" };
    String[] icons = ForumUtils.getClassIconWithAction();
    List<ActionData> actions = new ArrayList<ActionData>();

    ActionData ad;
    int i = 0;
    for (String string : strings) {
      ad = new ActionData();
      if (i == 0)
        ad.setActionListener("AddValuesUser");
      else
        ad.setActionListener("AddPrivate");
      ad.setActionParameter(String.valueOf(i) + ForumUtils.COMMA + FIELD_USERPRIVATE_MULTIVALUE);
      ad.setCssIconClass(icons[i]);
      ad.setActionName(string);
      actions.add(ad);
      ++i;
    }
    detailTab.setActionField(FIELD_USERPRIVATE_MULTIVALUE, actions);
    addUIFormInput(detailTab);
    
    UIPermissionPanel permissionPanel = createUIComponent(UIPermissionPanel.class, null, PERMISSION_TAB);
    permissionPanel.setPermission(null, new String[] { MODERAROR, TOPICABLE, POSTABLE, VIEWER });
    addChild(permissionPanel);
    
    setActions(new String[] { "Save", "Cancel" });
    setAddColonInLabel(true);
  }
  
  public void setSpaceGroupId(String spaceGroupId) {
    getChild(UIPermissionPanel.class).setSpaceGroupId(spaceGroupId);
  }

  protected boolean getIsSelected(int id) {
    if (this.id == id)
      return true;
    return false;
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void setCategoryValue(Category category, boolean isUpdate) throws Exception {
    if (isUpdate) {
      this.categoryId = category.getId();
      getUIStringInput(FIELD_CATEGORYTITLE_INPUT).setValue(StringCommonUtils.decodeSpecialCharToHTMLnumber(category.getCategoryName()));
      getUIStringInput(FIELD_CATEGORYORDER_INPUT).setValue(Long.toString(category.getCategoryOrder()));
      getUIFormTextAreaInput(FIELD_DESCRIPTION_INPUT).setDefaultValue(StringCommonUtils.decodeSpecialCharToHTMLnumber(category.getDescription()));
      String userPrivate = ForumUtils.unSplitForForum(category.getUserPrivate());
      getUIFormTextAreaInput(FIELD_USERPRIVATE_MULTIVALUE).setValue(userPrivate);
      
      UIPermissionPanel permissionTab = getChildById(PERMISSION_TAB);
      permissionTab.addPermissionForOwners(MODERAROR, category.getModerators());
      permissionTab.addPermissionForOwners(TOPICABLE, category.getCreateTopicRole());
      permissionTab.addPermissionForOwners(POSTABLE, category.getPoster());
      permissionTab.addPermissionForOwners(VIEWER, category.getViewer());
    }
  }

  public void updateSelect(String selectField, String value) throws Exception {
    UIFormTextAreaInput fieldInput = getUIFormTextAreaInput(selectField);
    String values = fieldInput.getValue();
    fieldInput.setValue(ForumUtils.updateMultiValues(value, values));
  }

  static public class SaveActionListener extends BaseEventListener<UICategoryForm> {
    public void onEvent(Event<UICategoryForm> event, UICategoryForm uiForm, String objectId) throws Exception {
      if (uiForm.isDoubleClickSubmit)
        return;
      String categoryTitle = uiForm.getUIStringInput(FIELD_CATEGORYTITLE_INPUT).getValue();
      int maxText = ForumUtils.MAXTITLE;
      if (categoryTitle.length() > maxText) {
        warning("NameValidator.msg.warning-long-text", new String[] { uiForm.getLabel(FIELD_CATEGORYTITLE_INPUT), String.valueOf(maxText) });
        return;
      }
      categoryTitle = StringCommonUtils.encodeSpecialCharForSimpleInput(categoryTitle);
      String description = uiForm.getUIFormTextAreaInput(FIELD_DESCRIPTION_INPUT).getValue();
      if (!ForumUtils.isEmpty(description) && description.length() > maxText) {
        warning("NameValidator.msg.warning-long-text", new String[] { uiForm.getLabel(FIELD_DESCRIPTION_INPUT), String.valueOf(maxText) });
        return;
      }
      description = StringCommonUtils.encodeSpecialCharForSimpleInput(description);
      String categoryOrder = uiForm.getUIStringInput(FIELD_CATEGORYORDER_INPUT).getValue();
      if (ForumUtils.isEmpty(categoryOrder))
        categoryOrder = "0";
      categoryOrder = ForumUtils.removeZeroFirstNumber(categoryOrder);
      if (categoryOrder.length() > 3) {
        warning("NameValidator.msg.erro-large-number", new String[] { uiForm.getLabel(FIELD_CATEGORYORDER_INPUT) });
        return;
      }
      
      UIPermissionPanel permissionTab = uiForm.getChildById(PERMISSION_TAB);
      String moderator = permissionTab.getOwnersByPermission(MODERAROR);
      moderator = ForumUtils.removeSpaceInString(moderator);
      moderator = ForumUtils.removeStringResemble(moderator);
      String[] moderators = ForumUtils.splitForForum(moderator);
      if (!ForumUtils.isEmpty(moderator)) {
        String erroUser = UserHelper.checkValueUser(moderator);
        if (!ForumUtils.isEmpty(erroUser)) {
          warning("NameValidator.msg.erroUser-input", new String[] { uiForm.getLabel(MODERAROR), erroUser });
          return;
        }
      } else {
        moderators = new String[] { "" };
      }

      String userPrivate = uiForm.getUIFormTextAreaInput(FIELD_USERPRIVATE_MULTIVALUE).getValue();
      if (!ForumUtils.isEmpty(userPrivate) && !ForumUtils.isEmpty(moderator)) {
        userPrivate = userPrivate + ForumUtils.COMMA + moderator;
      }
      userPrivate = ForumUtils.removeSpaceInString(userPrivate);
      userPrivate = ForumUtils.removeStringResemble(userPrivate);
      String[] userPrivates = ForumUtils.splitForForum(userPrivate);
      if (!ForumUtils.isEmpty(userPrivate)) {
        String erroUser = UserHelper.checkValueUser(userPrivate);
        if (!ForumUtils.isEmpty(erroUser)) {
          warning("NameValidator.msg.erroUser-input", new String[] { uiForm.getLabel(FIELD_USERPRIVATE_MULTIVALUE), erroUser });
          return;
        }
      } else {
        userPrivates = new String[] { "" };
      }

      String topicable = permissionTab.getOwnersByPermission(TOPICABLE);
      String postable = permissionTab.getOwnersByPermission(POSTABLE);
      String viewer = permissionTab.getOwnersByPermission(VIEWER);

      topicable = ForumUtils.removeSpaceInString(topicable);
      postable = ForumUtils.removeSpaceInString(postable);
      viewer = ForumUtils.removeSpaceInString(viewer);

      String erroUser = UserHelper.checkValueUser(topicable);
      erroUser = UserHelper.checkValueUser(topicable);
      if (!ForumUtils.isEmpty(erroUser)) {
        warning("NameValidator.msg.erroUser-input", new String[] { uiForm.getLabel(TOPICABLE), erroUser });
        return;
      }
      erroUser = UserHelper.checkValueUser(postable);
      if (!ForumUtils.isEmpty(erroUser)) {
        warning("NameValidator.msg.erroUser-input", new String[] { uiForm.getLabel(POSTABLE), erroUser });
        return;
      }
      erroUser = UserHelper.checkValueUser(viewer);
      if (!ForumUtils.isEmpty(erroUser)) {
        warning("NameValidator.msg.erroUser-input", new String[] { uiForm.getLabel(VIEWER), erroUser });
        return;
      }

      String[] setTopicable = ForumUtils.splitForForum(topicable);
      String[] setPostable = ForumUtils.splitForForum(postable);
      String[] setViewer = ForumUtils.splitForForum(viewer);

      String userName = uiForm.getUserProfile().getUserId();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      boolean isNew = true;
      Category cat = new Category();
      if (!ForumUtils.isEmpty(uiForm.categoryId)) {
        cat = uiForm.getForumService().getCategory(uiForm.categoryId);
        if(cat == null) {
          warning("UIForumPortlet.msg.catagory-deleted", false);
          forumPortlet.cancelAction();
          forumPortlet.renderForumHome();
          event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
          return;
        }
        isNew = false;
      }
      cat.setOwner(userName);
      cat.setCategoryName(categoryTitle.trim());
      cat.setCategoryOrder(Long.parseLong(categoryOrder));
      cat.setCreatedDate(new Date());
      cat.setDescription(description);
      cat.setModifiedBy(userName);
      cat.setModifiedDate(new Date());
      cat.setUserPrivate(userPrivates);
      cat.setModerators(moderators);
      cat.setCreateTopicRole(setTopicable);
      cat.setPoster(setPostable);
      cat.setViewer(setViewer);

      UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
      try {
        uiForm.getForumService().saveCategory(cat, isNew);
        List<String> invisibleCategories = forumPortlet.getInvisibleCategories();
        if (!invisibleCategories.isEmpty()) {
          List<String> invisibleForums = forumPortlet.getInvisibleForums();
          invisibleCategories.add(cat.getId());
          String listForumId = UICategoryForm.listToString(invisibleForums);
          String listCategoryId = UICategoryForm.listToString(invisibleCategories);
          ForumUtils.savePortletPreference(listCategoryId, listForumId);
          forumPortlet.loadPreferences();
        }
        UICategory uiCategory = categoryContainer.getChild(UICategory.class);
        uiCategory.setIsEditForum(true);
        uiCategory.updateByBreadcumbs(cat.getId());
        categoryContainer.updateIsRender(false);
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        forumPortlet.findFirstComponentOfType(UIBreadcumbs.class).setUpdataPath(cat.getId());
      } catch (Exception e) {
        warning("UIForumPortlet.msg.catagory-deleted", false);
        forumPortlet.renderForumHome();
      }
      forumPortlet.cancelAction();
      uiForm.isDoubleClickSubmit = true;
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public String listToString(List<String> list) {
    if (list == null) return ForumUtils.EMPTY_STR;
    String s = list.toString().substring(1);
    return s.substring(0, s.length() - 1).replaceAll(Utils.SPACE, ForumUtils.EMPTY_STR);
  }
  
  static public class SelectTabActionListener extends BaseEventListener<UICategoryForm> {
    public void onEvent(Event<UICategoryForm> event, UICategoryForm uiForm, String id) throws Exception {
      uiForm.id = Integer.parseInt(id);
      UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setWindowSize(665, 380);
      Util.getPortalRequestContext().setResponseComplete(true);
    }
  }

  static public class AddPrivateActionListener extends BaseEventListener<UICategoryForm> {
    public void onEvent(Event<UICategoryForm> event, UICategoryForm categoryForm, String objectId) throws Exception {
      String[] objects = objectId.split(ForumUtils.COMMA);
      String type = objects[0];
      String param = objects[1];
      UIPopupContainer popupContainer = categoryForm.getAncestorOfType(UIPopupContainer.class);
      UIUserSelect uiUserSelect = popupContainer.findFirstComponentOfType(UIUserSelect.class);
      if (uiUserSelect != null) {
        UIPopupWindow popupWindow = uiUserSelect.getParent();
        closePopupWindow(popupWindow);
      }
      UIGroupSelector uiGroupSelector = null;
      if (type.equals(UIGroupSelector.TYPE_MEMBERSHIP)) {
        uiGroupSelector = openPopup(popupContainer, UIGroupSelector.class, "UIMemberShipSelector", 600, 0);
      } else if (type.equals(UIGroupSelector.TYPE_GROUP)) {
        uiGroupSelector = openPopup(popupContainer, UIGroupSelector.class, "GroupSelector", 600, 0);
      }
      uiGroupSelector.getAncestorOfType(UIPopupWindow.class).setRendered(true);
      uiGroupSelector.setType(type);
      uiGroupSelector.setSpaceGroupId(categoryForm.getAncestorOfType(UIForumPortlet.class).getSpaceGroupId());
      uiGroupSelector.setComponent(categoryForm, new String[] { param });
      uiGroupSelector.getChild(UITree.class).setId(UIGroupSelector.TREE_GROUP_ID);
      uiGroupSelector.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(UIGroupSelector.BREADCUMB_GROUP_ID);
    }
  }

  static public class CancelActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      event.getSource().getAncestorOfType(UIForumPortlet.class).cancelAction();
    }
  }

  static public class CloseActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiUserSelector = event.getSource();
      UIPopupWindow popupWindow = uiUserSelector.getParent();
      closePopupWindow(popupWindow);
    }
  }

  static public class ClosePopupActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIPopupWindow popupWindow = event.getSource();
      closePopupWindow(popupWindow);
    }
  }

  private void setValueField(UIFormInputWithActions withActions, String field, String values) throws Exception {
    try {
      UIFormTextAreaInput textArea = withActions.getUIFormTextAreaInput(field);
      String vls = textArea.getValue();
      if (!ForumUtils.isEmpty(vls)) {
        values = values + ForumUtils.COMMA + vls;
        values = ForumUtils.removeStringResemble(values.replaceAll(",,", ForumUtils.COMMA));
      }
      textArea.setValue(values);
    } catch (Exception e) {
      log.debug("Set Value into field " + field + " is fall.", e);
    }
  }

  static public class AddActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiUserSelector = event.getSource();
      String values = uiUserSelector.getSelectedUsers();
      UIForumPortlet forumPortlet = uiUserSelector.getAncestorOfType(UIForumPortlet.class);
      UICategoryForm categoryForm = forumPortlet.findFirstComponentOfType(UICategoryForm.class);
      UIPopupWindow popupWindow = uiUserSelector.getParent();
      String id = uiUserSelector.getPermisionType();
      if (id.equals(FIELD_USERPRIVATE_MULTIVALUE)) {
        UIFormInputWithActions catDetail = categoryForm.getChildById(CATEGORY_DETAIL_TAB);
        categoryForm.setValueField(catDetail, FIELD_USERPRIVATE_MULTIVALUE, values);
      } 
      closePopupWindow(popupWindow);
      event.getRequestContext().addUIComponentToUpdateByAjax(categoryForm);
    }
  }

  static public class AddValuesUserActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm categoryForm = event.getSource();
      String id = event.getRequestContext().getRequestParameter(OBJECTID).replace("0,", ForumUtils.EMPTY_STR);
      UIPopupContainer uiPopupContainer = categoryForm.getAncestorOfType(UIPopupContainer.class);
      categoryForm.showUIUserSelect(uiPopupContainer, USER_SELECTOR_POPUPWINDOW, id);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
}
