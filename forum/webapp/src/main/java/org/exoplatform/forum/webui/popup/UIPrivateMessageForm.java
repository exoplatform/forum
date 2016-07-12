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
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.*;
import org.exoplatform.forum.service.ForumPrivateMessage;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.popup.UIForumInputWithActions.ActionData;
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
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/templates/forum/webui/popup/UIPrivateMessegeForm.gtmpl", events = {
        @EventConfig(listeners = UIPrivateMessageForm.AddValuesUserActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIPrivateMessageForm.AddUserActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIPrivateMessageForm.SelectTabActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIPrivateMessageForm.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIPrivateMessageForm.SendPrivateMessageActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(id = "UIPMUserPopupWindow", type = UIPopupWindow.class, template = "system:/groovy/webui/core/UIPopupWindow.gtmpl", events = {
        @EventConfig(listeners = UIPrivateMessageForm.ClosePopupActionListener.class, name = "ClosePopup"),
        @EventConfig(listeners = UIPrivateMessageForm.AddActionListener.class, name = "Add", phase = Phase.DECODE),
        @EventConfig(listeners = UIPrivateMessageForm.CloseActionListener.class, name = "Close", phase = Phase.DECODE) }) })
public class UIPrivateMessageForm extends BaseForumForm implements UIPopupComponent, UISelector {
  public static final String FIELD_SENDTO_TEXT         = "SendTo";
  public static final String FIELD_MAILTITLE_INPUT     = "MailTitle";
  public static final String FIELD_MAILMESSAGE_INPUT   = "MailMessage";
  public static final String FIELD_SENDMESSAGE_TAB     = "MessageTab";
  public static final String FIELD_REPLY_LABEL         = "Reply";
  public static final String FIELD_FORWARD_LABEL       = "Forward";
  public static final String USER_SELECTOR_POPUPWINDOW = "UIPMUserPopupWindow";
  private String             userName;
  private int                id                        = 0;
  private boolean            fullMessage               = true;

  public UIPrivateMessageForm() throws Exception {
    UIFormStringInput sendTo = new UIFormStringInput(FIELD_SENDTO_TEXT, FIELD_SENDTO_TEXT, null);
    sendTo.addValidator(MandatoryValidator.class);
    UIFormStringInput mailTitle = new UIFormStringInput(FIELD_MAILTITLE_INPUT, FIELD_MAILTITLE_INPUT, null);
    mailTitle.addValidator(MandatoryValidator.class);
    UIFormRichtextInput formWYSIWYGInput = new UIFormRichtextInput(FIELD_MAILMESSAGE_INPUT,
                                                                   FIELD_MAILMESSAGE_INPUT,
                                                                   CommonUtils.EMPTY_STR);
    formWYSIWYGInput.addValidator(MandatoryValidator.class);
    formWYSIWYGInput.setIsPasteAsPlainText(true).setIgnoreParserHTML(true).setToolbar(UIFormRichtextInput.FORUM_TOOLBAR);
    UIForumInputWithActions sendMessageTab = new UIForumInputWithActions(FIELD_SENDMESSAGE_TAB);
    sendMessageTab.addUIFormInput(sendTo);
    sendMessageTab.addUIFormInput(mailTitle);
    sendMessageTab.addUIFormInput(formWYSIWYGInput);

    String[] strings = new String[] { "SelectUser", "SelectMemberShip", "SelectGroup" };
    String[] icons = ForumUtils.getClassIconWithAction();
    ActionData ad;
    int i = 0;
    List<ActionData> actions = new ArrayList<ActionData>();
    for (String string : strings) {
      ad = new ActionData();
      if (i == 0)
        ad.setActionListener("AddUser");
      else
        ad.setActionListener("AddValuesUser");
      ad.setActionParameter(String.valueOf(i));
      ad.setCssIconClass(icons[i]);
      ad.setActionName("UIPrivateMessageForm.label.action." + string);
      actions.add(ad);
      ++i;
    }
    sendMessageTab.setActionField(FIELD_SENDTO_TEXT, actions);
    addUIFormInput(sendMessageTab);
    addChild(UIListInBoxPrivateMessage.class, null, null);
    addChild(UIListSentPrivateMessage.class, null, null);
    setAddColonInLabel(true);
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
    this.userName = userProfile.getUserId();
  }

  public void setSendtoField(String str) {
    getUIStringInput(FIELD_SENDTO_TEXT).setValue(str);
  }

  public void updateSelect(String selectField, String value) throws Exception {
    UIFormStringInput fieldInput = getUIStringInput(selectField);
    String values = fieldInput.getValue();
    fieldInput.setValue(ForumUtils.updateMultiValues(value, values));
  }

