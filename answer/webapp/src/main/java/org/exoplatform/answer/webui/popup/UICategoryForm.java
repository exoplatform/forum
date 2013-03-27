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
package org.exoplatform.answer.webui.popup;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIBreadcumbs;
import org.exoplatform.answer.webui.UICategories;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.faq.service.Category;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPermissionPanel;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.PositiveNumberFormatValidator;
/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com 
 * Aus 01, 2007 2:48:18 PM
 */

@ComponentConfigs( 
    {
        @ComponentConfig(
          lifecycle = UIFormLifecycle.class, 
          template = "app:/templates/answer/webui/popup/UICategoryForm.gtmpl", 
          events = {
              @EventConfig(listeners = UICategoryForm.SaveActionListener.class), 
              @EventConfig(listeners = UICategoryForm.CancelActionListener.class, phase = Phase.DECODE), 
              @EventConfig(listeners = UICategoryForm.SelectTabActionListener.class, phase=Phase.DECODE)
        }
      )
    }
)
public class UICategoryForm extends BaseUIFAQForm implements UIPopupComponent {
  private String              categoryId_                      = "";

  private String              parentId_;

  final private static String CATEGORY_DETAIL_TAB              = "UIAddCategoryForm";

  final private static String FIELD_NAME_INPUT                 = "eventCategoryName";

  final private static String FIELD_DESCRIPTION_INPUT          = "description";

  final private static String FIELD_USERPRIVATE_INPUT          = "userPrivate";

  final private static String FIELD_MODERATOR_INPUT            = "moderator";

  final private static String FIELD_INDEX_INPUT                = "index";

  final private static String FIELD_MODERATEQUESTIONS_CHECKBOX = "moderatequestions";

  public static final String  VIEW_AUTHOR_INFOR                = "ViewAuthorInfor".intern();

  final private static String FIELD_MODERATE_ANSWERS_CHECKBOX  = "moderateAnswers";

  public static final String  PERMISSION_TAB                   = "PermissionTab";

  private boolean             isAddNew_                        = true;

  private String              oldName_                         = "";

  private long                oldIndex_                        = 1l;

  private Category            currentCategory_                 = new Category();

  private int                id                           = 0;

  public UICategoryForm() throws Exception {
    setActions(new String[] { "Save", "Cancel" });
  }

