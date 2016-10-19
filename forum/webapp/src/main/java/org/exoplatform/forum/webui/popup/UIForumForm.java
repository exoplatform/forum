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

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.UIPermissionPanel;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIBreadcumbs;
import org.exoplatform.forum.webui.UICategories;
import org.exoplatform.forum.webui.UICategory;
import org.exoplatform.forum.webui.UIForumContainer;
import org.exoplatform.forum.webui.UIForumDescription;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicContainer;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.PositiveNumberFormatValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, 
      template = "app:/templates/forum/webui/popup/UIForumForm.gtmpl", 
      events = {
        @EventConfig(listeners = UIForumForm.SaveActionListener.class), 
        @EventConfig(listeners = UIForumForm.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIForumForm.SelectTabActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIForumForm.OnChangeAutoEmailActionListener.class, phase = Phase.DECODE) 
    })
})

public class UIForumForm extends BaseForumForm implements UIPopupComponent {
  private boolean            isCategoriesUpdate                  = true;

  private boolean            isForumUpdate                       = false;

  private boolean            isActionBar                         = false;

  private boolean            isMode                              = false;

  private boolean            isUpdate                            = false;

  private String             forumId                             = ForumUtils.EMPTY_STR;

  private String             categoryId                          = ForumUtils.EMPTY_STR;

  private int                id                                  = 0;

  private boolean            isDoubleClickSubmit;

  public static final String FIELD_NEWFORUM_FORM                 = "newForum";

  public static final String FIELD_MODERATOROPTION_FORM          = "moderationOptions";

  public static final String FIELD_CATEGORY_SELECTBOX            = "Category";

  public static final String FIELD_FORUMTITLE_INPUT              = "ForumTitle";

  public static final String FIELD_FORUMORDER_INPUT              = "ForumOrder";

  public static final String FIELD_FORUMSTATUS_SELECTBOX         = "ForumStatus";

  public static final String FIELD_FORUMSTATE_SELECTBOX          = "ForumState";

  public static final String FIELD_DESCRIPTION_TEXTAREA          = "Description";

  public static final String FIELD_AUTOADDEMAILNOTIFY_CHECKBOX   = "AutoAddEmailNotify";

  public static final String FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE = "NotifyWhenAddTopic";

  public static final String FIELD_NOTIFYWHENADDPOST_MULTIVALUE  = "NotifyWhenAddPost";

  public static final String FIELD_MODERATETHREAD_CHECKBOX       = "ModerateThread";

  public static final String FIELD_MODERATEPOST_CHECKBOX         = "ModeratePost";

  public static final String PERMISSION_TAB                      = "forumPermission";

  public static final String MODERATOR                           = "Moderator";

  public static final String VIEWER                              = "Viewer";

  public static final String POSTABLE                            = "Postable";

  public static final String TOPICABLE                           = "Topicable";

  public static final String USER_SELECTOR_POPUPWINDOW           = "UIForumUserPopupWindow";
  
  private Forum forum = null;

  public UIForumForm() throws Exception {
    isDoubleClickSubmit = false;
    setAddColonInLabel(true);
  }

  public boolean isMode() {
    return isMode;
  }

  public void setMode(boolean isMode) {
    this.isMode = isMode;
  }

