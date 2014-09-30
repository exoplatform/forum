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
package org.exoplatform.answer.webui.popup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.ValidatorDataInput;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
   lifecycle = UIFormLifecycle.class, 
   template = "app:/templates/answer/webui/popup/UIAnswerEditModeForm.gtmpl", 
     events = {
         @EventConfig(listeners = UIAnswerEditModeForm.SaveActionListener.class), 
         @EventConfig(listeners = UIAnswerEditModeForm.ChildTabChangeActionListener.class, phase = Phase.DECODE), 
         @EventConfig(listeners = UIAnswerEditModeForm.ResetMailContentActionListener.class, phase = Phase.DECODE), 
         @EventConfig(listeners = UIAnswerEditModeForm.SelectCategoryForumActionListener.class, phase = Phase.DECODE), 
         @EventConfig(listeners = UIAnswerEditModeForm.CancelActionListener.class, phase = Phase.DECODE) 
     }
 )
public class UIAnswerEditModeForm extends BaseCategoryTreeInputForm implements UIPopupComponent {
  public static final String  DISPLAY_TAB                      = "DisplayTab";

  public static final String  SET_DEFAULT_EMAIL_TAB            = "DefaultEmail";

  public static final String  SET_DEFAULT_ADDNEW_QUESTION_TAB  = "AddNewQuestionTab";

  public static final String  SET_DEFAULT_EDIT_QUESTION_TAB    = "EditQuestionTab";

  public static final String  SET_EMAIL_MOVE_QUESTION_TAB      = "EmailMoveQuestionTab";

  public static final String  ITEM_VOTE                        = "vote";

  public static final String  DISPLAY_MODE                     = "display-mode";

  public static final String  ORDER_BY                         = "order-by";

  public static final String  ORDER_TYPE                       = "order-type";

  public static final String  ENABLE_VOTE_COMMNET              = "enableVotComment";

  public static final String  ENABLE_ANONYMOUS_SUBMIT_QUESTION = "enableAnonymousSubmitQuestion";

  public static final String  ITEM_CREATE_DATE                 = "created";

  public static final String  ITEM_ALPHABET_INDEX              = "alphabetIndex";

  public static final String  ASC                              = "asc";

  public static final String  DESC                             = "desc";

  public static final String  ENABLE_RSS                       = "enableRSS";

  public static final String  ENABLE_VIEW_AVATAR               = "enableViewAvatar";

  public static final String  EMAIL_DEFAULT_ADD_QUESTION       = "EmailAddNewQuestion";

  public static final String  EMAIL_DEFAULT_EDIT_QUESTION      = "EmailEditQuestion";

  public static final String  EMAIL_MOVE_QUESTION              = "EmailMoveQuestion";

  public static final String  DISCUSSION_TAB                   = "Discussion";

  public static final String  FIELD_CATEGORY_PATH_INPUT        = "CategoryPath";

  public static final String  ENABLE_DISCUSSION                = "EnableDiscuss";

  public static final String  POST_QUESTION_IN_ROOT_CATEGORY   = "isPostQuestionInRootCategory";

  private FAQSetting          faqSetting_                      = new FAQSetting();

  private List<String>        idForumName                      = new ArrayList<String>();

  protected boolean           isResetMail                     = false;

  protected int               indexOfTab                       = 0;

  private String              tabSelected                      = DISPLAY_TAB;

