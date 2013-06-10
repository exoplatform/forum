package org.exoplatform.forum.webui.popup;

import org.exoplatform.forum.ForumSessionUtils;
import org.exoplatform.forum.common.webui.AbstractPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputSet;

@ComponentConfig(
     template = "app:/templates/forum/webui/popup/UIAvatarContainer.gtmpl",
     events = {
       @EventConfig(listeners = UIAvatarContainer.SetDeaultAvatarActionListener.class,  confirm="UIModeratorManagementForm.msg.setDefaultAvartar"),
       @EventConfig(listeners = UIAvatarContainer.UpdateAvatarActionListener.class)
     }
 )
public class UIAvatarContainer extends UIFormInputSet {
  private ForumService forumService;
  private UserProfile userProfile;
  private String userAvartarUrl;
  
  public UIAvatarContainer() {
  }
  
  public UserProfile getUserProfile() {
    return userProfile;
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }
  
  public ForumService getForumService() {
    return forumService;
  }

  public void setForumService(ForumService forumService) {
    this.forumService = forumService;
  }

  protected String getAvatarUrl() {
    userAvartarUrl = ForumSessionUtils.getUserAvatarURL(userProfile.getUserId(), getForumService());
    return userAvartarUrl;
  }

  protected boolean isDefaultAvatar() {
    return ForumSessionUtils.DEFAULT_AVATAR.equals(userAvartarUrl);
  }

  public static class UpdateAvatarActionListener extends EventListener<UIAvatarContainer> {
    public void execute(Event<UIAvatarContainer> event) throws Exception{
      UIAvatarContainer uiAvatar = event.getSource();
      UIPopupContainer popupContainer = uiAvatar.getAncestorOfType(UIPopupContainer.class);
      
      AbstractPopupAction popupAction = popupContainer.getChild(AbstractPopupAction.class);
      popupAction.getChild(UIPopupWindow.class).setId("UIForumChildPopupWindow");
      UIAttachFileForm attachFileForm = popupAction.createUIComponent(UIAttachFileForm.class, null, null);
      attachFileForm.setRendered(true);
      popupAction.activate(attachFileForm, 500, 0);

      attachFileForm.setId("UploadAvatar");
      attachFileForm.setChangeAvatarOfUser(uiAvatar.getUserProfile().getUserId());
      attachFileForm.updateIsTopicForm(false);
      attachFileForm.setIsChangeAvatar(true);
      attachFileForm.setMaxField(1, true);
      
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class SetDeaultAvatarActionListener extends EventListener<UIAvatarContainer> {
    public void execute(Event<UIAvatarContainer> event) throws Exception{
      UIAvatarContainer uiAvatar = event.getSource();
      if (uiAvatar.userAvartarUrl.equals(ForumSessionUtils.DEFAULT_AVATAR))
        return;
      
      uiAvatar.getForumService().setDefaultAvatar(uiAvatar.getUserProfile().getUserId());
      uiAvatar.userAvartarUrl = ForumSessionUtils.DEFAULT_AVATAR;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAvatar);
    }
  }

}
