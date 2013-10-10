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

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.bbcode.core.ExtendedBBCodeProvider;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIForumCheckBoxInput;
import org.exoplatform.forum.common.webui.UIPermissionPanel;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.common.webui.cssfile.CssClassUtils;
import org.exoplatform.forum.service.BufferAttachment;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIBreadcumbs;
import org.exoplatform.forum.webui.UICategories;
import org.exoplatform.forum.webui.UICategoryContainer;
import org.exoplatform.forum.webui.UIForumContainer;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicContainer;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.popup.UIForumInputWithActions.ActionData;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
   lifecycle = UIFormLifecycle.class,
   template = "app:/templates/forum/webui/popup/UITopicForm.gtmpl",
   events = {
     @EventConfig(listeners = UITopicForm.PreviewThreadActionListener.class,phase = Phase.DECODE), 
     @EventConfig(listeners = UITopicForm.SubmitThreadActionListener.class,phase = Phase.DECODE), 
     @EventConfig(listeners = UITopicForm.AttachmentActionListener.class,phase = Phase.DECODE), 
     @EventConfig(listeners = UITopicForm.RemoveAttachmentActionListener.class,phase = Phase.DECODE), 
     @EventConfig(listeners = UITopicForm.CancelActionListener.class,phase = Phase.DECODE),
     @EventConfig(listeners = UITopicForm.SelectTabActionListener.class, phase=Phase.DECODE)
   }
)
public class UITopicForm extends BaseForumForm {

  public static final String    FIELD_THREADCONTEN_TAB           = "ThreadContent";

  public static final String    FIELD_THREADOPTION_TAB           = "ThreadOption";

  public static final String    FIELD_TOPICTITLE_INPUT           = "ThreadTitle";

  public static final String    FIELD_EDITREASON_INPUT           = "editReason";

  public static final String    FIELD_MESSAGE_TEXTAREA           = "Message";

  final static public String    FIELD_MESSAGECONTENT             = "messageContent";

  public static final String    FIELD_TOPICSTATUS_SELECTBOX      = "TopicStatus";

  public static final String    FIELD_TOPICSTATE_SELECTBOX       = "TopicState";

  public static final String    FIELD_APPROVED_CHECKBOX          = "Approved";

  public static final String    FIELD_MODERATEPOST_CHECKBOX      = "ModeratePost";

  public static final String    FIELD_NOTIFYWHENADDPOST_CHECKBOX = "NotifyWhenAddPost";

  public static final String    FIELD_STICKY_CHECKBOX            = "Sticky";
  
  final static public String    FIELD_ATTACHMENTS                = "attachments";

  final static public String    USER_SELECTOR_POPUPWINDOW        = "UITopicUserPopupWindow";
  
  public static final String    PERMISSION_TAB       = "ThreadPermission";

  public static final String    CANVIEW              = "CanView";

  public static final String    CANPOST              = "CanPost";


  private List<ForumAttachment> attachments_                     = new ArrayList<ForumAttachment>();

  private String                categoryId;

  private String                forumId;

  private String                topicId;

  private Forum                 forum;

  private boolean               isMod                            = false;

  private boolean               isDetail                         = false;

  private int                   id                               = 0;

  private Topic                 topic                            = new Topic();

  private boolean               isDoubleClickSubmit              = false;