  public void initForm(String spaceGroupId) throws Exception {
    forum = new Forum();
    List<SelectItemOption<String>> list = new ArrayList<SelectItemOption<String>>();
    if (ForumUtils.isEmpty(categoryId)) {
      List<Category> categorys = getForumService().getCategories(false);
      for (Category category : categorys) {
        list.add(new SelectItemOption<String>(category.getCategoryName(), category.getId()));
      }
      if (list.size() > 0) {
        categoryId = list.get(0).getValue();
      }
    } else {
      Category category = getForumService().getCategory(categoryId);
      list.add(new SelectItemOption<String>(category.getCategoryName(), categoryId));
    }

    UIFormSelectBox selictCategoryId = new UIFormSelectBox(FIELD_CATEGORY_SELECTBOX, FIELD_CATEGORY_SELECTBOX, list);
    selictCategoryId.setDefaultValue(categoryId);

    UIFormStringInput forumTitle = new UIFormStringInput(FIELD_FORUMTITLE_INPUT, FIELD_FORUMTITLE_INPUT, null);
    forumTitle.addValidator(MandatoryValidator.class);
    UIFormStringInput forumOrder = new UIFormStringInput(FIELD_FORUMORDER_INPUT, FIELD_FORUMORDER_INPUT, "0");
    forumOrder.addValidator(PositiveNumberFormatValidator.class);
    List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>();
    ls.add(new SelectItemOption<String>(getLabel("Open"), "open"));
    ls.add(new SelectItemOption<String>(getLabel("Closed"), "closed"));
    UIFormSelectBox forumState = new UIFormSelectBox(FIELD_FORUMSTATE_SELECTBOX, FIELD_FORUMSTATE_SELECTBOX, ls);
    forumState.setDefaultValue("open");
    ls = new ArrayList<SelectItemOption<String>>();
    ls.add(new SelectItemOption<String>(this.getLabel("UnLock"), "unlock"));
    ls.add(new SelectItemOption<String>(this.getLabel("Locked"), "locked"));
    UIFormSelectBox forumStatus = new UIFormSelectBox(FIELD_FORUMSTATUS_SELECTBOX, FIELD_FORUMSTATUS_SELECTBOX, ls);
    forumStatus.setDefaultValue("unlock");
    UIFormTextAreaInput description = new UIFormTextAreaInput(FIELD_DESCRIPTION_TEXTAREA, FIELD_DESCRIPTION_TEXTAREA, null);

    UICheckBoxInput checkWhenAddTopic = new UICheckBoxInput(FIELD_MODERATETHREAD_CHECKBOX, FIELD_MODERATETHREAD_CHECKBOX, false);

    UIFormTextAreaInput notifyWhenAddPost = new UIFormTextAreaInput(FIELD_NOTIFYWHENADDPOST_MULTIVALUE, FIELD_NOTIFYWHENADDPOST_MULTIVALUE, null);

    UIFormTextAreaInput notifyWhenAddTopic = new UIFormTextAreaInput(FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE, FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE, null);


    UICheckBoxInput autoAddEmailNotify = new UICheckBoxInput(FIELD_AUTOADDEMAILNOTIFY_CHECKBOX, FIELD_AUTOADDEMAILNOTIFY_CHECKBOX, true);
    autoAddEmailNotify.setValue(true);
    autoAddEmailNotify.setOnChange("OnChangeAutoEmail");
    //addUIFormInput(selictCategoryId);
    UIFormInputWithActions newForum = new UIFormInputWithActions(FIELD_NEWFORUM_FORM);
    newForum.addUIFormInput(forumTitle);
    newForum.addUIFormInput(selictCategoryId);
    newForum.addUIFormInput(forumOrder);
    newForum.addUIFormInput(forumState);
    newForum.addUIFormInput(forumStatus);
    newForum.addUIFormInput(description);

    notifyWhenAddPost.setReadOnly(autoAddEmailNotify.getValue());
    notifyWhenAddTopic.setReadOnly(autoAddEmailNotify.getValue());
    
    UIFormInputWithActions moderationOptions = new UIFormInputWithActions(FIELD_MODERATOROPTION_FORM);
    moderationOptions.addUIFormInput(autoAddEmailNotify);
    moderationOptions.addUIFormInput(notifyWhenAddPost);
    moderationOptions.addUIFormInput(notifyWhenAddTopic);
    moderationOptions.addUIFormInput(checkWhenAddTopic);

    addUIFormInput(newForum);
    addUIFormInput(moderationOptions);

    UIPermissionPanel permissionTab = createUIComponent(UIPermissionPanel.class, null, PERMISSION_TAB);
    String []permssion = (isMode == true) ? new String[] { VIEWER, TOPICABLE, POSTABLE } : 
                                            new String[] { MODERATOR, VIEWER, TOPICABLE, POSTABLE }; 
    permissionTab.setPermission(spaceGroupId, permssion);
    addChild(permissionTab);

    setActions(new String[] { "Save", "Cancel" });
  }