  public void updateAddNew(boolean isAddNew, String spaceGroupId) throws Exception {
    isAddNew_ = isAddNew;
    
    
    UIFormInputWithActions inputset = new UIFormInputWithActions(CATEGORY_DETAIL_TAB);
    inputset.addUIFormInput(new UIFormStringInput(FIELD_NAME_INPUT, FIELD_NAME_INPUT, null).addValidator(MandatoryValidator.class));
    UIFormStringInput index = new UIFormStringInput(FIELD_INDEX_INPUT, FIELD_INDEX_INPUT, null);
    index.addValidator(PositiveNumberFormatValidator.class);
    if (isAddNew) {
      index.setValue(String.valueOf(getFAQService().getMaxindexCategory(parentId_) + 1));
    }
    inputset.addUIFormInput(index);
    
    inputset.addUIFormInput(new UIFormTextAreaInput(FIELD_DESCRIPTION_INPUT, FIELD_DESCRIPTION_INPUT, null));
    inputset.addUIFormInput(new UICheckBoxInput(FIELD_MODERATEQUESTIONS_CHECKBOX, FIELD_MODERATEQUESTIONS_CHECKBOX, false));
    inputset.addUIFormInput(new UICheckBoxInput(VIEW_AUTHOR_INFOR, VIEW_AUTHOR_INFOR, false));
    inputset.addUIFormInput(new UICheckBoxInput(FIELD_MODERATE_ANSWERS_CHECKBOX, FIELD_MODERATE_ANSWERS_CHECKBOX, false));
    addUIFormInput(inputset);
    
    UIPermissionPanel permissionPanel = createUIComponent(UIPermissionPanel.class, null, PERMISSION_TAB);
    permissionPanel.setPermission(spaceGroupId, new String[] { FIELD_MODERATOR_INPUT, FIELD_USERPRIVATE_INPUT });
    addChild(permissionPanel);
    
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public String getParentId() {
    return parentId_;
  }

  public void setParentId(String s) {
    parentId_ = s;
  }

  public void setCategoryValue(Category cat, boolean isUpdate) throws Exception {
    if (isUpdate) {
      isAddNew_ = false;
      categoryId_ = cat.getPath();
      currentCategory_ = cat;
      oldName_ = cat.getName();
      oldIndex_ = cat.getIndex();
      if (oldName_ != null && oldName_.trim().length() > 0) {
        getUIStringInput(FIELD_NAME_INPUT).setValue(CommonUtils.decodeSpecialCharToHTMLnumber(oldName_));
      } else {
        getUIStringInput(FIELD_NAME_INPUT).setValue("Root");
      }

      getUIStringInput(FIELD_INDEX_INPUT).setValue(String.valueOf(cat.getIndex()));
      getUIFormTextAreaInput(FIELD_DESCRIPTION_INPUT).setDefaultValue(cat.getDescription());
      getUICheckBoxInput(FIELD_MODERATEQUESTIONS_CHECKBOX).setChecked(cat.isModerateQuestions());
      getUICheckBoxInput(FIELD_MODERATE_ANSWERS_CHECKBOX).setChecked(cat.isModerateAnswers());
      getUICheckBoxInput(VIEW_AUTHOR_INFOR).setChecked(cat.isViewAuthorInfor());

      String[] moderators = (CommonUtils.isEmpty(cat.getModerators())) ? new String[] { FAQUtils.getCurrentUser() } : cat.getModerators();
      
      UIPermissionPanel permissionTab = getChildById(PERMISSION_TAB);
      permissionTab.addPermissionForOwners(FIELD_MODERATOR_INPUT, moderators);
      permissionTab.addPermissionForOwners(FIELD_USERPRIVATE_INPUT, cat.getUserPrivate());
    }
  }

  static public class SaveActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm uiCategory = event.getSource();
      String name = uiCategory.getUIStringInput(FIELD_NAME_INPUT).getValue();
      name = CommonUtils.encodeSpecialCharInTitle(name).replaceAll("( \\s*)", CommonUtils.SPACE).trim();
      if ((uiCategory.isAddNew_ || !name.equals(uiCategory.oldName_)) && 
          uiCategory.getFAQService().isCategoryExist(name, uiCategory.parentId_)) {
        uiCategory.warning("UICateforyForm.sms.cate-name-exist");
        return;
      }
      UIFormInputWithActions inputset = uiCategory.getChildById(CATEGORY_DETAIL_TAB);
      UIPermissionPanel permissionTab = uiCategory.getChildById(PERMISSION_TAB);
      long index = uiCategory.oldIndex_;
      String strIndex = inputset.getUIStringInput(FIELD_INDEX_INPUT).getValue();
      if (!CommonUtils.isEmpty(strIndex)) {
        index = Long.parseLong(strIndex);
        if(index > uiCategory.getFAQService().getMaxindexCategory(uiCategory.parentId_) + 1) {
          uiCategory.warning("UICateforyForm.msg.over-index-number", uiCategory.getLabel(FIELD_INDEX_INPUT));
          return;
        }
      } else if(uiCategory.isAddNew_){
        index = uiCategory.getFAQService().getMaxindexCategory(uiCategory.parentId_) + 1;
      }
      String description = inputset.getUIFormTextAreaInput(FIELD_DESCRIPTION_INPUT).getValue();
      String moderator = permissionTab.getOwnersByPermission(FIELD_MODERATOR_INPUT);
      String userPrivate = permissionTab.getOwnersByPermission(FIELD_USERPRIVATE_INPUT);
      String erroUser = UserHelper.checkValueUser(userPrivate);
      if (!FAQUtils.isFieldEmpty(erroUser)) {
        uiCategory.warning("UICateforyForm.sms.user-not-found", new String[] { uiCategory.getLabel(FIELD_USERPRIVATE_INPUT), erroUser });
        return;
      }
      String[] userPrivates = new String[] { CommonUtils.EMPTY_STR };
      if (!CommonUtils.isEmpty(userPrivate)) {
        userPrivates = FAQUtils.splitForFAQ(userPrivate);
      }
      erroUser = UserHelper.checkValueUser(moderator);
      if (!FAQUtils.isFieldEmpty(erroUser)) {
        uiCategory.warning("UICateforyForm.sms.user-not-found", new String[] { uiCategory.getLabel(FIELD_MODERATOR_INPUT), erroUser });
        return;
      }

      boolean moderatequestion = uiCategory.getUICheckBoxInput(FIELD_MODERATEQUESTIONS_CHECKBOX).isChecked();
      boolean moderateAnswer = uiCategory.getUICheckBoxInput(FIELD_MODERATE_ANSWERS_CHECKBOX).isChecked();
      boolean viewAuthorInfor = uiCategory.getUICheckBoxInput(VIEW_AUTHOR_INFOR).isChecked();
      String[] users = FAQUtils.splitForFAQ(moderator);

      Category cat = uiCategory.currentCategory_;
      cat.setName(name);
      cat.setUserPrivate(userPrivates);
      cat.setDescription(description);
      cat.setModerateQuestions(moderatequestion);
      cat.setModerateAnswers(moderateAnswer);
      cat.setViewAuthorInfor(viewAuthorInfor);
      cat.setIndex(index);
      cat.setModerators(users);
      uiCategory.getFAQService().saveCategory(uiCategory.parentId_, cat, uiCategory.isAddNew_);

      UIAnswersPortlet answerPortlet = uiCategory.getAncestorOfType(UIAnswersPortlet.class);
      if (!uiCategory.isAddNew_) {
        UICategories categories = answerPortlet.findFirstComponentOfType(UICategories.class);
        if (uiCategory.categoryId_.equals(categories.getCategoryPath())) {
          UIQuestions questions = answerPortlet.findFirstComponentOfType(UIQuestions.class);
          questions.viewAuthorInfor = uiCategory.getFAQService().isViewAuthorInfo(uiCategory.categoryId_);
          UIBreadcumbs breadcumbs = answerPortlet.getChild(UIAnswersContainer.class).getChild(UIBreadcumbs.class);
          breadcumbs.setUpdataPath(uiCategory.categoryId_);
        }
      }

      answerPortlet.cancelAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(answerPortlet.getChild(UIAnswersContainer.class));
    }
  }
  
  protected boolean getIsSelected(int id) {
    if (this.id == id)
      return true;
    return false;
  }
  
  static public class SelectTabActionListener extends BaseEventListener<UICategoryForm> {
    public void onEvent(Event<UICategoryForm> event, UICategoryForm uiForm, String id) throws Exception {
      uiForm.id = Integer.parseInt(id);
      UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
      if (uiForm.id == 1) {
        popupWindow.setWindowSize(550, 440);
      } else {
        popupWindow.setWindowSize(550, 380);
      }
      Util.getPortalRequestContext().setResponseComplete(true);
    }
  }


  static public class CancelActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      event.getSource().getAncestorOfType(UIAnswersPortlet.class).cancelAction();
    }
  }
}