  public UIAnswerEditModeForm() throws Exception {
    super();
    UIFormInputWithActions displayMainTab = new UIFormInputWithActions(DISPLAY_TAB);
    UIFormInputWithActions emailMainTab = new UIFormInputWithActions(SET_DEFAULT_EMAIL_TAB);
    UIFormInputWithActions emailAddNewQuestionSubTab = new UIFormInputWithActions(SET_DEFAULT_ADDNEW_QUESTION_TAB);
    UIFormInputWithActions emailEditQuestionSubTab = new UIFormInputWithActions(SET_DEFAULT_EDIT_QUESTION_TAB);
    UIFormInputWithActions emailMoveQuestionSubTab = new UIFormInputWithActions(SET_EMAIL_MOVE_QUESTION_TAB);
    UIFormInputWithActions discussionMainTab = new UIFormInputWithActions(DISCUSSION_TAB);

    displayMainTab.addUIFormInput(new UIFormSelectBox(DISPLAY_MODE, DISPLAY_MODE, null));
    displayMainTab.addUIFormInput(new UIFormSelectBox(ORDER_BY, ORDER_BY, null));
    displayMainTab.addUIFormInput(new UIFormSelectBox(ORDER_TYPE, ORDER_TYPE, null));
    displayMainTab.addUIFormInput(new UICheckBoxInput(ENABLE_VOTE_COMMNET, ENABLE_VOTE_COMMNET, false));
    displayMainTab.addUIFormInput(new UICheckBoxInput(ENABLE_ANONYMOUS_SUBMIT_QUESTION, ENABLE_ANONYMOUS_SUBMIT_QUESTION, false));
    displayMainTab.addUIFormInput(new UICheckBoxInput(ENABLE_RSS, ENABLE_RSS, false));
    displayMainTab.addUIFormInput(new UICheckBoxInput(ENABLE_VIEW_AVATAR, ENABLE_VIEW_AVATAR, false));
    displayMainTab.addUIFormInput(new UICheckBoxInput(POST_QUESTION_IN_ROOT_CATEGORY, POST_QUESTION_IN_ROOT_CATEGORY, true));

    emailAddNewQuestionSubTab.addUIFormInput(addNewUIFormRichtextInput(EMAIL_DEFAULT_ADD_QUESTION));
    emailEditQuestionSubTab.addUIFormInput(addNewUIFormRichtextInput(EMAIL_DEFAULT_EDIT_QUESTION));
    emailMoveQuestionSubTab.addUIFormInput(addNewUIFormRichtextInput(EMAIL_MOVE_QUESTION));

    emailMainTab.addChild(emailAddNewQuestionSubTab);
    emailMainTab.addChild(emailEditQuestionSubTab);
    emailMainTab.addChild(emailMoveQuestionSubTab);

    discussionMainTab.addUIFormInput(new UICheckBoxInput(ENABLE_DISCUSSION, ENABLE_DISCUSSION, false));
    discussionMainTab.addUIFormInput(new UIFormStringInput(FIELD_CATEGORY_PATH_INPUT, FIELD_CATEGORY_PATH_INPUT, "").setReadOnly(true));

    List<ActionData> actionData = new ArrayList<ActionData>();
    ActionData ad = new ActionData();
    ad.setActionListener("SelectCategoryForum");
    ad.setActionName("SelectCategoryForum");
    ad.setActionType(ActionData.TYPE_ICON);
    ad.setCssIconClass("uiIconPlus uiIconLightGray");
    actionData.add(ad);
    discussionMainTab.setActionField(FIELD_CATEGORY_PATH_INPUT, actionData);

    addChild(displayMainTab);
    addChild(emailMainTab);
    addChild(discussionMainTab);

    displayMainTab.setRendered(true);
    emailAddNewQuestionSubTab.setRendered(true);
    emailEditQuestionSubTab.setRendered(true);
    emailMainTab.setRendered(true);
    //
    setAddColonInLabel(true);
  }

  private UIFormRichtextInput addNewUIFormRichtextInput(String id) throws Exception {
	  UIFormRichtextInput richtext = new UIFormRichtextInput(id, id, "");
    richtext.setIsPasteAsPlainText(true)
            .setIgnoreParserHTML(true)
            .setToolbar(UIFormRichtextInput.FORUM_TOOLBAR);
    richtext.addValidator(MandatoryValidator.class);
    return richtext;
  }

  public void activate() {}

  public void deActivate() {}
  
