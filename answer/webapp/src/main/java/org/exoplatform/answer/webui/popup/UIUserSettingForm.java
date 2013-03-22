/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.answer.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UICategories;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.input.UICheckBoxInput;

@ComponentConfig(lifecycle = UIFormLifecycle.class, 
  template = "app:/templates/answer/webui/popup/UIUserSettingForm.gtmpl", 
  events = {
      @EventConfig(listeners = UIUserSettingForm.SaveActionListener.class),
      @EventConfig(listeners = UIUserSettingForm.ChangeAvatarActionListener.class),
      @EventConfig(listeners = UIUserSettingForm.OpenTabActionListener.class),
      @EventConfig(listeners = UIUserSettingForm.CancelActionListener.class) 
})

public class UIUserSettingForm extends BaseUIFAQForm implements UIPopupComponent{

  public static final String ORDER_BY         = "order-by".intern();

  public static final String ORDER_TYPE       = "order-type".intern();

  public final String        ITEM_VOTE        = "vote";

  private FAQService         faqService_;

  private FAQSetting         faqSetting_      = new FAQSetting();

  private String             avatarUrl;

  private int                id               = 0;

  public final String        DISPLAY_TAB      = "DisplayTab";
  
  public static final String WATCHES_TAB      = "WatchesTab";

  public static final String ITEM_CREATE_DATE = "created".intern();

  public static final String ITEM_ALPHABET    = "alphabet".intern();

  public static final String ASC              = "asc".intern();

  public static final String DESC             = "desc".intern();
  
  protected String           tabId            = "DisplayTab";
  
  public static final String FIELD_IS_DELETE_AVATAR_CHECKBOX     = "IsDeleteAvatar";

  public UIUserSettingForm() throws Exception {
    faqService_ = FAQUtils.getFAQService();
    setActions(new String[] { "Save", "Cancel" });
  }
  
  public void init() throws Exception {
    UIFormInputWithActions displayTab = new UIFormInputWithActions(DISPLAY_TAB);
    List<SelectItemOption<String>> orderBy = new ArrayList<SelectItemOption<String>>();
    orderBy.add(new SelectItemOption<String>(getLabel(ITEM_CREATE_DATE), FAQSetting.DISPLAY_TYPE_POSTDATE));
    orderBy.add(new SelectItemOption<String>(getLabel(ITEM_ALPHABET) + "/Index", FAQSetting.DISPLAY_TYPE_ALPHABET + "/Index"));
    displayTab.addUIFormInput((new UIFormSelectBox(ORDER_BY, ORDER_BY, orderBy)).setValue(String.valueOf(faqSetting_.getOrderBy())));

    List<SelectItemOption<String>> orderType = new ArrayList<SelectItemOption<String>>();
    orderType.add(new SelectItemOption<String>(getLabel(ASC), FAQSetting.ORDERBY_TYPE_ASC));
    orderType.add(new SelectItemOption<String>(getLabel(DESC), FAQSetting.ORDERBY_TYPE_DESC));
    displayTab.addUIFormInput((new UIFormSelectBox(ORDER_TYPE, ORDER_TYPE, orderType)).setValue(String.valueOf(faqSetting_.getOrderType())));

    displayTab.addUIFormInput((new UICheckBoxInput(ITEM_VOTE, ITEM_VOTE, false)).setChecked(faqSetting_.isSortQuestionByVote()));
    displayTab.addUIFormInput((new UICheckBoxInput(FIELD_IS_DELETE_AVATAR_CHECKBOX, FIELD_IS_DELETE_AVATAR_CHECKBOX, false)).setChecked(false));

    setAvatarUrl(FAQUtils.getUserAvatar(FAQUtils.getCurrentUser()));
    
    addChild(displayTab);
    
    //UIUserWatchManager watchForm = new UIUserWatchManager();
    UIUserWatchManager watchForm = createUIComponent(UIUserWatchManager.class, null, WATCHES_TAB);
    watchForm.setFAQSetting(faqSetting_);
    addChild(watchForm);
  }
  
  static public class SaveActionListener extends EventListener<UIUserSettingForm> {
    public void execute(Event<UIUserSettingForm> event) throws Exception {
      UIUserSettingForm settingForm = event.getSource();
      UIAnswersPortlet uiPortlet = settingForm.getAncestorOfType(UIAnswersPortlet.class);
      FAQSetting faqSetting = settingForm.faqSetting_;
      faqSetting.setOrderBy(String.valueOf(settingForm.getUIFormSelectBox(ORDER_BY).getValue()));
      faqSetting.setOrderType(String.valueOf(settingForm.getUIFormSelectBox(ORDER_TYPE).getValue()));
      faqSetting.setSortQuestionByVote(settingForm.getUICheckBoxInput(settingForm.ITEM_VOTE)
                                                  .isChecked());
      
      if (settingForm.getUICheckBoxInput(FIELD_IS_DELETE_AVATAR_CHECKBOX).isChecked()) {
        settingForm.faqService_.setDefaultAvatar(FAQUtils.getCurrentUser());
        settingForm.setAvatarUrl(Utils.DEFAULT_AVATAR_URL);
      }
      settingForm.faqService_.saveFAQSetting(faqSetting, FAQUtils.getCurrentUser());
      UIPopupAction uiPopupAction = settingForm.getAncestorOfType(UIPopupAction.class);
      uiPopupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
      UIQuestions questions = uiPortlet.findFirstComponentOfType(UIQuestions.class);
      UICategories categories = uiPortlet.findFirstComponentOfType(UICategories.class);
      categories.resetListCate();
      questions.setFAQSetting(faqSetting);
      questions.setListObject();
      questions.updateCurrentQuestionList();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet);
    }
  }
  
  static public class OpenTabActionListener extends BaseEventListener<UIUserSettingForm> {
    public void onEvent(Event<UIUserSettingForm> event, UIUserSettingForm uiForm, String objectId) throws Exception {
      uiForm.tabId = objectId;
      Util.getPortalRequestContext().setResponseComplete(true);
    }
  }
  
  static public class ChangeAvatarActionListener extends BaseEventListener<UIUserSettingForm> {
    public void onEvent(Event<UIUserSettingForm> event, UIUserSettingForm settingForm, String objectId) throws Exception {
      UIPopupContainer watchContainer = settingForm.getAncestorOfType(UIPopupContainer.class);
      UIAttachmentForm attachMentForm = openPopup(watchContainer, UIAttachmentForm.class, 550, 0);
      attachMentForm.setIsChangeAvatar(true);
      attachMentForm.setNumberUpload(1);
    }
  }
  
  static public class CancelActionListener extends EventListener<UIUserSettingForm> {
    public void execute(Event<UIUserSettingForm> event) throws Exception {
      UIUserSettingForm settingForm = event.getSource();
      UIAnswersPortlet uiPortlet = settingForm.getAncestorOfType(UIAnswersPortlet.class);
      uiPortlet.cancelAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPortlet.getChild(UIAnswersContainer.class));
    }
  }
  
  protected boolean getIsSelected(int id) {
    if (this.id == id)
      return true;
    return false;
  }
  
  public FAQSetting getFaqSetting() {
    return faqSetting_;
  }

  public void setFaqSetting(FAQSetting faqSetting) {
    this.faqSetting_ = faqSetting;
  }
  
  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String url) {
    this.avatarUrl = url;
  }
  
  boolean isDefaultAvatar() {
    return avatarUrl != null && avatarUrl.indexOf(Utils.DEFAULT_AVATAR_URL) >= 0;
  }
  
  public void activate() {
  }

  public void deActivate() {
  }

}
