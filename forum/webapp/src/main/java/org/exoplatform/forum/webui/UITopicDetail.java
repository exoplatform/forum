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
package org.exoplatform.forum.webui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PathNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.forum.ForumSessionUtils;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.TimeConvertUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.user.CommonContact;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.rendering.RenderHelper;
import org.exoplatform.forum.rendering.RenderingException;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.ForumSearchResult;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Tag;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.forum.service.impl.model.PostListAccess;
import org.exoplatform.forum.webui.popup.UIMovePostForm;
import org.exoplatform.forum.webui.popup.UIMoveTopicForm;
import org.exoplatform.forum.webui.popup.UIPageListPostHidden;
import org.exoplatform.forum.webui.popup.UIPageListPostUnApprove;
import org.exoplatform.forum.webui.popup.UIPollForm;
import org.exoplatform.forum.webui.popup.UIPostForm;
import org.exoplatform.forum.webui.popup.UIPrivateMessageForm;
import org.exoplatform.forum.webui.popup.UIRatingForm;
import org.exoplatform.forum.webui.popup.UISplitTopicForm;
import org.exoplatform.forum.webui.popup.UITopicForm;
import org.exoplatform.forum.webui.popup.UIViewPost;
import org.exoplatform.forum.webui.popup.UIViewPostedByUser;
import org.exoplatform.forum.webui.popup.UIViewTopicCreatedByUser;
import org.exoplatform.forum.webui.popup.UIViewUserProfile;
import org.exoplatform.forum.webui.popup.UIWatchToolsForm;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.ecms.css.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

import static org.exoplatform.forum.ForumUtils.SLASH;


@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/UITopicDetail.gtmpl", 
    events = {
      @EventConfig(listeners = UITopicDetail.AddPostActionListener.class ),
      @EventConfig(listeners = UITopicDetail.RatingTopicActionListener.class ),
      @EventConfig(listeners = UITopicDetail.AddTagTopicActionListener.class ),
      @EventConfig(listeners = UITopicDetail.UnTagTopicActionListener.class ),
      @EventConfig(listeners = UITopicDetail.OpenTopicsTagActionListener.class ),
      @EventConfig(listeners = UITopicDetail.GoNumberPageActionListener.class ),
      @EventConfig(listeners = UITopicDetail.SearchFormActionListener.class ),
      
      @EventConfig(listeners = UITopicDetail.PrintActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.EditActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.DeleteActionListener.class,confirm="UITopicDetail.confirm.DeleteThisPost" ),  
      @EventConfig(listeners = UITopicDetail.PrivatePostActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.QuoteActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.EditTopicActionListener.class ),  //Topic Menu
      @EventConfig(listeners = UITopicDetail.PrintPageActionListener.class ),
      @EventConfig(listeners = UITopicDetail.AddPollActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetOpenTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetCloseTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetLockedTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetUnLockTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetMoveTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetStickTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetUnStickTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SplitTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetApproveTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetUnApproveTopicActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetDeleteTopicActionListener.class,confirm="UITopicDetail.confirm.DeleteThisTopic" ),  
      @EventConfig(listeners = UITopicDetail.MergePostActionListener.class ), //Post Menu 
      @EventConfig(listeners = UITopicDetail.MovePostActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetApprovePostActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetCensorPostActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetHiddenPostActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.SetUnHiddenPostActionListener.class ),  
//      @EventConfig(listeners = UITopicDetail.SetUnApproveAttachmentActionListener.class ),  
      @EventConfig(listeners = UITopicDetail.DeletePostActionListener.class),
      
      @EventConfig(listeners = UITopicDetail.QuickReplyActionListener.class),
      @EventConfig(listeners = UITopicDetail.PreviewReplyActionListener.class),
      
      @EventConfig(listeners = UITopicDetail.WatchOptionActionListener.class ),
      @EventConfig(listeners = UITopicDetail.DownloadAttachActionListener.class ),
      @EventConfig(listeners = UIForumKeepStickPageIterator.GoPageActionListener.class),
      @EventConfig(listeners = UITopicDetail.AdvancedSearchActionListener.class),
      @EventConfig(listeners = UITopicDetail.BanIPAllForumActionListener.class),
      @EventConfig(listeners = UITopicDetail.BanIPThisForumActionListener.class),
      @EventConfig(listeners = UITopicDetail.AddBookMarkActionListener.class),
      @EventConfig(listeners = UITopicDetail.RSSActionListener.class),
      @EventConfig(listeners = UITopicDetail.UnWatchActionListener.class),
      @EventConfig(listeners = UITopicDetail.AddWatchingActionListener.class)
    }
)
public class UITopicDetail extends UIForumKeepStickPageIterator {
  private static final Log           LOG                     = ExoLogger.getLogger(UITopicDetail.class);

  private String                     categoryId;

  private String                     forumId;

  private String                     topicId                 = ForumUtils.EMPTY_STR;

  private Forum                      forum;

  private Topic                      topic                   = new Topic();

  private boolean                    isEditTopic             = false;

  private String                     IdPostView              = "false";

  private String                     IdLastPost              = "false";

  private String                     userName                = " ";

  private boolean                    isModeratePost          = false;

  private boolean                    isMod                   = false;

  private boolean                    enableIPLogging         = true;

  private boolean                    isCanPost               = false;

  private boolean                    canCreateTopic;

  protected boolean                  isGetSv                 = true;

  private boolean                    isShowQuickReply        = true;

  protected boolean                  isShowRule              = true;

  private boolean                    isDoubleClickQuickReply = false;

  private String                     lastPoistIdSave         = ForumUtils.EMPTY_STR;

  private String                     isHidden                = ForumUtils.EMPTY_STR;

  private String                     isApprove               = ForumUtils.EMPTY_STR;

  private String                     isWaiting               = ForumUtils.EMPTY_STR;

  private String                     lastPostId              = ForumUtils.EMPTY_STR;

  private List<String>               listContactsGotten      = new ArrayList<String>();

  private Map<String, Integer>       pagePostRemember        = new HashMap<String, Integer>();

  private Map<String, UserProfile>   mapUserProfile          = new HashMap<String, UserProfile>();

  private Map<String, CommonContact> mapContact              = new HashMap<String, CommonContact>();

  public static final String         FIELD_MESSAGE_TEXTAREA  = "UITopicDetail.label.Message";

  public static final String         FIELD_ADD_TAG           = "AddTag";

  public static final String         SIGNATURE               = "SignatureTypeID";

  RenderHelper                       renderHelper            = new RenderHelper();
  
  private PostListAccess             postListAccess;
  
  public UITopicDetail() throws Exception {
    isDoubleClickQuickReply = false;
    if (getId() == null)
      setId("UITopicDetail");
    addUIFormInput(new UIFormStringInput(ForumUtils.GOPAGE_ID_T, null));
    addUIFormInput(new UIFormStringInput(ForumUtils.GOPAGE_ID_B, null));
    addUIFormInput(new UIFormStringInput(ForumUtils.SEARCHFORM_ID, null));
    addUIFormInput(new UIFormStringInput(FIELD_ADD_TAG, null));
    UIFormTextAreaInput textArea = new UIFormTextAreaInput(FIELD_MESSAGE_TEXTAREA, FIELD_MESSAGE_TEXTAREA, null);
    addUIFormInput(textArea);
    addChild(UIPostRules.class, null, null);
    setActions(new String[] { "QuickReply", "PreviewReply" });
    isLink = true;
  }
  