  public void initContainer() throws Exception {
    FAQUtils.getPorletPreference(faqSetting_);
    UIFormInputWithActions inputWithActions = getChildById(DISPLAY_TAB);

    List<SelectItemOption<String>> displayMode = new ArrayList<SelectItemOption<String>>();
    displayMode.add(new SelectItemOption<String>(getLabel(FAQSetting.DISPLAY_APPROVED), FAQSetting.DISPLAY_APPROVED));
    displayMode.add(new SelectItemOption<String>(getLabel(FAQSetting.DISPLAY_BOTH), FAQSetting.DISPLAY_BOTH));
    inputWithActions.getUIFormSelectBox(DISPLAY_MODE).setOptions(displayMode).setValue(faqSetting_.getDisplayMode());

    List<SelectItemOption<String>> orderBy = new ArrayList<SelectItemOption<String>>();
    orderBy.add(new SelectItemOption<String>(getLabel(ITEM_CREATE_DATE), FAQSetting.DISPLAY_TYPE_POSTDATE));
    orderBy.add(new SelectItemOption<String>(getLabel(ITEM_ALPHABET_INDEX), FAQSetting.DISPLAY_TYPE_ALPHABET + "/Index"));
    inputWithActions.getUIFormSelectBox(ORDER_BY).setOptions(orderBy).setValue(faqSetting_.getOrderBy());

    List<SelectItemOption<String>> orderType = new ArrayList<SelectItemOption<String>>();
    orderType.add(new SelectItemOption<String>(getLabel(ASC), FAQSetting.ORDERBY_TYPE_ASC));
    orderType.add(new SelectItemOption<String>(getLabel(DESC), FAQSetting.ORDERBY_TYPE_DESC));
    inputWithActions.getUIFormSelectBox(ORDER_TYPE).setOptions(orderType).setValue(faqSetting_.getOrderType());

    inputWithActions.getUICheckBoxInput(ENABLE_VOTE_COMMNET).setValue(faqSetting_.isEnanbleVotesAndComments());
    inputWithActions.getUICheckBoxInput(ENABLE_ANONYMOUS_SUBMIT_QUESTION).setValue(faqSetting_.isEnableAnonymousSubmitQuestion());
    inputWithActions.getUICheckBoxInput(ENABLE_RSS).setValue(faqSetting_.isEnableAutomaticRSS());
    inputWithActions.getUICheckBoxInput(ENABLE_VIEW_AVATAR).setValue(faqSetting_.isEnableViewAvatar());
    inputWithActions.getUICheckBoxInput(POST_QUESTION_IN_ROOT_CATEGORY).setValue(faqSetting_.isPostQuestionInRootCategory());

    //
    inputWithActions = getChildById(DISCUSSION_TAB);
    inputWithActions.getUICheckBoxInput(ENABLE_DISCUSSION).setChecked(faqSetting_.getIsDiscussForum());
    String pathCate = faqSetting_.getIdNameCategoryForum();
    idForumName.clear();
    if (pathCate.indexOf(";") > 0) {
      this.idForumName.add(pathCate.substring(0, pathCate.indexOf(";")));
      this.idForumName.add(pathCate.substring(pathCate.indexOf(";") + 1));
    } else {
      this.idForumName.add("");
      this.idForumName.add("");
    }
    inputWithActions.getUIStringInput(FIELD_CATEGORY_PATH_INPUT).setValue(idForumName.get(1));

    //
    FAQUtils.getEmailSetting(faqSetting_, true, false);
    setValueEmailContent(SET_DEFAULT_ADDNEW_QUESTION_TAB, EMAIL_DEFAULT_ADD_QUESTION, faqSetting_.getEmailSettingContent());

    //
    FAQUtils.getEmailSetting(faqSetting_, false, false);
    setValueEmailContent(SET_DEFAULT_EDIT_QUESTION_TAB, EMAIL_DEFAULT_EDIT_QUESTION, faqSetting_.getEmailSettingContent());

    //
    setValueEmailContent(SET_EMAIL_MOVE_QUESTION_TAB, EMAIL_MOVE_QUESTION, faqSetting_.getEmailMoveQuestion());

    //
    categoryTree = FAQUtils.getFAQService().buildCategoryTree(null);
    
    //
    categoryMap.clear();
    categoryStatus.clear();
    categoriesChecked = null;
  }

  private void setValueEmailContent(String tabId, String editorId, String value) {
    UIFormInputWithActions emailTab = getChildById(SET_DEFAULT_EMAIL_TAB);
    UIFormInputWithActions inputWithActions = emailTab.getChildById(tabId);
    inputWithActions.getChild(UIFormRichtextInput.class).setValue(value);
  }

  private String getValueEmailContent(String tabId, String editorId) {
    UIFormInputWithActions emailTab = getChildById(SET_DEFAULT_EMAIL_TAB);
    UIFormInputWithActions inputWithActions = emailTab.getChildById(tabId);
    return inputWithActions.getChild(UIFormRichtextInput.class).getValue();
  }

  public void setPathCatygory(List<String> idForumName) {
    this.idForumName = idForumName;
    UIFormInputWithActions discussionTab = getChildById(DISCUSSION_TAB);
    discussionTab.getUIStringInput(FIELD_CATEGORY_PATH_INPUT).setValue(idForumName.get(1));
    discussionTab.getUICheckBoxInput(ENABLE_DISCUSSION).setChecked(true);
  }