  public UITopicForm() throws Exception {
    if (getId() == null)
      setId("UITopicForm");
    isDoubleClickSubmit = false;
    UIFormStringInput topicTitle = new UIFormStringInput(FIELD_TOPICTITLE_INPUT, FIELD_TOPICTITLE_INPUT, null);
    topicTitle.addValidator(MandatoryValidator.class);
    UIFormStringInput editReason = new UIFormStringInput(FIELD_EDITREASON_INPUT, FIELD_EDITREASON_INPUT, null);
    editReason.setRendered(false);

    UIForumCheckBoxInput topicState = new  UIForumCheckBoxInput(FIELD_TOPICSTATE_SELECTBOX, FIELD_TOPICSTATE_SELECTBOX, 
                                                                getLabel(FIELD_TOPICSTATE_SELECTBOX), false);
    UIForumCheckBoxInput topicStatus = new  UIForumCheckBoxInput(FIELD_TOPICSTATUS_SELECTBOX, FIELD_TOPICSTATUS_SELECTBOX,
                                                                 getLabel(FIELD_TOPICSTATUS_SELECTBOX), false);
    UIForumCheckBoxInput moderatePost = new UIForumCheckBoxInput(FIELD_MODERATEPOST_CHECKBOX, FIELD_MODERATEPOST_CHECKBOX, 
                                                                 getLabel(FIELD_MODERATEPOST_CHECKBOX), false);
    UIForumCheckBoxInput checkWhenAddPost = new UIForumCheckBoxInput(FIELD_NOTIFYWHENADDPOST_CHECKBOX, FIELD_NOTIFYWHENADDPOST_CHECKBOX,
                                                                     getLabel(FIELD_NOTIFYWHENADDPOST_CHECKBOX), false);
    UIForumCheckBoxInput sticky = new UIForumCheckBoxInput(FIELD_STICKY_CHECKBOX, FIELD_STICKY_CHECKBOX, 
                                                           getLabel(FIELD_STICKY_CHECKBOX), false);
        
    UIFormRichtextInput richtext = new UIFormRichtextInput(FIELD_MESSAGECONTENT, FIELD_MESSAGECONTENT, ForumUtils.EMPTY_STR);
    richtext.setToolbar(UIFormRichtextInput.FORUM_TOOLBAR);
    richtext.setIsPasteAsPlainText(true);

    UIForumInputWithActions threadContent = new UIForumInputWithActions(FIELD_THREADCONTEN_TAB);
    threadContent.addUIFormInput(topicTitle);
    threadContent.addUIFormInput(editReason);
    threadContent.addUIFormInput(richtext);
    threadContent.addUIFormInput(new UIFormInputInfo(FIELD_ATTACHMENTS, FIELD_ATTACHMENTS, null));
    threadContent.setActionField(FIELD_THREADCONTEN_TAB, getUploadFileList());
    threadContent.setActionIdAddItem(FIELD_ATTACHMENTS);
    threadContent.setActionAddItem("Attachment");
    threadContent.setLabelActionAddItem(getLabel("Attachment"));

    UIForumInputWithActions threadOption = new UIForumInputWithActions(FIELD_THREADOPTION_TAB);
    threadOption.addUIFormInput(topicState);
    threadOption.addUIFormInput(topicStatus);
    threadOption.addUIFormInput(sticky);
    threadOption.addUIFormInput(moderatePost);
    threadOption.addUIFormInput(checkWhenAddPost);

    addUIFormInput(threadContent);
    addUIFormInput(threadOption);
    
    UIPermissionPanel permissionTab = createUIComponent(UIPermissionPanel.class, null, PERMISSION_TAB);
    permissionTab.setPermission(null, new String[] { CANVIEW, CANPOST });
    addChild(permissionTab);
    
    this.setActions(new String[] { "SubmitThread", "PreviewThread", "Cancel" });
    setAddColonInLabel(true);
  }

  public void setSpaceGroupId(String spaceGroupId) {
    getChild(UIPermissionPanel.class).setSpaceGroupId(spaceGroupId);
  }

  public void setIsDetail(boolean isDetail) {
    this.isDetail = isDetail;
  }

  public void setTopicIds(String categoryId, String forumId, Forum forum) throws Exception {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.topic = new Topic();
    this.forum = forum;
    UIForumInputWithActions threadContent = this.getChildById(FIELD_THREADCONTEN_TAB);
    threadContent.getUIStringInput(FIELD_EDITREASON_INPUT).setRendered(false);
  }


  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {
  }

  protected boolean getIsSelected(int id) {
    if (this.id == id)
      return true;
    return false;
  }