  private String removeCurrentUser(String s) throws Exception {
    if (s.equals(userName))
      return ForumUtils.EMPTY_STR;
    if (s.contains(userName + ForumUtils.COMMA))
      s = StringUtils.remove(s, userName + ForumUtils.COMMA);
    if (s.contains(ForumUtils.COMMA + userName))
      s = StringUtils.remove(s, ForumUtils.COMMA + userName);
    return s;
  }

  protected int getIsSelected() {
    return this.id;
  }

  public void setUpdate(ForumPrivateMessage privateMessage, boolean isReply) throws Exception {
    UIForumInputWithActions messageTab = getChildById(FIELD_SENDMESSAGE_TAB);
    UIFormStringInput stringInput = messageTab.getUIStringInput(FIELD_MAILTITLE_INPUT);
    UIFormRichtextInput message = messageTab.getChild(UIFormRichtextInput.class);
    String content = privateMessage.getMessage();

    String replyLabel = getLabel(FIELD_REPLY_LABEL) + CommonUtils.COLON;
    String forwardLabel = getLabel(FIELD_FORWARD_LABEL) + CommonUtils.COLON;

    String title = CommonUtils.decodeSpecialCharToHTMLnumber(privateMessage.getName());
    title = getTitleMessage(getTitleMessage(title, replyLabel), forwardLabel);

    if (isReply) {
      setSendtoField(privateMessage.getFrom());
      stringInput.setValue(replyLabel + title);
      content = new StringBuffer("<br/><br/><br/><div style=\"padding: 5px; border-left:solid 2px blue;\">").append(content)
                                                                                                            .append("</div>")
                                                                                                            .toString();
    } else {
      setSendtoField(CommonUtils.EMPTY_STR);
      stringInput.setValue(forwardLabel + title);
    }
    message.setValue(content);
    this.id = 2;
  }

  private String getTitleMessage(String title, String defautlLabel) {
    if (CommonUtils.isEmpty(title) == true) {
      return CommonUtils.EMPTY_STR;
    }
    while (title.indexOf(defautlLabel) == 0) {
      title = title.replaceFirst(defautlLabel, CommonUtils.EMPTY_STR);
    }
    return title;
  }

  public boolean isFullMessage() {
    return fullMessage;
  }

  public void setFullMessage(boolean fullMessage) {
    this.fullMessage = fullMessage;
  }

  static public class SelectTabActionListener extends EventListener<UIPrivateMessageForm> {
    public void execute(Event<UIPrivateMessageForm> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIPrivateMessageForm messageForm = event.getSource();
      messageForm.id = Integer.parseInt(id);
      event.getRequestContext().addUIComponentToUpdateByAjax(messageForm);
    }
  }

  static public class SendPrivateMessageActionListener extends EventListener<UIPrivateMessageForm> {
    public void execute(Event<UIPrivateMessageForm> event) throws Exception {
      UIPrivateMessageForm messageForm = event.getSource();
      UIForumInputWithActions messageTab = messageForm.getChildById(FIELD_SENDMESSAGE_TAB);
      UIFormStringInput inputSendTo = messageForm.getUIStringInput(FIELD_SENDTO_TEXT);
      String sendTo = inputSendTo.getValue();
      sendTo = ForumUtils.removeSpaceInString(sendTo);
      sendTo = ForumUtils.removeStringResemble(sendTo);
      if (ForumUtils.isEmpty(sendTo)) {
        messageForm.warning("EmptyFieldValidator.msg.empty-input", messageForm.getLabel(FIELD_SENDTO_TEXT));
        return;
      }

      sendTo = messageForm.removeCurrentUser(sendTo);
      if (ForumUtils.isEmpty(sendTo)) {
        messageForm.warning("UIPrivateMessageForm.msg.sendToCurrentUser",
                            new String[] { messageForm.getLabel(FIELD_SENDTO_TEXT) });
        return;
      }
      String erroUser = UserHelper.checkValueUser(sendTo);
      if (!ForumUtils.isEmpty(erroUser)) {
        messageForm.warning("NameValidator.msg.erroUser-input",
                            new String[] { messageForm.getLabel(FIELD_SENDTO_TEXT), erroUser });
        return;
      }
      UIFormStringInput mailTitleInput = messageTab.getUIStringInput(FIELD_MAILTITLE_INPUT);
      String mailTitle = mailTitleInput.getValue();
      if (ForumUtils.isEmpty(mailTitle)) {
        messageForm.warning("EmptyFieldValidator.msg.empty-input", messageForm.getLabel(FIELD_MAILTITLE_INPUT));
        return;
      }
      int maxText = 80;
      if (mailTitle.length() > maxText) {
        messageForm.warning("NameValidator.msg.warning-long-text", new String[] { messageForm.getLabel(FIELD_MAILTITLE_INPUT),
            String.valueOf(maxText) });
        return;
      }
      mailTitle = CommonUtils.encodeSpecialCharInTitle(mailTitle);
      UIFormRichtextInput formWYSIWYGInput = messageTab.getChild(UIFormRichtextInput.class);
      String message = formWYSIWYGInput.getValue();
      message = HTMLSanitizer.sanitize(message);
      if (!ForumUtils.isEmpty(message)) {
        ForumPrivateMessage privateMessage = new ForumPrivateMessage();
        privateMessage.setFrom(messageForm.userName);
        privateMessage.setSendTo(sendTo);
        privateMessage.setName(mailTitle);
        privateMessage.setMessage(message);
        try {
          messageForm.getForumService().savePrivateMessage(privateMessage);
        } catch (Exception e) {
          messageForm.log.warn("Failed to save private message", e);
        }
        inputSendTo.setValue(ForumUtils.EMPTY_STR);
        mailTitleInput.setValue(ForumUtils.EMPTY_STR);
        formWYSIWYGInput.setValue(ForumUtils.EMPTY_STR);
        messageForm.info("UIPrivateMessageForm.msg.sent-successfully", false);
        if (messageForm.fullMessage) {
          messageForm.id = 1;
          event.getRequestContext().addUIComponentToUpdateByAjax(messageForm.getParent());
        } else {
          UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
          forumPortlet.cancelAction();
        }
      } else {
        messageForm.warning("EmptyFieldValidator.msg.empty-input", messageForm.getLabel(FIELD_MAILMESSAGE_INPUT));
      }
    }
  }