  protected String getSelectedTab() {
    return tabSelected;
  }

  static public class SaveActionListener extends EventListener<UIAnswerEditModeForm> {
    public void execute(Event<UIAnswerEditModeForm> event) throws Exception {
      UIAnswerEditModeForm settingForm = event.getSource();
      UIAnswersPortlet uiPortlet = settingForm.getAncestorOfType(UIAnswersPortlet.class);
      FAQSetting faqSetting = settingForm.faqSetting_;

      //
      UIFormInputWithActions discussionTab = settingForm.getChildById(DISCUSSION_TAB);
      boolean isDiscuss = discussionTab.getUICheckBoxInput(ENABLE_DISCUSSION).isChecked();
      if (isDiscuss) {
        String value = discussionTab.getUIStringInput(FIELD_CATEGORY_PATH_INPUT).getValue();
        if (!settingForm.idForumName.isEmpty() && !FAQUtils.isFieldEmpty(value)) {
          faqSetting.setIdNameCategoryForum(settingForm.idForumName.get(0) + ";" + settingForm.idForumName.get(1));
        } else {
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIAnswerEditModeForm.msg.pathCategory-empty", null, ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
          return;
        }
      } else {
        faqSetting.setIdNameCategoryForum("");
      }
      faqSetting.setIsDiscussForum(isDiscuss);

      //
      UIFormInputWithActions displayTab = settingForm.getChildById(DISPLAY_TAB);
      faqSetting.setDisplayMode(((UIFormSelectBox) displayTab.getChildById(DISPLAY_MODE)).getValue());
      faqSetting.setOrderBy(String.valueOf(((UIFormSelectBox) displayTab.getChildById(ORDER_BY)).getValue()));
      faqSetting.setOrderType(String.valueOf(((UIFormSelectBox) displayTab.getChildById(ORDER_TYPE)).getValue()));
      faqSetting.setEnanbleVotesAndComments(displayTab.getUICheckBoxInput(ENABLE_VOTE_COMMNET).isChecked());
      faqSetting.setEnableAnonymousSubmitQuestion(displayTab.getUICheckBoxInput(ENABLE_ANONYMOUS_SUBMIT_QUESTION).isChecked());
      faqSetting.setEnableAutomaticRSS(displayTab.getUICheckBoxInput(ENABLE_RSS).isChecked());
      faqSetting.setEnableViewAvatar(displayTab.getUICheckBoxInput(ENABLE_VIEW_AVATAR).isChecked());
      faqSetting.setPostQuestionInRootCategory(displayTab.getUICheckBoxInput(POST_QUESTION_IN_ROOT_CATEGORY).isChecked());

      String defaultAddnewQuestion = settingForm.getValueEmailContent(SET_DEFAULT_ADDNEW_QUESTION_TAB, EMAIL_DEFAULT_ADD_QUESTION);
      String defaultEditQuestion = settingForm.getValueEmailContent(SET_DEFAULT_EDIT_QUESTION_TAB, EMAIL_DEFAULT_EDIT_QUESTION);
      String emailMoveQuestion = settingForm.getValueEmailContent(SET_EMAIL_MOVE_QUESTION_TAB, EMAIL_MOVE_QUESTION);
      
      if (defaultAddnewQuestion == null || !ValidatorDataInput.fckContentIsNotEmpty(defaultAddnewQuestion))
        defaultAddnewQuestion = " ";
      if (defaultEditQuestion == null || !ValidatorDataInput.fckContentIsNotEmpty(defaultEditQuestion))
        defaultEditQuestion = " ";
      faqSetting.setEmailMoveQuestion(emailMoveQuestion);
      //
      FAQUtils.savePortletPreference(faqSetting, defaultAddnewQuestion.replaceAll("&amp;", "&"), defaultEditQuestion.replaceAll("&amp;", "&"));

      //
      UIFormInputWithActions scopingTab = settingForm.getChildById(CATEGORY_SCOPING);
      Set<String> listCateIds = new HashSet<String>();
      List<UIComponent> childrens = scopingTab.getChildren();
      for (UIComponent child : childrens) {
        if(child instanceof UICheckBoxInput) {
          boolean ischecked = ((UICheckBoxInput)child).isChecked();
          if(Boolean.valueOf(ischecked).equals(settingForm.categoryStatus.get(child.getId())) == false) {
            listCateIds.add(settingForm.categoryMap.get(child.getId()));
            settingForm.categoryStatus.put(child.getId(), Boolean.valueOf(ischecked)) ;
          }
        }
      }
      
      if (listCateIds.isEmpty() == false){
        FAQUtils.getFAQService().changeStatusCategoryView(new ArrayList<String>(listCateIds));
      }
      
      //
      settingForm.initContainer();
      event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIAnswerEditModeForm.msg.update-successful", null, ApplicationMessage.INFO));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
    }
  }

