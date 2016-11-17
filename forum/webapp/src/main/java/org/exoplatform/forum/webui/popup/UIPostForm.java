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

import javax.jcr.PathNotFoundException;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.bbcode.core.ExtendedBBCodeProvider;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.*;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
import org.exoplatform.forum.webui.popup.UIForumInputWithActions.ActionData;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/templates/forum/webui/popup/UIPostForm.gtmpl", events = {
    @EventConfig(listeners = UIPostForm.PreviewPostActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPostForm.SubmitPostActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPostForm.AttachmentActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPostForm.RemoveAttachmentActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPostForm.SelectTabActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIPostForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UIPostForm extends BaseForumForm implements UIPopupComponent {
  public static final String    FIELD_POSTTITLE_INPUT  = "PostTitle";

  public static final String    FIELD_EDITREASON_INPUT = "editReason";

  public static final String    FIELD_LABEL_QUOTE      = "ReUser";

  final static public String    FIELD_ATTACHMENTS      = "Attachments";

  final static public String    FIELD_FROM_INPUT       = "FromInput";

  final static public String    FIELD_MESSAGECONTENT   = "MessageContent";

  public static final String    FIELD_THREADCONTEN_TAB = "ThreadContent";

  public static String          STR_RE                 = "";

  private int                   tabId                  = 0;

  private List<ForumAttachment> attachments_           = new ArrayList<ForumAttachment>();

  private String                categoryId;

  private String                forumId;

  private String                topicId;

  private String                postId                 = ForumUtils.EMPTY_STR;

  private boolean               isMod                  = false;

  private Topic                 topic;

  private Post                  post_                  = new Post();

  private boolean               isQuote                = false;

  private boolean               isMP                   = false;

  private boolean               isDoubleClickSubmit    = false;

  public UIPostForm() throws Exception {
    if (ForumUtils.isEmpty(getId()))
      setId("UIPostForm");
    isDoubleClickSubmit = false;
    UIFormStringInput postTitle = new UIFormStringInput(FIELD_POSTTITLE_INPUT, FIELD_POSTTITLE_INPUT, null);
    postTitle.addValidator(MandatoryValidator.class);
    UIFormStringInput editReason = new UIFormStringInput(FIELD_EDITREASON_INPUT, FIELD_EDITREASON_INPUT, null);
    editReason.setRendered(false);
    UIForumInputWithActions threadContent = new UIForumInputWithActions(FIELD_THREADCONTEN_TAB);

    UIFormRichtextInput richtext = new UIFormRichtextInput(FIELD_MESSAGECONTENT, FIELD_MESSAGECONTENT, ForumUtils.EMPTY_STR);
    richtext.setIsPasteAsPlainText(true).setIgnoreParserHTML(true).setToolbar(UIFormRichtextInput.FORUM_TOOLBAR);
    richtext.addValidator(MandatoryValidator.class);

    threadContent.addChild(postTitle);
    threadContent.addChild(editReason);
    threadContent.addChild(richtext);
    threadContent.addUIFormInput(new UIFormInputInfo(FIELD_ATTACHMENTS, FIELD_ATTACHMENTS, null));
    threadContent.setActionField(FIELD_THREADCONTEN_TAB, getUploadFileList());
    threadContent.setActionIdAddItem(FIELD_ATTACHMENTS);
    threadContent.setActionAddItem("Attachment");
    threadContent.setLabelActionAddItem(getLabel("Attachment"));

    addUIFormInput(threadContent);
    this.setActions(new String[] { "SubmitPost", "PreviewPost", "Cancel" });
    setAddColonInLabel(true);
  }

  protected boolean tabIsSelected(int tabId) {
    if (this.tabId == tabId)
      return true;
    else
      return false;
  }

  protected String[] getTabName() {
    String[] tab = { "UIPostForm.tittle.threadContent", "UIPostForm.tittle.iconAndSmiley" };
    return tab;
  }

  public void setPostIds(String categoryId, String forumId, String topicId, Topic topic) {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.topicId = topicId;
    this.topic = topic;
  }

  public List<ActionData> getUploadFileList() {
    List<ActionData> uploadedFiles = new ArrayList<ActionData>();
    for (ForumAttachment attachdata : attachments_) {
      ActionData fileUpload = new ActionData();
      fileUpload.setActionListener(ForumUtils.EMPTY_STR);
      fileUpload.setActionType(ActionData.TYPE_ATT);
      String fileName = attachdata.getName();
      fileUpload.setActionName(fileName + "(" + ForumUtils.getSizeFile(attachdata.getSize()) + ")");
      fileUpload.setShowLabel(true);
      fileUpload.setCssIconClass(CssClassUtils.getCSSClassByFileNameAndFileType(fileName, attachdata.getMimeType(), null));
      uploadedFiles.add(fileUpload);

      ActionData removeAction = new ActionData();
      removeAction.setActionListener("RemoveAttachment");
      removeAction.setActionName("UITopicForm.action.RemoveAttachment");
      removeAction.setActionParameter(attachdata.getId());
      removeAction.setActionType(ActionData.TYPE_ICON);
      removeAction.setCssIconClass("uiIconDelete uiIconLightGray");
      removeAction.setBreakLine(true);
      uploadedFiles.add(removeAction);
    }
    return uploadedFiles;
  }

  public void refreshUploadFileList() throws Exception {
    UIForumInputWithActions threadContent = this.getChildById(FIELD_THREADCONTEN_TAB);
    threadContent.setActionField(FIELD_ATTACHMENTS, getUploadFileList());
  }

  public void addUploadFile(ForumAttachment attachfile) {
    attachments_.add(attachfile);
  }

  public void addUploadFileList(List<BufferAttachment> attachfiles) {
    attachments_.addAll(attachfiles);
  }

  public void removeFromUploadFileList(ForumAttachment attachfile) {
    attachments_.remove(attachfile);
  }

  public void removeUploadFileList() {
    attachments_.clear();
  }

  public List<ForumAttachment> getAttachFileList() {
    return attachments_;
  }

  public void updatePost(String postId, boolean isQuote, boolean isPP, Post post) throws Exception {
    if (post != null)
      this.post_ = post;
    this.postId = postId;
    this.isQuote = isQuote;
    this.isMP = isPP;
    UIForumInputWithActions threadContent = this.getChildById(FIELD_THREADCONTEN_TAB);
    UIFormStringInput editReason = threadContent.getUIStringInput(FIELD_EDITREASON_INPUT);
    editReason.setRendered(false);
    if (!ForumUtils.isEmpty(this.postId) && post != null) {
      String message = CommonUtils.decodeSpecialCharToHTMLnumberIgnore(post.getMessage());
      if (isQuote) {// quote
        threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT)
                     .setValue(CommonUtils.decodeSpecialCharToHTMLnumber(getTitle(post.getName())));

        String value = "[QUOTE=" + getForumService().getScreenName(post.getOwner()) + "]" + message + "[/QUOTE]";
        threadContent.getChild(UIFormRichtextInput.class).setValue(value);
      } else if (isPP) {
        threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT)
                     .setValue(CommonUtils.decodeSpecialCharToHTMLnumber(getTitle(topic.getTopicName())));
      } else {// edit
        this.attachments_.clear();
        editReason.setRendered(true);
        threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT).setValue(CommonUtils.decodeSpecialCharToHTMLnumber(post.getName()));
        if (post.getAttachments() != null && post.getAttachments().size() > 0) {
          this.attachments_.addAll(post.getAttachments());
          this.refreshUploadFileList();
        }
        threadContent.getChild(UIFormRichtextInput.class).setValue(message);
      }
    } else {
      if (!isQuote) {// reply
        threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT)
                     .setValue(CommonUtils.decodeSpecialCharToHTMLnumber(getTitle(topic.getTopicName())));
      }
    }
  }

  private String getTitle(String title) {
    if (ForumUtils.isEmpty(STR_RE)) {
      STR_RE = getLabel(FIELD_LABEL_QUOTE) + ": ";
    }
    while (title.indexOf(STR_RE.trim()) == 0) {
      title = title.replaceFirst(STR_RE.trim(), ForumUtils.EMPTY_STR).trim();
    }
    return STR_RE + title;
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public boolean isMod() {
    return isMod;
  }

  public void setMod(boolean isMod) {
    this.isMod = isMod;
  }

  static public class PreviewPostActionListener extends BaseEventListener<UIPostForm> {
    public void onEvent(Event<UIPostForm> event, UIPostForm uiForm, String id) throws Exception {
      UIForumInputWithActions threadContent = uiForm.getChildById(FIELD_THREADCONTEN_TAB);
      int t = 0, k = 1;
      String postTitle = threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT).getValue();
      String userName = uiForm.getUserProfile().getUserId();
      String message = threadContent.getChild(UIFormRichtextInput.class).getValue();
      String checksms = TransformHTML.cleanHtmlCode(message,
                                                    new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes()));
      checksms = checksms.replaceAll("&nbsp;", " ");
      t = checksms.trim().length();
      if (ForumUtils.isEmpty(postTitle)) {
        k = 0;
      }
      if (t > 0 && k != 0 && !checksms.equals("null")) {
        Date currentDate = CommonUtils.getGreenwichMeanTime().getTime();
        postTitle = CommonUtils.encodeSpecialCharInTitle(postTitle);
        Post post = uiForm.post_;
        post.setName(postTitle);
        post.setMessage(message);
        if (CommonUtils.isEmpty(uiForm.postId) || (uiForm.isQuote || uiForm.isMP)) {
          post.setOwner(userName);
          post.setCreatedDate(currentDate);
        }
        post.setModifiedDate(currentDate);
        post.setModifiedBy(userName);
        post.setRemoteAddr(ForumUtils.EMPTY_STR);
        post.setIcon("uiIconForumTopic uiIconForumLightGray");
        post.setIsApproved(false);
        post.setAttachments(uiForm.getAttachFileList());
        UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
        UIViewPost viewPost = uiForm.openPopup(popupContainer, UIViewPost.class, 670, 0);
        viewPost.setPostView(post);
        viewPost.setActionForm(new String[] { "Close" });
      } else {
        String[] args = { ForumUtils.EMPTY_STR };
        if (k == 0) {
          args = new String[] { uiForm.getLabel(FIELD_POSTTITLE_INPUT) };
          if (t <= 0)
            args = new String[] { uiForm.getLabel(FIELD_POSTTITLE_INPUT) + ", " + uiForm.getLabel(FIELD_MESSAGECONTENT) };
          uiForm.warning("NameValidator.msg.ShortText", args);
        } else if (t <= 0) {
          args = new String[] { uiForm.getLabel(FIELD_MESSAGECONTENT) };
          uiForm.warning("NameValidator.msg.ShortMessage", args);
        }
      }
    }
  }

  static public class SubmitPostActionListener extends BaseEventListener<UIPostForm> {
    public void onEvent(Event<UIPostForm> event, UIPostForm uiForm, String id) throws Exception {
      if (uiForm.isDoubleClickSubmit)
        return;
      uiForm.isDoubleClickSubmit = true;
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      UserProfile userProfile = forumPortlet.getUserProfile();
      try {
        if (forumPortlet.checkForumHasAddPost(uiForm.categoryId, uiForm.forumId, uiForm.topicId)) {
          UIForumInputWithActions threadContent = uiForm.getChildById(FIELD_THREADCONTEN_TAB);
          int t = 0, k = 1;
          String postTitle = threadContent.getUIStringInput(FIELD_POSTTITLE_INPUT).getValue();
          boolean isAddRe = false;
          int maxText = ForumUtils.MAXTITLE;
          if (!ForumUtils.isEmpty(postTitle)) {
            while (postTitle.indexOf(uiForm.getTitle("").trim()) == 0) {
              postTitle = postTitle.replaceFirst(STR_RE.trim(), ForumUtils.EMPTY_STR).trim();
              isAddRe = true;
            }
            if (postTitle.length() > maxText) {
              warning("NameValidator.msg.warning-long-text",
                      new String[] { uiForm.getLabel(FIELD_POSTTITLE_INPUT), String.valueOf(maxText) });
              uiForm.isDoubleClickSubmit = false;
              return;
            }
          }
          String message = threadContent.getChild(UIFormRichtextInput.class).getValue();
          String checksms = TransformHTML.cleanHtmlCode(message,
                                                        new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes()));
          checksms = checksms.replaceAll("&nbsp;", " ");
          t = checksms.length();
          if (ForumUtils.isEmpty(postTitle)) {
            k = 0;
          }
          if (t > 0 && k != 0 && !checksms.equals("null")) {
            String editReason = threadContent.getUIStringInput(FIELD_EDITREASON_INPUT).getValue();
            if (!ForumUtils.isEmpty(editReason) && editReason.length() > maxText) {
              warning("NameValidator.msg.warning-long-text",
                      new String[] { uiForm.getLabel(FIELD_EDITREASON_INPUT), String.valueOf(maxText) });
              uiForm.isDoubleClickSubmit = false;
              return;
            }
            String userName = userProfile.getUserId();
            editReason = CommonUtils.encodeSpecialCharInTitle(editReason);
            message = TransformHTML.fixAddBBcodeAction(message);
            postTitle = CommonUtils.encodeSpecialCharInTitle(postTitle);
            Post post = uiForm.post_;
            boolean isPP = false;
            boolean isOffend = false;
            boolean hasTopicMod = false;
            if (!uiForm.isMod()) {
              String[] censoredKeyword = ForumUtils.getCensoredKeyword(uiForm.getForumService());
              checksms = checksms.toLowerCase().trim();
              for (String string : censoredKeyword) {
                if (checksms.indexOf(string.trim()) >= 0) {
                  isOffend = true;
                  break;
                }
                if (postTitle.toLowerCase().indexOf(string.trim()) >= 0) {
                  isOffend = true;
                  break;
                }
              }
              if (post.getUserPrivate() != null && post.getUserPrivate().length == 2)
                isPP = true;
              if ((!uiForm.isMP || !isPP) && uiForm.topic != null)
                hasTopicMod = uiForm.topic.getIsModeratePost();
            }
            if (isOffend && (uiForm.isMP || isPP)) {
              uiForm.warning("UIPostForm.msg.PrivateCensor");
              uiForm.isDoubleClickSubmit = false;
              return;
            }
            // set link
            String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, uiForm.topicId, false);
            //
            Date currentDate = CommonUtils.getGreenwichMeanTime().getTime();
            if (uiForm.isQuote || uiForm.isMP || CommonUtils.isEmpty(uiForm.postId)) {
              post = new Post();
              post.setOwner(userName);
              post.setCreatedDate(currentDate);
            }
            post.setName((isAddRe) ? uiForm.getTitle(postTitle) : postTitle);
            message = HTMLSanitizer.sanitize(message);
            post.setMessage(message);
            post.setIcon("uiIconForumTopic uiIconForumLightGray");
            post.setAttachments(uiForm.getAttachFileList());
            post.setIsWaiting(isOffend);
            post.setLink(link);
            String[] userPrivate = new String[] { "exoUserPri" };
            if (uiForm.isMP) {
              userPrivate = new String[] { userName, uiForm.post_.getOwner() };
              hasTopicMod = false;
            }
            post.setUserPrivate(userPrivate);
            post.setIsApproved(!hasTopicMod);

            UITopicDetailContainer topicDetailContainer = forumPortlet.findFirstComponentOfType(UITopicDetailContainer.class);
            UITopicDetail topicDetail = topicDetailContainer.getChild(UITopicDetail.class);
            boolean isParentDelete = false;
            boolean isNew = false;
            try {
              MessageBuilder messageBuilder = ForumUtils.getDefaultMail();
              messageBuilder.setLink(link);
              if (!ForumUtils.isEmpty(uiForm.postId)) {
                if (uiForm.isQuote || uiForm.isMP) {
                  post.setRemoteAddr(WebUIUtils.getRemoteIP());
                  try {
                    uiForm.getForumService().savePost(uiForm.categoryId,
                                                      uiForm.forumId,
                                                      uiForm.topicId,
                                                      post,
                                                      true,
                                                      ForumUtils.getDefaultMail());
                    isNew = true;
                  } catch (PathNotFoundException e) {
                    isParentDelete = true;
                  }
                  topicDetail.setIdPostView("lastpost");
                } else {
                  // post.setId(uiForm.postId) ;
                  post.setModifiedBy(userName);
                  post.setModifiedDate(currentDate);
                  post.setEditReason(editReason);
                  try {
                    uiForm.getForumService().savePost(uiForm.categoryId,
                                                      uiForm.forumId,
                                                      uiForm.topicId,
                                                      post,
                                                      false,
                                                      messageBuilder);
                  } catch (PathNotFoundException e) {
                    isParentDelete = true;
                  }
                  topicDetail.setIdPostView(uiForm.postId);
                }
              } else {
                post.setRemoteAddr(WebUIUtils.getRemoteIP());
                try {
                  uiForm.getForumService()
                        .savePost(uiForm.categoryId, uiForm.forumId, uiForm.topicId, post, true, messageBuilder);
                  isNew = true;
                } catch (PathNotFoundException e) {
                  isParentDelete = true;
                } catch (Exception ex) {
                  uiForm.log.warn(String.format("Failed to save post %s", post.getName()), ex);
                }
                topicDetail.setIdPostView("lastpost");
              }
              if (isNew) {
                if (userProfile.getIsAutoWatchTopicIPost()) {
                  List<String> values = new ArrayList<String>();
                  values.add(userProfile.getEmail());
                  String path = uiForm.categoryId + ForumUtils.SLASH + uiForm.forumId + ForumUtils.SLASH + uiForm.topicId;
                  uiForm.getForumService().addWatch(1, path, values, userProfile.getUserId());
                }
              }
              uiForm.getForumService().updateTopicAccess(forumPortlet.getUserProfile().getUserId(), uiForm.topicId);
              forumPortlet.getUserProfile().setLastTimeAccessTopic(uiForm.topicId, currentDate.getTime());
            } catch (Exception e) {
              uiForm.log.warn("Failed to save topic", e);
            }
            uiForm.isMP = uiForm.isQuote = false;
            if (isParentDelete) {
              forumPortlet.cancelAction();
              uiForm.warning("UIPostForm.msg.isParentDelete");
              return;
            }
            forumPortlet.cancelAction();
            if (isOffend || hasTopicMod) {
              topicDetail.setIdPostView("normal");
              if (isOffend)
                uiForm.warning("MessagePost.msg.isOffend", false);
              else {
                uiForm.warning("MessagePost.msg.isModerate", false);
              }
            }
            event.getRequestContext().addUIComponentToUpdateByAjax(topicDetailContainer);
          } else {
            String[] args = { ForumUtils.EMPTY_STR };
            if (k == 0) {
              args = new String[] { uiForm.getLabel(FIELD_POSTTITLE_INPUT) };
              if (t == 0)
                args = new String[] { uiForm.getLabel(FIELD_POSTTITLE_INPUT) + ", " + uiForm.getLabel(FIELD_MESSAGECONTENT) };
              uiForm.isDoubleClickSubmit = false;
              uiForm.warning("NameValidator.msg.ShortMessage", args);
            } else if (t == 0) {
              args = new String[] { uiForm.getLabel(FIELD_MESSAGECONTENT) };
              uiForm.isDoubleClickSubmit = false;
              uiForm.warning("NameValidator.msg.ShortMessage", args);
            }
          }
        } else {
          forumPortlet.cancelAction();
          forumPortlet.removeCacheUserProfile();
          UITopicDetail topicDetail = forumPortlet.findFirstComponentOfType(UITopicDetail.class);
          topicDetail.initInfoTopic(uiForm.categoryId, uiForm.forumId, uiForm.topic, 0);
          uiForm.warning("UIPostForm.msg.no-permission", false);
          event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        }
      } catch (Exception e) {
        uiForm.log.error("Can not save post into this topic, exception: " + e.getMessage(), e);
        uiForm.warning("UIPostForm.msg.isParentDelete", false);
        forumPortlet.cancelAction();
      }
    }
  }

  static public class AttachmentActionListener extends EventListener<UIPostForm> {
    public void execute(Event<UIPostForm> event) throws Exception {
      UIPostForm uiForm = event.getSource();
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UIAttachFileForm attachFileForm = uiForm.openPopup(popupContainer, UIAttachFileForm.class, 500, 0);
      attachFileForm.updateIsTopicForm(false);
      attachFileForm.setMaxField(5, false);
    }
  }

  static public class SelectTabActionListener extends BaseEventListener<UIPostForm> {
    public void onEvent(Event<UIPostForm> event, UIPostForm postForm, String id) throws Exception {
      postForm.tabId = Integer.parseInt(id);
      Util.getPortalRequestContext().setResponseComplete(true);
    }
  }

  static public class RemoveAttachmentActionListener extends BaseEventListener<UIPostForm> {
    public void onEvent(Event<UIPostForm> event, UIPostForm uiPostForm, String attFileId) throws Exception {
      for (ForumAttachment att : uiPostForm.attachments_) {
        if (att.getId().equals(attFileId)) {
          uiPostForm.removeFromUploadFileList(att);
          break;
        }
      }
      UIForumInputWithActions threadContent = uiPostForm.getChildById(FIELD_THREADCONTEN_TAB);
      threadContent.setActionField(FIELD_ATTACHMENTS, uiPostForm.getUploadFileList());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPostForm);
    }
  }

  static public class CancelActionListener extends EventListener<UIPostForm> {
    public void execute(Event<UIPostForm> event) throws Exception {
      UIPostForm uiForm = event.getSource();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }
}