  static public class AddValuesUserActionListener extends EventListener<UIPrivateMessageForm> {
    public void execute(Event<UIPrivateMessageForm> event) throws Exception {
      UIPrivateMessageForm messageForm = event.getSource();
      String type = event.getRequestContext().getRequestParameter(OBJECTID);
      if (!ForumUtils.isEmpty(type)) {
        UIPopupContainer popupContainer = messageForm.getAncestorOfType(UIPopupContainer.class);
        UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class);
        UIUserSelect uiUserSelect = popupContainer.findFirstComponentOfType(UIUserSelect.class);
        if (uiUserSelect != null) {
          UIPopupWindow popupWindow = uiUserSelect.getParent();
          closePopupWindow(popupWindow);
        }
        UIGroupSelector uiGroupSelector = popupAction.activate(UIGroupSelector.class, 600);
        if (type.equals(UIGroupSelector.TYPE_MEMBERSHIP))
          uiGroupSelector.setId("UIMemberShipSelector");
        else if (type.equals(UIGroupSelector.TYPE_GROUP))
          uiGroupSelector.setId("UIGroupSelector");
        uiGroupSelector.setType(type);
        uiGroupSelector.setSelectedGroups(null);
        uiGroupSelector.setComponent(messageForm, new String[] { FIELD_SENDTO_TEXT });
        uiGroupSelector.getChild(UITree.class).setId(UIGroupSelector.TREE_GROUP_ID);
        uiGroupSelector.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(UIGroupSelector.BREADCUMB_GROUP_ID);
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIPrivateMessageForm> {
    public void execute(Event<UIPrivateMessageForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
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

  static public class AddActionListener extends EventListener<UIUserSelect> {
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiUserSelector = event.getSource();
      String values = uiUserSelector.getSelectedUsers();
      UIForumPortlet forumPortlet = uiUserSelector.getAncestorOfType(UIForumPortlet.class);
      UIPrivateMessageForm messageForm = forumPortlet.findFirstComponentOfType(UIPrivateMessageForm.class);
      if (messageForm != null) {
        messageForm.updateSelect(FIELD_SENDTO_TEXT, values);
      }
      UIPopupWindow popupWindow = uiUserSelector.getParent();
      closePopupWindow(popupWindow);
      UIPopupContainer uiPopupContainer = messageForm.getAncestorOfType(UIPopupContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  static public class AddUserActionListener extends EventListener<UIPrivateMessageForm> {
    public void execute(Event<UIPrivateMessageForm> event) throws Exception {
      UIPrivateMessageForm messageForm = event.getSource();
      UIPopupContainer uiPopupContainer = messageForm.getAncestorOfType(UIPopupContainer.class);
      messageForm.showUIUserSelect(uiPopupContainer, USER_SELECTOR_POPUPWINDOW, ForumUtils.EMPTY_STR);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
}