  protected void initPlaceholder() throws Exception {
    ((UIFormTextAreaInput)getChildById(FIELD_MESSAGE_TEXTAREA)).setHTMLAttribute("placeholder", WebUIUtils.getLabel(null, FIELD_MESSAGE_TEXTAREA));
  }

  protected String getCSSByFileType(String fileName, String fileType) {
    return CssClassUtils.getCSSClassByFileNameAndFileType(fileName, fileType, null);
  }
  
  public boolean isShowQuickReply() {
    return isShowQuickReply;
  }

  public String getLastPostId() {
    return lastPostId;
  }

  public void setLastPostId(String lastPost) {
    this.lastPostId = lastPost;
  }

  public String getRSSLink(String cateId) {
    PortalContainer pcontainer = PortalContainer.getInstance();
    return CommonUtils.getRSSLink("forum", pcontainer.getPortalContainerInfo().getContainerName(), cateId);
  }

  protected String getRestPath() throws Exception {
    try {
      ExoContainerContext exoContext = (ExoContainerContext) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ExoContainerContext.class);
      return SLASH + exoContext.getPortalContainerName() + SLASH + exoContext.getRestContextName();
    } catch (Exception e) {
      log.error("Can not get portal name or rest context name, exception: ", e);
    }
    return ForumUtils.EMPTY_STR;
  }

  public boolean getHasEnableIPLogging() {
    return enableIPLogging;
  }

  public boolean isIPBaned(String ip) {
    List<String> ipBaneds = forum.getBanIP();
    if (ipBaneds != null && ipBaneds.size() > 0 && ipBaneds.contains(ip))
      return true;
    return false;
  }

  public boolean isOnline(String userId) throws Exception {
    return getForumService().isOnline(userId);
  }

  private int getPagePostRemember(String topicId) {
    if (pagePostRemember.containsKey(topicId))
      return pagePostRemember.get(topicId);
    return 1;
  }

  public boolean isNotLogin() throws Exception {

    if (UserHelper.isAnonim() && !forum.getIsLock() && !topic.getIsLock())
      return true;
    return false;
  }

  public void setUpdateTopic(String categoryId, String forumId, String topicId) throws Exception {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.topicId = topicId;
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    isShowQuickReply = forumPortlet.isShowQuickReply();
    isShowRule = forumPortlet.isShowRules();
    enableIPLogging = forumPortlet.isEnableIPLogging();
    forumPortlet.updateAccessTopic(topicId);
    userName = getUserProfile().getUserId();
    cleanCheckedList();
    forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath((categoryId + SLASH + forumId + SLASH + topicId));
    this.isUseAjax = forumPortlet.isUseAjax();
    this.topic = getForumService().getTopic(categoryId, forumId, topicId, userName);
    getForumService().setViewCountTopic((categoryId + SLASH + forumId + SLASH + topicId), userName);
    setRenderInfoPorlet();
  }

  public void initInfoTopic(String categoryId, String forumId, Topic topic, int page) throws Exception {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.topicId = topic.getId();
    this.topic = topic;
    if (page > 0)
      pageSelect = page;
    else
      pageSelect = getPagePostRemember(topicId);
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    isShowQuickReply = forumPortlet.isShowQuickReply();
    isShowRule = forumPortlet.isShowRules();
    enableIPLogging = forumPortlet.isEnableIPLogging();
    cleanCheckedList();
    getForumService().setViewCountTopic((categoryId + SLASH + forumId + SLASH + topicId), userName);
    forumPortlet.updateAccessTopic(topicId);
    forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath((categoryId + SLASH + forumId + SLASH + topicId));
    this.isUseAjax = forumPortlet.isUseAjax();
    userName = getUserProfile().getUserId();
    setRenderInfoPorlet();
  }

  public void hasPoll(boolean hasPoll) throws Exception {
    this.topic.setIsPoll(hasPoll);
    if (hasPoll)
      setRenderInfoPorlet();
  }

  public void setRenderInfoPorlet() throws Exception {
    /**
     * Set permission for current user login.    
    */
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    isMod = (getUserProfile().getUserRole() == UserProfile.ADMIN) || (ForumServiceUtils.isModerator(forum.getModerators(), userName));
    if (topic != null) {
      canCreateTopic =  forumPortlet.checkForumHasAddTopic(categoryId, forumId);
      isCanPost = forumPortlet.checkForumHasAddPost(categoryId, forumId, topicId);
    }
  }

  public void setIsGetSv(boolean isGetSv) {
    this.isGetSv = isGetSv;
  }

  public boolean getCanPost() throws Exception {
    return isCanPost;
  }

  public void setUpdateForum(Forum forum) throws Exception {
    this.forum = forum;
  }

  protected boolean isCanPostReply() throws Exception {
    //
    boolean isCanReply = (forum != null && !forum.getIsClosed() && !forum.getIsLock() &&
                           topic != null && !topic.getIsClosed() && !topic.getIsLock() &&
                           getUserProfile().getUserRole() != UserProfile.GUEST && !userProfile.isDisabled());
    if(!isCanReply) {
      return false;
    }
    //
    if(isMod) {
      return true;
    }
    //1. topic is Active -> can reply
    //2. forum is Active -> can reply
    //3. topic is Waiting -> can not reply
    //4. forum is moderate topic AND topic is Approved -> can reply
    //5. forum is moderate topic AND topic is UnApproved -> can not reply
    //6. user's IP is not banned -> can reply
    //7. user is not banned -> can reply
    isCanReply = (!userProfile.getIsBanned() && !isIPBaned(getRemoteIP()) &&
                  topic.getIsActive() && topic.getIsActiveByForum() && !topic.getIsWaiting());
    //Forum moderating option a topic is active AND topic is approved) 
    //OR (Forum moderating option a topic is deactivate)  -> CAN POST
    isCanReply &= ((forum.getIsModerateTopic() && topic.getIsApproved()) || !forum.getIsModerateTopic());
    //
    if(!isCanReply) {
      return false;
    }
    try {
      List<String> listUser = new ArrayList<String>();
      listUser = ForumUtils.addArrayToList(listUser, topic.getCanPost());
      listUser = ForumUtils.addArrayToList(listUser, forum.getPoster());
      listUser = ForumUtils.addArrayToList(listUser, getForumService().getCategory(categoryId).getPoster());
      if (!listUser.isEmpty()) {
        listUser.add(topic.getOwner());
        return ForumServiceUtils.hasPermission(listUser.toArray(new String[listUser.size()]), userName);
      }
    } catch (Exception e) {
      log.warn("Check can reply topic is unsuccessfully.");
      log.debug(e.getMessage(), e);
    }
    return true;
  }

  private String getRemoteIP() throws Exception {
    if (enableIPLogging) {
      return WebUIUtils.getRemoteIP();
    }
    return ForumUtils.EMPTY_STR;
  }

  public Forum getForum() throws Exception {
    return this.forum;
  }

  public String getIdPostView() {
    if (this.IdPostView.equals(ForumUtils.VIEW_LAST_POST)) {
      this.IdPostView = "normal";
      return this.IdLastPost;
    }
    if (this.IdPostView.equals("top")) {
      this.IdPostView = "normal";
      return "top";
    }
    String temp = this.IdPostView;
    this.IdPostView = "normal";
    return temp;
  }

  public void setIdPostView(String IdPostView) {
    this.IdPostView = IdPostView;
  }

  public void setIsEditTopic(boolean isEditTopic) {
    this.isEditTopic = isEditTopic;
  }

  protected boolean isModerator() {
    return isMod;
  }

  private Topic getTopic() throws Exception {
    try {
      if (this.isEditTopic || this.topic == null) {
        this.topic = getForumService().getTopic(categoryId, forumId, topicId, UserProfile.USER_GUEST);
        this.isEditTopic = false;
      }
      return this.topic;
    } catch (Exception e) {
      log.warn("Failed to load topic: " + e.getMessage(), e);
    }
    return null;
  }

  public boolean userCanView() throws Exception {
    return getAncestorOfType(UIForumPortlet.class).checkCanView(getForumService().getCategory(categoryId), forum, topic);
  }

  public String getImageUrl(String imagePath) throws Exception {
    String url = ForumUtils.EMPTY_STR;
    try {
      url = CommonUtils.getImageUrl(imagePath);
    } catch (Exception e) {
      log.debug("Failed to get image url.", e);
    }
    return url;
  }

  public String getFileSource(ForumAttachment attachment) throws Exception {
    return ForumUtils.getFileSource(attachment);
  }

  public String getAvatarUrl(String userId) throws Exception {
    return ForumSessionUtils.getUserAvatarURL(userId, getForumService());
  }

  public CommonContact getPersonalContact(String userId) throws Exception {
    CommonContact contact;
    if (mapContact.containsKey(userId) && listContactsGotten.contains(userId)) {
      contact = mapContact.get(userId);
    } else {
      contact = ForumSessionUtils.getPersonalContact(userId);
      mapContact.put(userId, contact);
      listContactsGotten.add(userId);
    }
    return contact;
  }

  public void initPage() throws Exception {
    setListWatches();
    objectId = topicId;
    isDoubleClickQuickReply = false;
    isGetSv = true;
    listContactsGotten = new ArrayList<String>();
    try {
      isApprove = ForumUtils.EMPTY_STR;
      isHidden = ForumUtils.EMPTY_STR;
      isWaiting = ForumUtils.EMPTY_STR;
      if (!isMod){
        isHidden = "false";
        isWaiting = "false";
      }
      if (this.forum.getIsModeratePost() || this.topic.getIsModeratePost()) {
        isModeratePost = true;
        if (!isMod && !(this.topic.getOwner().equals(userName)))
          isApprove = "true";
      }
      
      this.postListAccess = (PostListAccess) getForumService().getPosts(new PostFilter(this.categoryId, this.forumId, topicId, isApprove, isHidden, isWaiting, userName));

      int pageSize = (int)getUserProfile().getMaxPostInPage();
      postListAccess.initialize(pageSize, pageSelect);
      if (IdPostView.equals("lastpost")) {
        this.pageSelect = postListAccess.getTotalPages();
      }
    } catch (Exception e) {
      log.debug("Failed to init topic page: " + e.getMessage(), e);
    }
  }

  protected List<String> getActionsEachPost(UserProfile owner, boolean isFirstPost) throws Exception {
    List<String> actions = new ArrayList<>();
    if(getUserProfile().getUserRole() < 3 ) {
      Forum forum = getForum();
      Topic topic = getTopic();
      if(forum != null && !forum.getIsLock() && !forum.getIsClosed()
              && topic != null && !topic.getIsLock() && !topic.getIsClosed()) {
        actions.add("Quote");
      }
      if(!owner.isDisabled()){
        actions.add("PrivatePost");
      }
      if (!isFirstPost && (actions.isEmpty() || isModerator() || owner.getUserId().equals(getUserProfile().getUserId()))) {
        actions.add("Delete");
        actions.add("Edit");
      }
    }
    //
    return actions;
  }

  protected boolean getIsModeratePost() {
    return this.isModeratePost;
  }

  @Override
  public List<Integer> getInfoPage() throws Exception {
    List<Integer> temp = new ArrayList<Integer>();
    try {
      temp.add(postListAccess.getPageSize());
      temp.add(postListAccess.getCurrentPage());
      temp.add(postListAccess.getSize());
      temp.add(postListAccess.getTotalPages());
    } catch (Exception e) {
      temp.add(1);
      temp.add(1);
      temp.add(1);
      temp.add(1);
    }
    return temp;
  }

  protected List<Post> getPostPageList() throws Exception {
    Post[] posts = null;
    mapUserProfile.clear();
    int pageSize = (int)getUserProfile().getMaxPostInPage();
    try {
      try {
        if (!ForumUtils.isEmpty(lastPostId)) {
          
          Long index = getForumService().getLastReadIndex((categoryId + SLASH + forumId + SLASH + topicId + SLASH + lastPostId), isApprove, isHidden, userName);
          if (index.intValue() <= pageSize)
            pageSelect = 1;
          else {
            pageSelect = (int) (index / pageSize);
            if (pageSize * pageSelect < index)
              pageSelect = pageSelect + 1;
          }
          lastPostId = ForumUtils.EMPTY_STR;
        }
      } catch (Exception e) {
        log.warn("Failed to find last read index for topic: " + e.getMessage(), e);
      }
      postListAccess.setCurrentPage(pageSelect);
      this.pageSelect = postListAccess.getCurrentPage();

      maxPage = postListAccess.getTotalPages();
      //update last post view in user profile in load method
      //more detail in JCRDataStorage#getPosts() method
      posts = postListAccess.load(pageSelect);
      this.pageSelect = postListAccess.getCurrentPage();
      
      pagePostRemember.put(topicId, pageSelect);

      List<String> userNames = new ArrayList<String>();
      for (Post post : posts) {
        if (!userNames.contains(post.getOwner()))
          userNames.add(post.getOwner());
        synchronized(this) {
          if (getUICheckBoxInput(post.getId()) != null) {
            getUICheckBoxInput(post.getId()).setChecked(false);
          } else {
            addUIFormInput(new UICheckBoxInput(post.getId(), post.getId(), false));
          }
        }
        this.IdLastPost = post.getId();
      }
      
      if (!lastPoistIdSave.equals(IdLastPost)) {
        lastPoistIdSave = IdLastPost;
        getUserProfile().addLastPostIdReadOfForum(forumId, topicId + SLASH + IdLastPost);
        userProfile.addLastPostIdReadOfTopic(topicId, IdLastPost);
        if (!UserProfile.USER_GUEST.equals(userName)) {
          getForumService().saveLastPostIdRead(userName, userProfile.getLastReadPostOfForum(), userProfile.getLastReadPostOfTopic());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to load posts page: " + e.getMessage(), e);
    }
    return posts == null ? Collections.emptyList() : Arrays.asList(posts);
  }

  public List<Tag> getTagsByTopic() throws Exception {
    List<Tag> list = new ArrayList<Tag>();
    List<String> listTagId = new ArrayList<String>();
    String[] tagIds = topic.getTagId();
    String[] temp;
    for (int i = 0; i < tagIds.length; i++) {
      temp = tagIds[i].split(":");
      if (temp[0].equals(userName)) {
        listTagId.add(temp[1]);
      }
    }
    try {
      list = getForumService().getMyTagInTopic(listTagId.toArray(new String[listTagId.size()]));
    } catch (Exception e) {
      log.warn("Failed to load user tags in topic: " + e.getMessage(), e);
    }
    return list;
  }

  private Post getPost(String postId) throws Exception {
    return getForumService().getPost(categoryId, forumId, topicId, postId);
  }

  public void setPostRules(boolean isNull) throws Exception {
    UIPostRules postRules = getChild(UIPostRules.class);
    postRules.setUserProfile(getUserProfile());
    if (!isNull) {
      if (this.forum.getIsClosed() || this.forum.getIsLock()) {
        postRules.setLock(true);
      } else {
        postRules.setCanCreateNewThread(canCreateTopic);
        /**
         * set permission for post reply
         */
        if (this.topic != null && !this.topic.getIsClosed() && !this.topic.getIsLock()) {
          postRules.setCanAddPost(getCanPost());
        } else {
          postRules.setCanAddPost(false);
        }
      }
    } else {
      postRules.setCanCreateNewThread(!isNull);
      postRules.setCanAddPost(!isNull);
    }
  }
  
  public UserProfile getUserInfo(String userName) throws Exception {
    if (!mapUserProfile.containsKey(userName)) {
      UserProfile profile;
      try {
        profile = getForumService().getQuickProfile(userName);
      } catch (Exception e) {
        profile = ForumUtils.getDeletedUserProfile(getForumService(), userName);
      }
      mapUserProfile.put(userName, profile);
    }
    return mapUserProfile.get(userName);
  }

  protected void renderPoll() throws Exception {
    UITopicDetailContainer container = this.getParent();
    container.setRederPoll(false);
    ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).addUIComponentToUpdateByAjax(container);
  }

  private void refreshPortlet() throws Exception {
    UIForumPortlet forumPortlet = getAncestorOfType(UIForumPortlet.class);
    UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
    categoryContainer.updateIsRender(true);
    forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
    ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).addUIComponentToUpdateByAjax(forumPortlet);
  }

  static public class AddPostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        topicDetail.setUpdateTopic(topic.getCategoryId(), topic.getForumId(), topic.getId());
        UIPostForm postForm = topicDetail.openPopup(UIPostForm.class, "UIAddPostContainer", 850, 520);
        postForm.setPostIds(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, topicDetail.topic);
        postForm.updatePost(ForumUtils.EMPTY_STR, false, false, null);
        postForm.setMod(topicDetail.isMod);
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class RatingTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        UIRatingForm ratingForm = topicDetail.openPopup(UIRatingForm.class, 320, 0);
        ratingForm.updateRating(topicDetail.topic);
        topicDetail.isEditTopic = true;
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class AddTagTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      UIFormStringInput stringInput = topicDetail.getUIStringInput(FIELD_ADD_TAG);
      String tagIds = stringInput.getValue();
      if (!ForumUtils.isEmpty(tagIds)) {
        try {
          String special = "\\,.?!`~/][)(;#@$%^&*<>-_+=*':}{\"";
          for (int i = 0; i < special.length(); i++) {
            char c = special.charAt(i);
            if (tagIds.indexOf(c) >= 0) {
              warning("UITopicDetail.msg.failure");
              return;
            }
          }
          while (tagIds.indexOf("  ") > 0) {
            tagIds = StringUtils.replace(tagIds, "  ", " ");
          }
          List<String> listTags = new ArrayList<String>();
          for (String string : Arrays.asList(tagIds.split(" "))) {
            if (!listTags.contains(string) && !ForumUtils.isEmpty(string)) {
              listTags.add(string);
            }
          }
          List<Tag> tags = new ArrayList<Tag>();
          Tag tag;
          for (String string : listTags) {
            tag = new Tag();
            tag.setName(string);
            tag.setId(Utils.TAG + string);
            tag.setUserTag(new String[] { topicDetail.userName });
            tags.add(tag);
          }
          try {
            topicDetail.getForumService().addTag(tags, topicDetail.userName, topicDetail.topic.getPath());
          } catch (Exception e) {
            topicDetail.log.error("Failed to add tag : ", e);
          }
          stringInput.setValue(ForumUtils.EMPTY_STR);
          topicDetail.isEditTopic = true;
          refresh();
        } catch (Exception e) {
          warning("UIForumPortlet.msg.topicEmpty", false);
          topicDetail.refreshPortlet();
        }
      } else {
        throwWarning("UITopicDetail.msg.empty-field");
      }
    }
  }

  static public class UnTagTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String tagId) throws Exception {
      topicDetail.getForumService().unTag(tagId, topicDetail.userName, topicDetail.topic.getPath());
      topicDetail.isEditTopic = true;
      refresh();
    }
  }

  static public class OpenTopicsTagActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String tagId) throws Exception {
      UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.updateIsRendered(ForumUtils.TAG);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(tagId);
      forumPortlet.getChild(UITopicsTag.class).setIdTag(tagId);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class SearchFormActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      String path = topicDetail.topic.getPath();
      UIFormStringInput formStringInput = topicDetail.getUIStringInput(ForumUtils.SEARCHFORM_ID);
      String text = formStringInput.getValue();
      if (!ForumUtils.isEmpty(text) && !ForumUtils.isEmpty(path)) {
        String special = "\\,.?!`~/][)(;#@$%^&*<>-_+=|:\"'";
        for (int i = 0; i < special.length(); i++) {
          char c = special.charAt(i);
          if (text.indexOf(c) >= 0) {
            warning("UIQuickSearchForm.msg.failure");
            return;
          }
        }
        StringBuffer type = new StringBuffer();
        if (topicDetail.isMod) {
          type.append("true,").append(Utils.POST);
        } else {
          type.append("false,").append(Utils.POST);
        }
        UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
        categoryContainer.updateIsRender(true);
        UICategories categories = categoryContainer.getChild(UICategories.class);
        categories.setIsRenderChild(true);
        List<ForumSearchResult> list = topicDetail.getForumService().getQuickSearch(text, type.toString(), path, topicDetail.getUserProfile().getUserId(), forumPortlet.getInvisibleCategories(), forumPortlet.getInvisibleForums(), null);

        UIForumListSearch listSearchEvent = categories.getChild(UIForumListSearch.class);
        listSearchEvent.setListSearchEvent(text, list, path.substring(path.indexOf(Utils.CATEGORY))+ SLASH+topicDetail.getPageSelect());
        forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL);
        formStringInput.setValue(ForumUtils.EMPTY_STR);
        topicDetail.refreshPortlet();
      } else {
        throwWarning("UIQuickSearchForm.msg.checkEmpty");
      }
    }
  }

  static public class PrintActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      //      
    }
  }

  static public class GoNumberPageActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      int idbt = Integer.parseInt(objectId);
      UIFormStringInput stringInput1 = topicDetail.getUIStringInput(ForumUtils.GOPAGE_ID_T);
      UIFormStringInput stringInput2 = topicDetail.getUIStringInput(ForumUtils.GOPAGE_ID_B);
      String numberPage = ForumUtils.EMPTY_STR;
      if (idbt == 1) {
        numberPage = stringInput1.getValue();
      } else {
        numberPage = stringInput2.getValue();
      }
      numberPage = ForumUtils.removeZeroFirstNumber(numberPage);
      stringInput1.setValue(ForumUtils.EMPTY_STR);
      stringInput2.setValue(ForumUtils.EMPTY_STR);
      if (!ForumUtils.isEmpty(numberPage)) {
        try {
          int page = Integer.parseInt(numberPage.trim());
          if (page < 0) {
            throwWarning("NameValidator.msg.Invalid-number", "go page");
          } else {
            if (page == 0) {
              page = 1;
            } else if (page > topicDetail.postListAccess.getTotalPages()) {
              page = topicDetail.postListAccess.getTotalPages();
            }
            topicDetail.pageSelect = page;
            refresh();
          }
        } catch (NumberFormatException e) {
          throwWarning("NameValidator.msg.Invalid-number", "go page");
        }
      }
    }
  }

  static public class EditActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String postId) throws Exception {
      Post post = topicDetail.getPost(postId);
      if (post != null) {
        UIPostForm postForm = topicDetail.openPopup(UIPostForm.class, "UIEditPostContainer", 850, 545);
        postForm.setPostIds(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, topicDetail.topic);
        postForm.updatePost(postId, false, false, post);
        postForm.setMod(topicDetail.isMod);
      } else {
        throwWarning("UIPostForm.msg.canNotEdit");
      }
    }
  }

  static public class DeleteActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String postId) throws Exception {
      try {
        topicDetail.getForumService().removePost(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, postId);
        topicDetail.IdPostView = "top";
      } catch (Exception e) {
        topicDetail.log.warn("Failed to delete topic: " + e.getMessage(), e);
      }
      refresh();
    }
  }

  static public class QuoteActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String postId) throws Exception {
      Post post = topicDetail.getPost(postId);
      if (post != null) {
        UIPostForm postForm = topicDetail.openPopup(UIPostForm.class, "UIQuoteContainer", 850, 520);
        postForm.setPostIds(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, topicDetail.topic);
        postForm.updatePost(postId, true, (post.getUserPrivate() != null && post.getUserPrivate().length > 1), post);
        postForm.setMod(topicDetail.isMod);
      } else {
        throwWarning("UIPostForm.msg.isParentDelete");
      }
    }
  }

  static public class PrivatePostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String postId) throws Exception {
      Post post = topicDetail.getPost(postId);
      if (post != null) {
        UIPostForm postForm = topicDetail.openPopup(UIPostForm.class, "UIPrivatePostContainer", 900, 520);
        postForm.setPostIds(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, topicDetail.topic);
        postForm.updatePost(postId, false, true, post);
        postForm.setMod(topicDetail.isMod);
      } else {
        throwWarning("UIPostForm.msg.isParentDelete");
      }
    }
  }

  // -------------------------------- Topic Menu -------------------------------------------//
  static public class EditTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        Forum forum = topicDetail.getForumService().getForum(topic.getCategoryId(),topic.getForumId());
        UITopicForm topicForm = openPopup(forumPortlet, UITopicForm.class, "UIEditTopicContainer", 900, 545);
        topicForm.setTopicIds(topic.getCategoryId(), topic.getForumId(), forum);
        topicForm.setUpdateTopic(topic, true);
        topicForm.setMod(topic.getIsModeratePost());
        String spaceGroupId = forumPortlet.getSpaceGroupId();
        if(Utils.CATEGORY_SPACE_ID_PREFIX.equals(topic.getCategoryId()) && CommonUtils.isEmpty(spaceGroupId)) {
          spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + topic.getForumId().replace(Utils.FORUM_SPACE_ID_PREFIX, "");
        }
        topicForm.setSpaceGroupId(spaceGroupId);
        topicForm.setIsDetail(true);
        topicDetail.isEditTopic = true;
      } catch (Exception e) {
        topicDetail.log.warn("Error while editing topic: " + e.getMessage(), e);
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class PrintPageActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      //      
    }
  }

  static public class AddPollActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        UIPollForm pollForm = topicDetail.openPopup(UIPollForm.class, 655, 455);
        pollForm.setTopicPath(topic.getPath());
      } catch (Exception e) {
        topicDetail.log.debug("Failed to open UIPollForm for add new poll.", e);
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetOpenTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (topic.getIsClosed()) {
          topic.setIsClosed(false);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.CLOSE);
          topicDetail.isEditTopic = true;
          topicDetail.setRenderInfoPorlet();
          event.getRequestContext().addUIComponentToUpdateByAjax(topicDetail.getParent());
        } else {
          throwWarning("UITopicContainer.sms.Open", topic.getTopicName());
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetCloseTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (!topic.getIsClosed()) {
          topic.setIsClosed(true);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.CLOSE);
          topicDetail.isEditTopic = true;
          topicDetail.setRenderInfoPorlet();
          event.getRequestContext().addUIComponentToUpdateByAjax(topicDetail.getParent());
        } else {
          warning("UITopicContainer.sms.Close", topic.getTopicName(), false);
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetLockedTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (!topic.getIsLock()) {
          topic.setIsLock(true);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.LOCK);
          topicDetail.isEditTopic = true;
          topicDetail.setRenderInfoPorlet();
          event.getRequestContext().addUIComponentToUpdateByAjax(topicDetail.getParent());
        } else {
          warning("UITopicContainer.sms.Locked", topic.getTopicName(), false);
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetUnLockTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (topic.getIsLock()) {
          topic.setIsLock(false);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.LOCK);
          topicDetail.isEditTopic = true;
          topicDetail.setRenderInfoPorlet();
          event.getRequestContext().addUIComponentToUpdateByAjax(topicDetail.getParent());
        } else {
          warning("UITopicContainer.sms.UnLock", topic.getTopicName(), false);
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetMoveTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        List<Topic> topics = new ArrayList<Topic>();
        topics.add(topic);
        topicDetail.isEditTopic = true;
        UIMoveTopicForm moveTopicForm = topicDetail.openPopup(UIMoveTopicForm.class, 400, 420);
        moveTopicForm.updateTopic(topic.getForumId(), topics, true);
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetStickTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (!topic.getIsSticky()) {
          topic.setIsSticky(true);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.STICKY);
          topicDetail.isEditTopic = true;
          refresh();
        } else {
          warning("UITopicContainer.sms.Stick", topic.getTopicName(), false);
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetUnStickTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (topic.getIsSticky()) {
          topic.setIsSticky(false);
          List<Topic> topics = new ArrayList<Topic>();
          topics.add(topic);
          topicDetail.getForumService().modifyTopic(topics, Utils.STICKY);
          topicDetail.isEditTopic = true;
          refresh();
        } else {
          warning("UITopicContainer.sms.UnStick", topic.getTopicName(), false);
        }
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SplitTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        JCRPageList pageList = topicDetail.getForumService().getPostForSplitTopic(objectId);
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        if (pageList.getAvailable() > 0) {
          UISplitTopicForm splitTopicForm = topicDetail.openPopup(UISplitTopicForm.class, 700, 400);
          splitTopicForm.setPageListPost(pageList);
          splitTopicForm.setTopic(topic);
        } else {
          warning("UITopicContainer.sms.NotSplit");
        }
      } catch (Exception e) {
        topicDetail.log.warn("Failed to split topic: " + e.getMessage(), e);
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetApproveTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        topic.setIsApproved(true);
        topic.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, topic.getId(), false));
        List<Topic> topics = new ArrayList<Topic>();
        topics.add(topic);
        topicDetail.getForumService().modifyTopic(topics, Utils.APPROVE);
        topicDetail.isEditTopic = true;
        refresh();
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetUnApproveTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        topic.setIsApproved(false);
        topic.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, topic.getId(), false));
        List<Topic> topics = new ArrayList<Topic>();
        topics.add(topic);
        topicDetail.getForumService().modifyTopic(topics, Utils.APPROVE);
        topicDetail.isEditTopic = true;
        refresh();
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class SetDeleteTopicActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      try {
        Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
        Forum forum = topicDetail.getForumService().getForum(topic.getCategoryId(),topic.getForumId());
        topicDetail.getForumService().removeTopic(topic.getCategoryId(), topic.getForumId(), topic.getId());
        UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
        UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
        uiForumContainer.setIsRenderChild(true);
        UITopicContainer topicContainer = uiForumContainer.getChild(UITopicContainer.class);
        topicContainer.setUpdateForum(topic.getCategoryId(), forum, 0);
        UIBreadcumbs breadcumbs = forumPortlet.getChild(UIBreadcumbs.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForumContainer);
        event.getRequestContext().addUIComponentToUpdateByAjax(breadcumbs);
        forumPortlet.removeCacheUserProfile();
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  // --------------------------------- Post Menu --------------------------------------//
  static public class MergePostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
    }
  }

  static public class DownloadAttachActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      UITopicDetail uiTopicDetail = event.getSource();
      String object =event.getRequestContext().getRequestParameter(OBJECTID);

      if (StringUtils.isNotBlank(object) && object.split("-").length == 2 ){
        String attId = object.split("-")[0];
        String postId = object.split("-")[1];
        ForumAttachment attach = uiTopicDetail.getForumAttachmentById(uiTopicDetail.getPost(postId).getAttachments(), attId);
        if (attach != null && attach.getMimeType() != null ) {
          String mimeType = attach.getMimeType().substring(attach.getMimeType().indexOf("/") + 1);
          DownloadResource dresource = new InputStreamDownloadResource(attach.getInputStream(), mimeType);
          DownloadService dservice = (DownloadService) uiTopicDetail.getApplicationComponent(DownloadService.class);
          dresource.setDownloadName(attach.getName());
          String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource));
          event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
          event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicDetail);
      }
      } else {
        LOG.warn("ObjectID does not have correct form, expected \"topicId-postId\", got: " + object);
    }
    }
  }

  static public class MovePostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      List<String> postIds = topicDetail.getIdSelected();
      List<Post> posts = new ArrayList<Post>();
      for (String postId : postIds) {
        posts.add(topicDetail.getPost(postId));
      }
      if (posts.size() > 0) {
        UIMovePostForm movePostForm = topicDetail.openPopup(UIMovePostForm.class, 400, 430);
        movePostForm.updatePost(topicDetail.topicId, posts);
      } else {
        throwWarning("UITopicDetail.msg.notCheckPost");
      }
    }
  }

  static public class SetApprovePostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      List<String> postIds = topicDetail.getIdSelected();
      List<Post> posts = new ArrayList<Post>();
      for (String postId : postIds) {
        posts.add(topicDetail.getPost(postId));
      }
      if (posts.isEmpty()) {
        UIPageListPostUnApprove postUnApprove = topicDetail.openPopup(UIPageListPostUnApprove.class, "PageListPostUnApprove", 500, 360);
        postUnApprove.setUpdateContainer(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, true);
      } else {
        int count = 0;
        while (count < posts.size()) {
          if (!posts.get(count).getIsApproved()) {
            Post p = posts.get(count);
            p.setIsApproved(true);
            p.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, p.getTopicId(), false));
            count++;
          } else {
            posts.remove(count);
          }
        }
        if (posts.size() > 0) {
          try {
            topicDetail.getForumService().modifyPost(posts, Utils.APPROVE);
          } catch (Exception e) {
            topicDetail.log.warn("Failed to modify: " + e.getMessage(), e);
          }
          refresh();
        }
      }
    }
  }
  
  static public class SetCensorPostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      List<String> postIds = topicDetail.getIdSelected();
      List<Post> posts = new ArrayList<Post>();
      for (String postId : postIds) {
        posts.add(topicDetail.getPost(postId));
      }
      if (posts.isEmpty()) {
        UIPageListPostUnApprove postUnApprove = topicDetail.openPopup(UIPageListPostUnApprove.class, "PageListPostCensor", 500, 360);
        postUnApprove.setUpdateContainer(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, false);
      } else {
        int count = 0;
        while (count < posts.size()) {
          if (posts.get(count).getIsWaiting()) {
            Post p = posts.get(count);
            p.setIsWaiting(false);
            p.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, p.getTopicId(), false));
            count++;
          } else {
            posts.remove(count);
          }
        }
        if (posts.size() > 0) {
          try {
            topicDetail.getForumService().modifyPost(posts, Utils.WAITING);
          } catch (Exception e) {
            topicDetail.log.warn("Failed to modify: " + e.getMessage(), e);
          }
          refresh();
        }
      }
    }
  }

  static public class SetHiddenPostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      List<String> postIds = topicDetail.getIdSelected();
      if (postIds == null || postIds.isEmpty()) {
        throwWarning("UITopicDetail.msg.notCheckPost");
      }
      List<Post> posts = new ArrayList<Post>();
      String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, "topicId", false);
      for (String postId : postIds) {
        Post post = topicDetail.getPost(postId);
        if (post != null && !post.getIsHidden()) {
          post.setIsHidden(true);
          post.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, post.getTopicId(), false));
          posts.add(post);
        }
      }
      try {
        topicDetail.getForumService().modifyPost(posts, Utils.HIDDEN);
      } catch (Exception e) {
        topicDetail.log.warn("Failed to modify post: " + e.getMessage(), e);
      }
      refresh();
    }
  }

  static public class SetUnHiddenPostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      List<String> postIds = topicDetail.getIdSelected();
      List<Post> posts = new ArrayList<Post>();
      for (String postId : postIds) {
        Post post = topicDetail.getPost(postId);
        if (post != null) {
          posts.add(post);
        }
      }
      if (posts.isEmpty()) {
        UIPageListPostHidden listPostHidden = topicDetail.openPopup(UIPageListPostHidden.class, 500, 360);
        listPostHidden.setUpdateContainer(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId);
      } else {
        int count = 0;
        while (count < posts.size()) {
          if (posts.get(count).getIsHidden()) {
            Post p = posts.get(count);
            p.setIsHidden(false);
            p.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, p.getTopicId(), false));
            count++;
          } else {
            posts.remove(count);
          }
        }
        if (posts.size() > 0) {
          try {
            topicDetail.getForumService().modifyPost(posts, Utils.HIDDEN);
          } catch (Exception e) {
            topicDetail.log.warn("Failed to modify post: " + e.getMessage(), e);
          }
          refresh();
        }
      }
    }
  }

  static public class SetUnApproveAttachmentActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
    }
  }

  static public class DeletePostActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      List<String> postIds = topicDetail.getIdSelected();
      List<Post> posts = new ArrayList<Post>();
      for (String postId : postIds) {
        posts.add(topicDetail.getPost(postId));
      }
      for (Post post : posts) {
        try {
          topicDetail.getForumService().removePost(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, post.getId());
        } catch (Exception e) {
          topicDetail.log.warn("Failed to remove post: " + e.getMessage(), e);
        }
        refresh();
      }
    }
  }

  static public class ViewPublicUserInfoActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String userId) throws Exception {
      UIViewUserProfile viewUserProfile = topicDetail.openPopup(UIViewUserProfile.class, 670, 400);
      UserProfile selectProfile = topicDetail.getUserInfo(userId);
      try {
        selectProfile = topicDetail.getForumService().getUserInformations(selectProfile);
      } catch (Exception e) {
        LOG.warn("Failed in getting user informations.", e);
      }
      viewUserProfile.setUserProfileViewer(selectProfile);
    }
  }

  static public class PrivateMessageActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String userId) throws Exception {
      if (topicDetail.getUserProfile().getIsBanned()) {
        throwWarning("UITopicDetail.msg.userIsBannedCanNotSendMail");
      }
      int t = userId.indexOf(Utils.DELETED);
      if (t > 0) {
        String[] args = new String[] { userId.substring(0, t) };
        throw new MessageException(new ApplicationMessage("UITopicDetail.msg.userIsDeleted", args, ApplicationMessage.WARNING));
      }
      UIPrivateMessageForm messageForm = topicDetail.openPopup(UIPrivateMessageForm.class, 720, 550);
      messageForm.setFullMessage(false);
      messageForm.setUserProfile(topicDetail.userProfile);
      messageForm.setSendtoField(userId);
    }
  }

  static public class ViewPostedByUserActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      UIViewPostedByUser viewPostedByUser = topicDetail.openPopup(UIViewPostedByUser.class, 760, 370);
      viewPostedByUser.setUserProfile(userId);
    }
  }

  static public class ViewThreadByUserActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String userId) throws Exception {
      UIViewTopicCreatedByUser topicCreatedByUser = topicDetail.openPopup(UIViewTopicCreatedByUser.class, 760, 450);
      topicCreatedByUser.setUserId(userId);
    }
  }

  private String getTitle(String title, WebuiRequestContext context) {
    String strRe = context.getApplicationResourceBundle().getString("UIPostForm.label.ReUser")+": ";
    while (title.indexOf(strRe.trim()) == 0) {
      title = title.replaceFirst(strRe.trim(), ForumUtils.EMPTY_STR).trim();
    }
    return strRe + title;
  }
  
  static public class QuickReplyActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      if (topicDetail.isDoubleClickQuickReply)
        return;
      topicDetail.isEditTopic = true;
      Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
      topicDetail.setUpdateTopic(topic.getCategoryId(), topic.getForumId(), topic.getId());
      UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
      if(topic == null) {
        warning("UIPostForm.msg.isParentDelete", false);
        forumPortlet.renderForumHome();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        return;
      }
      topicDetail.isDoubleClickQuickReply = true;
      try {
        UIFormTextAreaInput textAreaInput = topicDetail.getUIFormTextAreaInput(FIELD_MESSAGE_TEXTAREA);
        String message = ForumUtils.EMPTY_STR;
        try {
          message = textAreaInput.getValue();
        } catch (Exception e) {
          topicDetail.log.warn("Failed read quick reply: " + e.getMessage(), e);
        }
        String checksms = message;
        if (message != null && message.trim().length() > 0) {
          if (forumPortlet.checkForumHasAddPost(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId)) {
            boolean isOffend = false;
            boolean hasTopicMod = false;
            if (!topicDetail.isMod) {
              String[] censoredKeyword = ForumUtils.getCensoredKeyword(topicDetail.getForumService());
              checksms = checksms.toLowerCase().trim();
              for (String string : censoredKeyword) {
                if (checksms.indexOf(string.trim()) >= 0) {
                  isOffend = true;
                  break;
                }
              }
              if (topicDetail.topic != null)
                hasTopicMod = topicDetail.topic.getIsModeratePost();
            }
            message = TransformHTML.enCodeHTMLContent(message);


            String userName = topicDetail.getUserProfile().getUserId();
            Post post = new Post();
            post.setName(topicDetail.getTitle(topic.getTopicName(), event.getRequestContext()));
            post.setMessage(message);
            post.setOwner(userName);
            post.setRemoteAddr(topicDetail.getRemoteIP());
            post.setIcon(topic.getIcon());
            post.setIsWaiting(isOffend);
            post.setIsApproved(!hasTopicMod);
            // set link
            String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, topicDetail.topicId, false) + SLASH + post.getId();
            //
            post.setLink(link);
            MessageBuilder messageBuilder = ForumUtils.getDefaultMail();
            messageBuilder.setLink(link);
            try {
              topicDetail.getForumService().savePost(topicDetail.categoryId, topicDetail.forumId, topicDetail.topicId, post, true, messageBuilder);
              long postCount = topicDetail.getUserInfo(userName).getTotalPost() + 1;
              topicDetail.getUserInfo(userName).setTotalPost(postCount);
              topicDetail.getUserInfo(userName).setLastPostDate(CommonUtils.getGreenwichMeanTime().getTime());
              topicDetail.getForumService().updateTopicAccess(forumPortlet.getUserProfile().getUserId(), topic.getId());
              forumPortlet.getUserProfile().setLastTimeAccessTopic(topic.getId(), CommonUtils.getGreenwichMeanTime().getTimeInMillis());
              if (topicDetail.userProfile.getIsAutoWatchTopicIPost()) {
                List<String> values = new ArrayList<String>();
                values.add(topicDetail.userProfile.getEmail());
                String path = topicDetail.categoryId + SLASH + topicDetail.forumId + SLASH + topicDetail.topicId;
                topicDetail.getForumService().addWatch(1, path, values, topicDetail.userProfile.getUserId());
              }
            } catch (PathNotFoundException e) {
              throwWarning("UIPostForm.msg.isParentDelete");
            } catch (Exception e) {
              topicDetail.log.warn("Failed to save post: " + e.getMessage(), e);
            }
            textAreaInput.setValue(ForumUtils.EMPTY_STR);
            if (isOffend || hasTopicMod) {
              if (isOffend) {
                warning("MessagePost.msg.isOffend", false);
              } else {
                warning("MessagePost.msg.isModerate", false);
              }
              topicDetail.IdPostView = "normal";
            } else {
              topicDetail.IdPostView = ForumUtils.VIEW_LAST_POST;
            }
          } else {
            topicDetail.topic = null;
            topicDetail.getTopic();
            topicDetail.setRenderInfoPorlet();
            forumPortlet.removeCacheUserProfile();
            warning("UIPostForm.msg.no-permission", false);
          }
          refresh();
        } else {
          warning("MessagePost.msg.message-empty", new String[]{getLabel(FIELD_MESSAGE_TEXTAREA)}, false);
          topicDetail.isDoubleClickQuickReply = false;
        }
      } catch (Exception e) {
        warning("UIPostForm.msg.isParentDelete", false);
        forumPortlet.renderForumHome();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      }
    }
  }

  static public class PreviewReplyActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      String message = topicDetail.getUIFormTextAreaInput(FIELD_MESSAGE_TEXTAREA).getValue();
      String checksms = (message);
      if (checksms != null && message.trim().length() > 0) {
        message = TransformHTML.enCodeHTMLContent(message);
        String userName = topicDetail.getUserProfile().getUserId();
        Topic topic = topicDetail.topic;
        Post post = new Post();
        post.setName(topicDetail.getTitle(topic.getTopicName(), event.getRequestContext()));
        post.setMessage(message);
        post.setOwner(userName);
        post.setRemoteAddr(ForumUtils.EMPTY_STR);
        post.setIcon(topic.getIcon());
        post.setIsApproved(false);
        post.setCreatedDate(new Date());

        UIViewPost viewPost = topicDetail.openPopup(UIViewPost.class, 670, 0);

        viewPost.setPostView(post);
        viewPost.setViewUserInfo(false);
        viewPost.setActionForm(new String[] { "Close" });
      } else {
        warning("MessagePost.msg.message-empty", new String[] { getLabel(FIELD_MESSAGE_TEXTAREA) });
      }
    }
  }

  static public class WatchOptionActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
      topicDetail.isEditTopic = true;
      Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
      UIWatchToolsForm watchToolsForm = openPopup(forumPortlet, UIWatchToolsForm.class, 500, 365);
      watchToolsForm.setPath(topic.getPath());
      watchToolsForm.setEmails(topic.getEmailNotification());
      watchToolsForm.setIsTopic(true);
    }
  }

  static public class AdvancedSearchActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      UIForumPortlet forumPortlet = topicDetail.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.updateIsRendered(ForumUtils.FIELD_SEARCHFORUM_LABEL);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL);
      UISearchForm searchForm = forumPortlet.getChild(UISearchForm.class);
      searchForm.setPath(topicDetail.topic.getPath());
      searchForm.setSelectType(Utils.POST);
      searchForm.setSearchOptionsObjectType(Utils.POST);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class BanIPAllForumActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String ip) throws Exception {
      if (!topicDetail.getForumService().addBanIP(ip)) {
        warning("UIBanIPForumManagerForm.sms.ipBanFalse", ip);
        return;
      }
      refresh();
    }
  }

  static public class BanIPThisForumActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String ip) throws Exception {
      List<String> listIp = topicDetail.forum.getBanIP();
      if (listIp == null || listIp.size() == 0)
        listIp = new ArrayList<String>();
      listIp.add(ip);
      topicDetail.forum.setBanIP(listIp);
      if (!topicDetail.getForumService().addBanIPForum(ip, (topicDetail.categoryId + SLASH + topicDetail.forumId))) {
        warning("UIBanIPForumManagerForm.sms.ipBanFalse", ip);
        return;
      }
      refresh();
    }
  }

  static public class AddBookMarkActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {

      try {
        Topic topic = topicDetail.getTopic();
        StringBuffer buffer = new StringBuffer();
        buffer.append("uiIconForumTopic//").append(topic.getTopicName()).append("//").append(topic.getId());
        String userName = topicDetail.getUserProfile().getUserId();
        topicDetail.getForumService().saveUserBookmark(userName, buffer.toString(), true);
      } catch (Exception e) {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class AddWatchingActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
      if (topic != null) {
        StringBuffer buffer = new StringBuffer().append(topic.getCategoryId()).append(SLASH)
                              .append(topic.getForumId()).append(SLASH).append(topic.getId());
        if(topicDetail.addWatch(buffer.toString())) {
          topicDetail.isEditTopic = true;
          refresh();
        }
      } else {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class UnWatchActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String objectId) throws Exception {
      Topic topic = topicDetail.getForumService().getTopicByPath(objectId,false);
      if (topic != null) {
        topicDetail.isEditTopic = true;
        StringBuffer buffer = new StringBuffer().append(topic.getCategoryId()).append(SLASH)
                              .append(topic.getForumId()).append(SLASH).append(topic.getId());
        if(topicDetail.unWatch(buffer.toString())) {
          topicDetail.isEditTopic = true;
          refresh();
        }
      } else {
        warning("UIForumPortlet.msg.topicEmpty", false);
        topicDetail.refreshPortlet();
      }
    }
  }

  static public class RSSActionListener extends BaseEventListener<UITopicDetail> {
    public void onEvent(Event<UITopicDetail> event, UITopicDetail topicDetail, final String topicId) throws Exception {
      if (!topicDetail.getUserProfile().getUserId().equals(UserProfile.USER_GUEST)) {
        topicDetail.getForumService().addWatch(-1, topicId, null, topicDetail.userName);
      }
    }
  }

  public String renderPost(Post post) throws RenderingException {
    if (SIGNATURE.equals(post.getId())) {
      post.setMessage(TransformHTML.enCodeViewSignature(post.getMessage()));
    }
    return renderHelper.renderPost(post);
  }

  protected String getLastEditedBy(String userId, Date modifiedDate) throws Exception {
    UserProfile userEditByInfo = getUserInfo(userId);
    String editByScreeName = userEditByInfo.getScreenName();
    //
    StringBuilder builder = new StringBuilder("<div class=\"dropdown uiUserInfo\">");
    builder.append("<a href=\"javascript:void(0);\" class=\"txtEditBy\">").append(editByScreeName).append("</a>")
           .append(getMenuUser(userEditByInfo))
           .append("</div>");
    
    if (TimeConvertUtils.getGreenwichMeanTime().getTimeInMillis() - modifiedDate.getTime() > 60000) {
      String longDateFormat = getUserProfile().getLongDateFormat() + ", " + userProfile.getTimeFormat();
      long setTime = (long) (userProfile.getTimeZone() * 3600000);
      String editDate = TimeConvertUtils.convertXTimeAgo(modifiedDate, longDateFormat, setTime);
      String editByLabel = WebUIUtils.getLabel(getId(), "LastEditedOnDate");
      // if format of last edition date is X time ago
      long day = 24 * 60 * 60 * 1000;
      if ((TimeConvertUtils.getGreenwichMeanTime().getTimeInMillis() - modifiedDate.getTime()) < (31l * day)) {
        editByLabel = WebUIUtils.getLabel(getId(), "LastEditedDateAgo");
      }
      return editByLabel.replace("{0}", builder.toString()).replace("{1}", editDate);
    }
    //
    String editByLabel = WebUIUtils.getLabel(getId(), "LastEditedJustNow");
    return editByLabel.replace("{0}", builder.toString());
  }
  
  protected String getMenuUser(UserProfile userInfo) throws Exception {
    UIForumPortlet forumPortlet = getAncestorOfType(UIForumPortlet.class);
    StringBuilder builder = new StringBuilder("<ul class=\"dropdown-menu uiUserMenuInfo dropdownArrowTop\">");
    //
    String[] menuViewInfos = ForumUtils.getUserActionsMenu(getUserProfile().getUserRole(), userInfo.getUserId());
    //
    for (int i = 0; i < menuViewInfos.length; i++) {
      String viewAction = menuViewInfos[i];
      String action = forumPortlet.getPortletLink(viewAction, userInfo.getUserId());
      String itemLabelView = WebUIUtils.getLabel(null, "UITopicDetail.action." + viewAction).replace("{0}", userInfo.getScreenName());

      builder.append("<li onclick=\"").append(action).append("\">")
             .append("  <a href=\"javaScript:void(0)\">").append(itemLabelView).append("</a>")
             .append("</li>");
    }
    builder.append("</ul>");
    return builder.toString();
  }

  private ForumAttachment getForumAttachmentById(List<ForumAttachment> list, String id){
    for (ForumAttachment attach : list) {
      if (attach.getId().equals(id)) {
        return attach;
      }
    }
    return null;
  }
}