  public void activate() {
  }

  public void deActivate() {
  }

  protected boolean getIsSelected(int id) {
    if (this.id == id)
      return true;
    return false;
  }

  public void setForumValue(Forum forum_, boolean isUpdate) throws Exception {
    this.isUpdate = isUpdate;
    if (isUpdate) {
      forumId = forum_.getId();
      forum = getForumService().getForum(categoryId, forumId);
      UIFormInputWithActions newForum = this.getChildById(FIELD_NEWFORUM_FORM);
      newForum.getUIStringInput(FIELD_FORUMTITLE_INPUT).setValue(CommonUtils.decodeSpecialCharToHTMLnumber(forum.getForumName()));
      newForum.getUIStringInput(FIELD_FORUMORDER_INPUT).setValue(String.valueOf(forum.getForumOrder()));
      String stat = "open";
      if (forum.getIsClosed())
        stat = "closed";
      newForum.getUIFormSelectBox(FIELD_FORUMSTATE_SELECTBOX).setValue(stat);
      if (forum.getIsLock())
        stat = "locked";
      else
        stat = "unlock";
      newForum.getUIFormSelectBox(FIELD_FORUMSTATUS_SELECTBOX).setValue(stat);
      newForum.getUIFormTextAreaInput(FIELD_DESCRIPTION_TEXTAREA).setDefaultValue(CommonUtils.decodeSpecialCharToHTMLnumber(forum.getDescription()));

      UIFormInputWithActions moderationOptions = this.getChildById(FIELD_MODERATOROPTION_FORM);
      boolean isAutoAddEmail = forum.getIsAutoAddEmailNotify();
      UICheckBoxInput boxInput = getUICheckBoxInput(FIELD_AUTOADDEMAILNOTIFY_CHECKBOX);
      boxInput.setChecked(isAutoAddEmail);
      boxInput.setReadOnly(isMode);

      UIFormTextAreaInput notifyWhenAddPost = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDPOST_MULTIVALUE);
      UIFormTextAreaInput notifyWhenAddTopic = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE);
      notifyWhenAddPost.setValue(ForumUtils.unSplitForForum(forum.getNotifyWhenAddPost()));
      notifyWhenAddTopic.setValue(ForumUtils.unSplitForForum(forum.getNotifyWhenAddTopic()));
      notifyWhenAddPost.setReadOnly(isAutoAddEmail);
      notifyWhenAddTopic.setReadOnly(isAutoAddEmail);
      getUICheckBoxInput(FIELD_MODERATETHREAD_CHECKBOX).setChecked(forum.getIsModerateTopic());