  static public class ResetMailContentActionListener extends EventListener<UIAnswerEditModeForm> {
    public void execute(Event<UIAnswerEditModeForm> event) throws Exception {
      UIAnswerEditModeForm settingForm = event.getSource();
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      String emailContent = "";
      if (id.equals("0")) {
        emailContent = settingForm.i18n("SendEmail.AddNewQuestion.Default");
        settingForm.setValueEmailContent(SET_DEFAULT_ADDNEW_QUESTION_TAB, EMAIL_DEFAULT_ADD_QUESTION, emailContent);
      } else if (id.equals("1")) {
        emailContent = settingForm.i18n("SendEmail.ResponseQuestion.Default");
        settingForm.setValueEmailContent(SET_DEFAULT_EDIT_QUESTION_TAB, EMAIL_DEFAULT_EDIT_QUESTION, emailContent);
      } else {
        emailContent = settingForm.i18n("SendEmail.MoveQuetstion.Default");
        settingForm.setValueEmailContent(SET_EMAIL_MOVE_QUESTION_TAB, EMAIL_MOVE_QUESTION, emailContent);
      }
      settingForm.isResetMail = true;
      settingForm.indexOfTab = Integer.parseInt(id);
      event.getRequestContext().addUIComponentToUpdateByAjax(settingForm);
    }
  }

  static public class ChildTabChangeActionListener extends EventListener<UIAnswerEditModeForm> {
    public void execute(Event<UIAnswerEditModeForm> event) throws Exception {
      UIAnswerEditModeForm settingForm = event.getSource();
      String[] tabId = event.getRequestContext().getRequestParameter(OBJECTID).split("/");
      String tab = tabId[0];
      if (tab.equals("parent")) {
        settingForm.tabSelected = tabId[1];
        settingForm.isResetMail = false;
      } else {
        settingForm.indexOfTab = Integer.parseInt(tabId[1]);
        settingForm.isResetMail = true;
      }
      
      UIFormInputWithActions categoryScoping = settingForm.getChildById(CATEGORY_SCOPING);
      settingForm.categoriesChecked = new ArrayList<String>();
      List<UIComponent> childs = categoryScoping.getChildren();
      for (UIComponent child : childs) {
        if(child instanceof UICheckBoxInput) {
          boolean ischecked = ((UICheckBoxInput) child).isChecked();
          if(ischecked) {
            settingForm.categoriesChecked.add(child.getId());
          }
        }
        
      }
      if (settingForm.categoriesChecked.size() == 0) settingForm.categoriesChecked = null;
      event.getRequestContext().addUIComponentToUpdateByAjax(settingForm.getParent());
    }
  }

  static public class SelectCategoryForumActionListener extends EventListener<UIAnswerEditModeForm> {
    public void execute(Event<UIAnswerEditModeForm> event) throws Exception {
      UIAnswerEditModeForm settingForm = event.getSource();
      UIAnswersPortlet uiPortlet = settingForm.getAncestorOfType(UIAnswersPortlet.class);
      try {
        UIPopupContainer watchContainer = settingForm.getAncestorOfType(UIPopupContainer.class);
        settingForm.openPopup(watchContainer, UISelectCategoryForumForm.class, 400, 0);
      } catch (Exception e) {
        UIPopupAction popupAction = uiPortlet.getChild(UIPopupAction.class);
        UISelectCategoryForumForm listCateForm = popupAction.createUIComponent(UISelectCategoryForumForm.class, null, null);
        popupAction.activate(listCateForm, 400, 0);
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIAnswerEditModeForm> {
    public void execute(Event<UIAnswerEditModeForm> event) throws Exception {
      UIAnswerEditModeForm settingForm = event.getSource();
      UIAnswersPortlet uiPortlet = settingForm.getAncestorOfType(UIAnswersPortlet.class);
      uiPortlet.cancelAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet.getChild(UIAnswersContainer.class));
    }
  }
}