  public List<ActionData> getUploadFileList() {
    List<ActionData> uploadedFiles = new ArrayList<ActionData>();
    for (ForumAttachment attachdata : attachments_) {
      ActionData fileUpload = new ActionData();
      fileUpload.setActionListener(ForumUtils.EMPTY_STR);
      fileUpload.setActionType(ActionData.TYPE_ATT);
      String fileName = attachdata.getName();;
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
    UIForumInputWithActions inputSet = getChildById(FIELD_THREADCONTEN_TAB);
    inputSet.setActionField(FIELD_ATTACHMENTS, getUploadFileList());
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

  public boolean isMod() {
    return isMod;
  }

  public void setMod(boolean isMod) {
    this.isMod = isMod;
  }
  
  private UIForumCheckBoxInput getUIForumCheckBoxInput(String id) {
    UIForumInputWithActions threadOption = getChildById(FIELD_THREADOPTION_TAB);
    UIForumCheckBoxInput boxInput = threadOption.findComponentById(id);
    return boxInput;
  }

  public void setUpdateTopic(Topic topic, boolean isUpdate) throws Exception {
    if (isUpdate) {
      this.topicId = topic.getId();
      this.topic = getForumService().getTopic(categoryId, forumId, topicId, ForumUtils.EMPTY_STR);
      UIForumInputWithActions threadContent = this.getChildById(FIELD_THREADCONTEN_TAB);
      threadContent.getUIStringInput(FIELD_EDITREASON_INPUT).setRendered(true);
      threadContent.getUIStringInput(FIELD_TOPICTITLE_INPUT).setValue(CommonUtils.decodeSpecialCharToHTMLnumber(topic.getTopicName()));
      threadContent.getChild(UIFormRichtextInput.class).setValue(CommonUtils.decodeSpecialCharToHTMLnumberIgnore(topic.getDescription()));

      getUIForumCheckBoxInput(FIELD_TOPICSTATE_SELECTBOX).setValue(topic.getIsClosed());
      
      getUIForumCheckBoxInput(FIELD_TOPICSTATUS_SELECTBOX).setValue(topic.getIsLock());

      getUIForumCheckBoxInput(FIELD_MODERATEPOST_CHECKBOX).setChecked(this.topic.getIsModeratePost());
      if (this.topic.getIsNotifyWhenAddPost() != null && this.topic.getIsNotifyWhenAddPost().trim().length() > 0) {
        getUIForumCheckBoxInput(FIELD_NOTIFYWHENADDPOST_CHECKBOX).setChecked(true);
      }
      getUIForumCheckBoxInput(FIELD_STICKY_CHECKBOX).setChecked(this.topic.getIsSticky());

      UIPermissionPanel permissionTab = this.getChildById(PERMISSION_TAB);
      permissionTab.addPermissionForOwners(CANVIEW, topic.getCanView());
      permissionTab.addPermissionForOwners(CANPOST, topic.getCanPost());
      String postId = topicId.replaceFirst(Utils.TOPIC, Utils.POST);
      Post post = getForumService().getPost(this.categoryId, this.forumId, this.topicId, postId);
      if (post != null && post.getAttachments() != null && post.getAttachments().size() > 0) {
        this.attachments_ = post.getAttachments();
        this.refreshUploadFileList();
      }
    }
  }

  static public class PreviewThreadActionListener extends BaseEventListener<UITopicForm> {
    public void onEvent(Event<UITopicForm> event, UITopicForm uiForm, final String objectId) throws Exception {
      int t = 0, k = 1;
      UIForumInputWithActions threadContent = uiForm.getChildById(FIELD_THREADCONTEN_TAB);
      String topicTitle = (" " + threadContent.getUIStringInput(FIELD_TOPICTITLE_INPUT).getValue()).trim();
      String message = threadContent.getChild(UIFormRichtextInput.class).getValue();
      String checksms = TransformHTML.cleanHtmlCode(message, new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes()));
      checksms = checksms.replaceAll("&nbsp;", " ");
      t = checksms.trim().length();
      if (topicTitle.length() <= 0 || topicTitle.equals("null")) {
        k = 0;
      }
      if (t > 0 && k != 0 && !checksms.equals("null")) {
        String userName = uiForm.getUserProfile().getUserId();
        topicTitle = CommonUtils.encodeSpecialCharInTitle(topicTitle);
        Post postNew = new Post();
        postNew.setOwner(userName);
        postNew.setName(topicTitle);
        if (ForumUtils.isEmpty(uiForm.topicId)) {
          postNew.setCreatedDate(CommonUtils.getGreenwichMeanTime().getTime());
          postNew.setModifiedDate(CommonUtils.getGreenwichMeanTime().getTime());
        } else {
          postNew.setCreatedDate(uiForm.topic.getCreatedDate());
          postNew.setModifiedDate(uiForm.topic.getModifiedDate());
        }
        postNew.setModifiedBy(userName);
        postNew.setMessage(message);
        postNew.setAttachments(uiForm.attachments_);
        postNew.setIcon("uiIconForumTopic uiIconForumLightGray");

        UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
        UIViewPost viewPost = openPopup(popupContainer, UIViewPost.class, "ViewTopic", 670, 0);
        viewPost.setPostView(postNew);
        viewPost.setActionForm(new String[] { "Close" });
      } else {
        String[] args = new String[] {  uiForm.getLabel(FIELD_MESSAGECONTENT) };
        if (k == 0) {
          args = new String[] { uiForm.getLabel(FIELD_TOPICTITLE_INPUT) };
          if (t <= 0)
            args = new String[] { uiForm.getLabel(FIELD_TOPICTITLE_INPUT) + " " + 
                                  uiForm.getLabel("and") + " " + uiForm.getLabel(FIELD_MESSAGECONTENT) };
          warning("NameValidator.msg.ShortMessage", args);
        } else if (t <= 0) {
          warning("NameValidator.msg.ShortMessage", args);
        }
        return;
      }
    }
  }

  static public class SubmitThreadActionListener extends BaseEventListener<UITopicForm> {
    public void onEvent(Event<UITopicForm> event, UITopicForm uiForm, final String objectId) throws Exception {
      if (uiForm.isDoubleClickSubmit)
        return;
      uiForm.isDoubleClickSubmit = true;
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      UserProfile userProfile = uiForm.getUserProfile();
      try {
        if (forumPortlet.checkForumHasAddTopic(uiForm.categoryId, uiForm.forumId)) {
          int t = 0, k = 1;
          UIForumInputWithActions threadContent = uiForm.getChildById(FIELD_THREADCONTEN_TAB);
          String topicTitle = (" " + threadContent.getUIStringInput(FIELD_TOPICTITLE_INPUT).getValue()).trim();
          int maxText = ForumUtils.MAXTITLE;
          if (topicTitle.length() > maxText) {
            String[] args = { uiForm.getLabel(FIELD_TOPICTITLE_INPUT), String.valueOf(maxText) };
            warning("NameValidator.msg.warning-long-text", args);
            uiForm.isDoubleClickSubmit = false;
            return;
          }
          String editReason = threadContent.getUIStringInput(FIELD_EDITREASON_INPUT).getValue();
          if (!ForumUtils.isEmpty(editReason) && editReason.length() > maxText) {
            String[] args = { uiForm.getLabel(FIELD_EDITREASON_INPUT), String.valueOf(maxText) };
            warning("NameValidator.msg.warning-long-text", args);
            uiForm.isDoubleClickSubmit = false;
            return;
          }
          String message = threadContent.getChild(UIFormRichtextInput.class).getValue();
          String checksms = TransformHTML.cleanHtmlCode(message, new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes()));
          checksms = checksms.replaceAll("&nbsp;", " ");
          t = checksms.trim().length();
          if (topicTitle.length() <= 0 || topicTitle.equals("null")) {
            k = 0;
          }
          if (t > 0 && k != 0 && !checksms.equals("null")) {
            message = CommonUtils.encodeSpecialCharInSearchTerm(message);
            message = TransformHTML.fixAddBBcodeAction(message);
            message = message.replaceAll("<script", "&lt;script").replaceAll("<link", "&lt;link").replaceAll("</script>", "&lt;/script>");
            boolean isOffend = false;
            boolean hasForumMod = false;
            if (!uiForm.isMod()) {
              String[] censoredKeyword = ForumUtils.getCensoredKeyword(uiForm.getForumService());
              checksms = checksms.toLowerCase();
              for (String string : censoredKeyword) {
                if (checksms.indexOf(string.trim()) >= 0) {
                  isOffend = true;
                  break;
                }
                if (topicTitle.toLowerCase().indexOf(string.trim()) >= 0) {
                  isOffend = true;
                  break;
                }
              }
              if (uiForm.forum != null)
                hasForumMod = uiForm.forum.getIsModerateTopic();
            }
            topicTitle = CommonUtils.encodeSpecialCharInTitle(topicTitle);
            editReason = CommonUtils.encodeSpecialCharInTitle(editReason);

            boolean topicState = uiForm.getUIForumCheckBoxInput(FIELD_TOPICSTATE_SELECTBOX).isChecked();
            boolean topicStatus = uiForm.getUIForumCheckBoxInput(FIELD_TOPICSTATUS_SELECTBOX).isChecked();
            boolean moderatePost = uiForm.getUIForumCheckBoxInput(FIELD_MODERATEPOST_CHECKBOX).isChecked();
            boolean whenNewPost = uiForm.getUIForumCheckBoxInput(FIELD_NOTIFYWHENADDPOST_CHECKBOX).isChecked();
            boolean sticky = uiForm.getUIForumCheckBoxInput(FIELD_STICKY_CHECKBOX).isChecked();

            UIPermissionPanel permissionTab = uiForm.getChildById(PERMISSION_TAB);
            String canPost = permissionTab.getOwnersByPermission(CANPOST);
            String canView = permissionTab.getOwnersByPermission(CANVIEW);
            
            // set link
            Topic topicNew = uiForm.topic;
            String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, topicNew.getId(), false);
            //
            String userName = userProfile.getUserId();
            topicNew.setOwner(userName);
            topicNew.setTopicName(topicTitle);
            topicNew.setCreatedDate(new Date());
            topicNew.setModifiedBy(userName);
            topicNew.setModifiedDate(new Date());
            topicNew.setLastPostBy(userName);
            topicNew.setLastPostDate(new Date());
            topicNew.setDescription(message);
            topicNew.setLink(link);
            if (whenNewPost) {
              String email = userProfile.getEmail();
              if (ForumUtils.isEmpty(email)) {
                try {
                  email = UserHelper.getUserByUserId(userName).getEmail();
                } catch (Exception e) {
                  email = "true";
                }
              }
              topicNew.setIsNotifyWhenAddPost(email);
            } else {
              topicNew.setIsNotifyWhenAddPost(ForumUtils.EMPTY_STR);
            }
            topicNew.setAttachments(uiForm.attachments_);
            topicNew.setIsWaiting(isOffend);
            topicNew.setIsClosed(topicState);
            topicNew.setIsLock(topicStatus);
            topicNew.setIsModeratePost(moderatePost);
            topicNew.setIsSticky(sticky);

            topicNew.setIcon("uiIconForumTopic uiIconForumLightGray");
            String[] canPosts = ForumUtils.splitForForum(canPost);
            String[] canViews = ForumUtils.splitForForum(canView);

            topicNew.setCanView(canViews);
            topicNew.setCanPost(canPosts);
            topicNew.setIsApproved(!hasForumMod);
            if (!ForumUtils.isEmpty(uiForm.topicId)) {
              topicNew.setId(uiForm.topicId);
              topicNew.setEditReason(editReason);
              try {
                uiForm.getForumService().saveTopic(uiForm.categoryId, uiForm.forumId, topicNew, false, false, ForumUtils.getDefaultMail());
                if (uiForm.isDetail) {
                  forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath((uiForm.categoryId + ForumUtils.SLASH + uiForm.forumId + ForumUtils.SLASH + uiForm.topicId));
                  UITopicDetail topicDetail = forumPortlet.findFirstComponentOfType(UITopicDetail.class);
                  topicDetail.setIsEditTopic(true);
                  uiForm.isDetail = false;
                }
              } catch (PathNotFoundException e) {
                forumPortlet.renderForumHome();
                forumPortlet.cancelAction();
                event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
                uiForm.isDoubleClickSubmit = false;
                warning("UITopicForm.msg.forum-deleted", false);
                return;
              }
            } else {
              topicNew.setVoteRating(0.0);
              topicNew.setUserVoteRating(new String[] {});
              try {
                String remoteAddr = ForumUtils.EMPTY_STR;
                if (forumPortlet.isEnableIPLogging()) {
                  remoteAddr = WebUIUtils.getRemoteIP();
                }
                topicNew.setRemoteAddr(remoteAddr);
                uiForm.getForumService().saveTopic(uiForm.categoryId, uiForm.forumId, topicNew, true, false, ForumUtils.getDefaultMail());
                if (userProfile.getIsAutoWatchMyTopics()) {
                  List<String> values = new ArrayList<String>();
                  values.add(userProfile.getEmail());
                  String path = uiForm.categoryId + ForumUtils.SLASH + uiForm.forumId + ForumUtils.SLASH + topicNew.getId();
                  uiForm.getForumService().addWatch(1, path, values, userName);
                }
              } catch (PathNotFoundException e) {
                forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
                UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
                categoryContainer.updateIsRender(true);
                categoryContainer.getChild(UICategories.class).setIsRenderChild(false);
                forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(Utils.FORUM_SERVICE);
                forumPortlet.cancelAction();
                event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
                warning("UITopicForm.msg.forum-deleted");
                uiForm.isDoubleClickSubmit = false;
                return;
              }
            }
            uiForm.topic = new Topic();
            forumPortlet.cancelAction();
            if (isOffend || hasForumMod) {
              if (isOffend)
                warning("MessagePost.msg.isOffend", false);
              else {
                String[] args = new String[] { "forum", "thread" };
                warning("MessageThread.msg.isModerate", args, false);
                UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
                uiForumContainer.setIsRenderChild(true);
                UITopicContainer topicContainer = uiForumContainer.getChild(UITopicContainer.class);
                topicContainer.setUpdateForum(uiForm.categoryId, uiForm.forum, 0);
                UIBreadcumbs breadcumbs = forumPortlet.getChild(UIBreadcumbs.class);
                event.getRequestContext().addUIComponentToUpdateByAjax(uiForumContainer);
                event.getRequestContext().addUIComponentToUpdateByAjax(breadcumbs);
                forumPortlet.removeCacheUserProfile();
              }
            }
            event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
          } else {
            String[] args = new String[] {  uiForm.getLabel(FIELD_MESSAGECONTENT) };
            if (k == 0) {
              args = new String[] { uiForm.getLabel(FIELD_TOPICTITLE_INPUT) };
              if (t <= 0)
                args = new String[] { uiForm.getLabel(FIELD_TOPICTITLE_INPUT) + " " + 
                                      uiForm.getLabel("and") + " " + uiForm.getLabel(FIELD_MESSAGECONTENT) };
              uiForm.isDoubleClickSubmit = false;
              warning("NameValidator.msg.ShortMessage", args);
            } else if (t <= 0) {
              uiForm.isDoubleClickSubmit = false;
              warning("NameValidator.msg.ShortMessage", args);
            }
            return;
          }
        } else {
          forumPortlet.cancelAction();
          forumPortlet.removeCacheUserProfile();
          UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class);
          topicContainer.setUpdateForum(uiForm.categoryId, uiForm.forum, 0);
          event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
          uiForm.isDoubleClickSubmit = false;
          warning("UITopicForm.msg.no-permission", false);
          return;
        }
      } catch (Exception e) {
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
        categoryContainer.updateIsRender(true);
        categoryContainer.getChild(UICategories.class).setIsRenderChild(false);
        forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(Utils.FORUM_SERVICE);
        forumPortlet.cancelAction();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        warning("UITopicForm.msg.forum-deleted", false);
      }
    }
  }

  static public class AttachmentActionListener extends EventListener<UITopicForm> {
    public void execute(Event<UITopicForm> event) throws Exception {
      UITopicForm uiForm = event.getSource();
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UIAttachFileForm attachFileForm = uiForm.openPopup(popupContainer, UIAttachFileForm.class, 500, 0);
      attachFileForm.updateIsTopicForm(true);
      attachFileForm.setMaxField(5, false);
    }
  }

  static public class RemoveAttachmentActionListener extends EventListener<UITopicForm> {
    public void execute(Event<UITopicForm> event) throws Exception {
      UITopicForm uiTopicForm = event.getSource();
      String attFileId = event.getRequestContext().getRequestParameter(OBJECTID);
      for (ForumAttachment att : uiTopicForm.attachments_) {
        if (att.getId().equals(attFileId)) {
          uiTopicForm.removeFromUploadFileList(att);
          uiTopicForm.attachments_.remove(att);
          break;
        }
      }
      uiTopicForm.refreshUploadFileList();
    }
  }

  static public class CancelActionListener extends EventListener<UITopicForm> {
    public void execute(Event<UITopicForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }

  static public class SelectTabActionListener extends EventListener<UITopicForm> {
    public void execute(Event<UITopicForm> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UITopicForm topicForm = event.getSource();
      topicForm.id = Integer.parseInt(id);
      Util.getPortalRequestContext().setResponseComplete(true);
    }
  }

}