      UIPermissionPanel permisisonTab = this.getChildById(PERMISSION_TAB);
      if(isMode == false) {
        permisisonTab.addPermissionForOwners(MODERATOR, forum.getModerators());
      }
      permisisonTab.addPermissionForOwners(VIEWER, forum.getViewer());
      permisisonTab.addPermissionForOwners(TOPICABLE, forum.getCreateTopicRole());
      permisisonTab.addPermissionForOwners(POSTABLE, forum.getPoster());
    }
  }

  public void setCategoryValue(String categoryId, boolean isEditable) throws Exception {
    UIFormInputWithActions newForum = this.getChildById(FIELD_NEWFORUM_FORM);
    if (!ForumUtils.isEmpty(categoryId)) {
      newForum.getUIFormSelectBox(FIELD_CATEGORY_SELECTBOX).setValue(categoryId);
    }
    newForum.getUIFormSelectBox(FIELD_CATEGORY_SELECTBOX).setDisabled(!isEditable);
    isCategoriesUpdate = isEditable;
    this.categoryId = categoryId;
    isUpdate = false;
  }

  public void setForumUpdate(boolean isForumUpdate) {
    this.isForumUpdate = isForumUpdate;
  }

  public boolean isActionBar() {
    return isActionBar;
  }

  public void setActionBar(boolean isActionBar) {
    this.isActionBar = isActionBar;
  }

  private static String listToString(Collection<String> list) {
    return list.toString().replace("[", ForumUtils.EMPTY_STR).replace("]", ForumUtils.EMPTY_STR);
  }

  static public class SaveActionListener extends EventListener<UIForumForm> {
    public void execute(Event<UIForumForm> event) throws Exception {
      UIForumForm uiForm = event.getSource();
      if (uiForm.isDoubleClickSubmit)
        return;
      uiForm.isDoubleClickSubmit = true;
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);

      UIFormInputWithActions newForumForm = uiForm.getChildById(FIELD_NEWFORUM_FORM);
      
      UIFormSelectBox categorySelectBox = newForumForm.getUIFormSelectBox(FIELD_CATEGORY_SELECTBOX);
      String categoryId = categorySelectBox.getValue();
      Category category = uiForm.getForumService().getCategory(categoryId);
      if (category == null) {
        uiForm.warning("UICategory.msg.CategoryNotExist");
        forumPortlet.renderForumHome();
        forumPortlet.cancelAction();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        return;
      }
      String forumTitle = newForumForm.getUIStringInput(FIELD_FORUMTITLE_INPUT).getValue();
      forumTitle = forumTitle.trim();
      int maxText = 50;// ForumUtils.MAXTITLE ;
      if (forumTitle.length() > maxText) {
        uiForm.warning("NameValidator.msg.warning-long-text", new String[] { uiForm.getLabel(FIELD_FORUMTITLE_INPUT), String.valueOf(maxText) });
        uiForm.isDoubleClickSubmit = false;
        return;
      }
      forumTitle = CommonUtils.encodeSpecialCharInTitle(forumTitle);
      String forumOrder = newForumForm.getUIStringInput(FIELD_FORUMORDER_INPUT).getValue();
      if (ForumUtils.isEmpty(forumOrder))
        forumOrder = "0";
      forumOrder = ForumUtils.removeZeroFirstNumber(forumOrder);
      if (forumOrder.length() > 3) {
        uiForm.warning("NameValidator.msg.erro-large-number", new String[] { uiForm.getLabel(FIELD_FORUMORDER_INPUT) });
        uiForm.isDoubleClickSubmit = false;
        return;
      }
      String forumState = newForumForm.getUIFormSelectBox(FIELD_FORUMSTATE_SELECTBOX).getValue();
      String forumStatus = newForumForm.getUIFormSelectBox(FIELD_FORUMSTATUS_SELECTBOX).getValue();
      String description = newForumForm.getUIFormTextAreaInput(FIELD_DESCRIPTION_TEXTAREA).getValue();
      description = CommonUtils.encodeSpecialCharInTitle(description);
      
      UIPermissionPanel permissionTab = uiForm.getChildById(PERMISSION_TAB);
      String moderators = permissionTab.getOwnersByPermission(MODERATOR);
      String topicable = permissionTab.getOwnersByPermission(TOPICABLE);
      String postable = permissionTab.getOwnersByPermission(POSTABLE);
      String viewer = permissionTab.getOwnersByPermission(VIEWER);

      UIFormInputWithActions moderationOptions = uiForm.getChildById(FIELD_MODERATOROPTION_FORM);
      boolean isAutoAddEmail = uiForm.getUICheckBoxInput(FIELD_AUTOADDEMAILNOTIFY_CHECKBOX).isChecked();
      // set email
      if(uiForm.isMode == false) {
        if (isAutoAddEmail) {
          uiForm.setDefaultEmail(moderationOptions, moderators);
        }
      }
      String notifyWhenAddTopics = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE).getValue();
      String notifyWhenAddPosts = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDPOST_MULTIVALUE).getValue();

      if (!ForumUtils.isValidEmailAddresses(notifyWhenAddPosts) || !ForumUtils.isValidEmailAddresses(notifyWhenAddTopics)) {
        uiForm.warning("MessagePost.msg.invalid-email");
        uiForm.isDoubleClickSubmit = false;
        return;
      }
      String[] notifyWhenAddTopic = ForumUtils.splitForForum(notifyWhenAddTopics);
      String[] notifyWhenAddPost = ForumUtils.splitForForum(notifyWhenAddPosts);
      boolean ModerateTopic = uiForm.getUICheckBoxInput(FIELD_MODERATETHREAD_CHECKBOX).getValue();

      String userName = UserHelper.getCurrentUser();
      Forum newForum = uiForm.forum;
      newForum.setForumName(forumTitle);
      newForum.setOwner(userName);
      newForum.setForumOrder(Integer.valueOf(forumOrder).intValue());
      newForum.setCreatedDate(new Date());
      newForum.setDescription(description);
      newForum.setLastTopicPath(ForumUtils.EMPTY_STR);
      newForum.setPath(ForumUtils.EMPTY_STR);
      newForum.setModifiedBy(userName);
      newForum.setModifiedDate(new Date());
      newForum.setPostCount(0);
      newForum.setTopicCount(0);
      newForum.setIsAutoAddEmailNotify(isAutoAddEmail);
      newForum.setNotifyWhenAddPost(notifyWhenAddPost);
      newForum.setNotifyWhenAddTopic(notifyWhenAddTopic);
      newForum.setIsModeratePost(false);
      newForum.setIsModerateTopic(ModerateTopic);
      if (forumState.equals("closed")) {
        newForum.setIsClosed(true);
      } else {
        newForum.setIsClosed(false);
      }
      if (forumStatus.equals("locked")) {
        newForum.setIsLock(true);
      } else {
        newForum.setIsLock(false);
      }
      
      String[] setTopicable = ForumUtils.splitForForum(topicable);
      String[] setPostable = ForumUtils.splitForForum(postable);
      String[] setViewer = ForumUtils.splitForForum(viewer);

      if(uiForm.isMode == false) {
        String[] setModerators = ForumUtils.splitForForum(moderators);
        newForum.setModerators(setModerators);
      }

      newForum.setCreateTopicRole(setTopicable);
      newForum.setPoster(setPostable);
      newForum.setViewer(setViewer);

      try {
        if (!ForumUtils.isEmpty(uiForm.forumId)) {
          newForum.setId(uiForm.forumId);
          uiForm.getForumService().saveForum(categoryId, newForum, false);
        } else {
          uiForm.getForumService().saveForum(categoryId, newForum, true);
          List<String> invisibleCategories = forumPortlet.getInvisibleCategories();
          List<String> invisibleForums = forumPortlet.getInvisibleForums();
          String listForumId = ForumUtils.EMPTY_STR, listCategoryId = ForumUtils.EMPTY_STR;
          if (!invisibleCategories.isEmpty()) {
            if (invisibleCategories.contains(categoryId)) {
              invisibleForums.add(newForum.getId());
              listForumId = listToString(invisibleForums).replaceAll(" ", ForumUtils.EMPTY_STR);
              listCategoryId = listToString(invisibleCategories).replaceAll(" ", ForumUtils.EMPTY_STR);
              ForumUtils.savePortletPreference(listCategoryId, listForumId);
            }
          }
        }
      } catch (Exception e) {
        uiForm.log.error("Save portlet preference is fall, exception: ", e);
      }

      forumPortlet.cancelAction();
      WebuiRequestContext context = event.getRequestContext();

      if (uiForm.isUpdate && !uiForm.isForumUpdate) {
        if (uiForm.isCategoriesUpdate && uiForm.isActionBar == false) {
          UICategories uiCategories = forumPortlet.findFirstComponentOfType(UICategories.class);
           context.addUIComponentToUpdateByAjax(uiCategories);
        } else {
          UICategory uiCategory = forumPortlet.findFirstComponentOfType(UICategory.class);
          uiCategory.setIsEditForum(true);
          if (!uiForm.isActionBar)
            context.addUIComponentToUpdateByAjax(uiCategory);
        }
        if (uiForm.isActionBar) {
          forumPortlet.findFirstComponentOfType(UICategory.class).setIsEditForum(true);
          context.addUIComponentToUpdateByAjax(forumPortlet);
        }
      } else {
        UITopicContainer uiTopicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class);
        UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
        if (!uiForm.isForumUpdate) {
          forumPortlet.updateIsRendered(ForumUtils.FORUM);
          uiForumContainer.setIsRenderChild(true);
          uiTopicContainer.updateByBreadcumbs(categoryId, newForum.getId(), true, 1);
        }
        UIForumDescription forumDescription = uiForumContainer.getChild(UIForumDescription.class);
        forumDescription.setForum(newForum);
        UIBreadcumbs breadcumbs = forumPortlet.getChild(UIBreadcumbs.class);
        breadcumbs.setUpdataPath(categoryId + ForumUtils.SLASH + newForum.getId());
        context.addUIComponentToUpdateByAjax(forumPortlet);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIForumForm> {
    public void execute(Event<UIForumForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }

  static public class OnChangeAutoEmailActionListener extends EventListener<UIForumForm> {
    public void execute(Event<UIForumForm> event) throws Exception {
      UIForumForm forumForm = event.getSource();
      UIFormInputWithActions moderationOptions = forumForm.getChildById(FIELD_MODERATOROPTION_FORM);
      UIFormTextAreaInput notifyWhenAddTopics = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE);
      UIFormTextAreaInput notifyWhenAddPosts = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDPOST_MULTIVALUE);
      boolean isCheck = forumForm.getUICheckBoxInput(FIELD_AUTOADDEMAILNOTIFY_CHECKBOX).isChecked();
      if (isCheck) {
        UIPermissionPanel permissionTab = forumForm.getChildById(PERMISSION_TAB);
        String moderators = permissionTab.getOwnersByPermission(MODERATOR);
        forumForm.setDefaultEmail(moderationOptions, moderators);
        notifyWhenAddTopics.setReadOnly(true);
        notifyWhenAddPosts.setReadOnly(true);
      } else {
        notifyWhenAddTopics.setReadOnly(false);
        notifyWhenAddPosts.setReadOnly(false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(moderationOptions);
    }
  }

  private void setDefaultEmail(UIFormInputWithActions moderationOptions, String moderators) throws Exception {
    UIFormTextAreaInput notifyWhenAddTopics = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDTOPIC_MULTIVALUE);
    UIFormTextAreaInput notifyWhenAddPosts = moderationOptions.getUIFormTextAreaInput(FIELD_NOTIFYWHENADDPOST_MULTIVALUE);
    UICheckBoxInput boxInput = getUICheckBoxInput(FIELD_AUTOADDEMAILNOTIFY_CHECKBOX);
    if (! boxInput.isChecked()) {
      return;
    }
    if (isMode()) {
      moderators = ForumUtils.unSplitForForum(forum.getModerators());
    }
    Set<String> listModerator = new HashSet<String>();
    if (!ForumUtils.isEmpty(moderators)) {
      String[] moderators_ = ForumUtils.splitForForum(moderators);
      String email;
      User user = null;
      List<String> list = ForumServiceUtils.getUserPermission(moderators_);
      for (String string : list) {
        user = UserHelper.getUserByUserId(string);
        if (user != null && user.isEnabled()) {
          email = user.getEmail();
          listModerator.add(email);
        }
      }
    }
    notifyWhenAddTopics.setValue(listToString(listModerator));
    notifyWhenAddPosts.setValue(listToString(listModerator));
  }

  static public class SelectTabActionListener extends EventListener<UIForumForm> {
    public void execute(Event<UIForumForm> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIForumForm forumForm = event.getSource();
      forumForm.id = Integer.parseInt(id);
      if (forumForm.id == 1 && !forumForm.isMode) {
        UIFormInputWithActions moderationOptions = forumForm.getChildById(FIELD_MODERATOROPTION_FORM);
        UIPermissionPanel permissionTab = forumForm.getChildById(PERMISSION_TAB);
        String moderators = permissionTab.getOwnersByPermission(MODERATOR);
        forumForm.setDefaultEmail(moderationOptions, moderators);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(forumForm);
    }
  }
}
