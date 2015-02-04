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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAdministration;
import org.exoplatform.forum.service.ForumSearchResult;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.forum.service.impl.model.TopicListAccess;
import org.exoplatform.forum.webui.popup.UIBanIPForumManagerForm;
import org.exoplatform.forum.webui.popup.UIExportForm;
import org.exoplatform.forum.webui.popup.UIForumForm;
import org.exoplatform.forum.webui.popup.UIMergeTopicForm;
import org.exoplatform.forum.webui.popup.UIMoveForumForm;
import org.exoplatform.forum.webui.popup.UIMoveTopicForm;
import org.exoplatform.forum.webui.popup.UIPageListTopicUnApprove;
import org.exoplatform.forum.webui.popup.UIPollForm;
import org.exoplatform.forum.webui.popup.UITopicForm;
import org.exoplatform.forum.webui.popup.UIWatchToolsForm;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "app:/templates/forum/webui/UITopicContainer.gtmpl", 
  events = {
    @EventConfig(listeners = UITopicContainer.SearchFormActionListener.class ),  
    @EventConfig(listeners = UITopicContainer.GoNumberPageActionListener.class ),  
    @EventConfig(listeners = UITopicContainer.AddTopicActionListener.class ),  
    @EventConfig(listeners = UITopicContainer.AddPollActionListener.class ),  
    @EventConfig(listeners = UITopicContainer.OpenTopicActionListener.class ),
                                                                                  // Forum
    @EventConfig(listeners = UITopicContainer.EditForumActionListener.class ),  
    @EventConfig(listeners = UITopicContainer.SetLockedForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetUnLockForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetOpenForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetCloseForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.MoveForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.RemoveForumActionListener.class),// Menu
                                                                                                                              // Topic
    @EventConfig(listeners = UITopicContainer.WatchOptionActionListener.class),
    
    @EventConfig(listeners = UITopicContainer.EditTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetOpenTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetCloseTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetLockedTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetUnLockTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetStickTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetUnStickTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetMoveTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.MergeTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetDeleteTopicActionListener.class),
    @EventConfig(listeners = UITopicContainer.SetUnWaitingActionListener.class),
    @EventConfig(listeners = UITopicContainer.ApproveTopicsActionListener.class ),
    @EventConfig(listeners = UITopicContainer.ActivateTopicsActionListener.class ),
    
    @EventConfig(listeners = UITopicContainer.SetOrderByActionListener.class),
    @EventConfig(listeners = UITopicContainer.AddWatchingActionListener.class),
    @EventConfig(listeners = UITopicContainer.UnWatchActionListener.class),
    @EventConfig(listeners = UITopicContainer.AddBookMarkActionListener.class),
    @EventConfig(listeners = UITopicContainer.ExportForumActionListener.class),
    @EventConfig(listeners = UITopicContainer.AdvancedSearchActionListener.class),
    @EventConfig(listeners = UITopicContainer.BanIpForumToolsActionListener.class),
    @EventConfig(listeners = UITopicContainer.RSSActionListener.class),
    @EventConfig(listeners = UIForumKeepStickPageIterator.GoPageActionListener.class)
  }
)
public class UITopicContainer extends UIForumKeepStickPageIterator {
  private String                 forumId           = ForumUtils.EMPTY_STR;

  private String                 categoryId        = ForumUtils.EMPTY_STR;

  private Forum                  forum;

  private List<Topic>            topicList;

  private List<String>           moderators;

  private boolean                isModerator       = false;

  private boolean                canAddNewThread   = true;

  private String                 strOrderBy        = ForumUtils.EMPTY_STR;

  private boolean                isLogin           = false;

  private boolean                isNull            = false;

  private boolean                enableIPLogging   = true;

  private boolean                isShowActive      = false;

  public String                   openTopicId      = ForumUtils.EMPTY_STR;

  private Map<String, Integer>   pageTopicRemember = new HashMap<String, Integer>();

  private TopicListAccess topicListAccess;

  public UITopicContainer() throws Exception {
    addUIFormInput(new UIFormStringInput(ForumUtils.GOPAGE_ID_T, null));
    addUIFormInput(new UIFormStringInput(ForumUtils.GOPAGE_ID_B, null));
    addUIFormInput(new UIFormStringInput(ForumUtils.SEARCHFORM_ID, null));
    if (!UserHelper.isAnonim())
      isLogin = true;
    isLink = true;
    
    setSubmitAction("return false;");
  }
  
  public boolean isNull() {
    return isNull;
  }

  public void setNull(boolean isNull) {
    this.isNull = isNull;
  }

  public boolean isLogin() {
    return isLogin;
  }

  public void setLogin(boolean isLogin) {
    this.isLogin = isLogin;
  }

  public void setOrderBy(String orderBy) {
    this.strOrderBy = orderBy;
  }

  public String getRSSLink(String cateId) {
    PortalContainer pcontainer = PortalContainer.getInstance();
    return CommonUtils.getRSSLink("forum", pcontainer.getPortalContainerInfo().getContainerName(), cateId);
  }

  public String getLastPostIdReadOfTopic(String topicId) throws Exception {
    return getUserProfile().getLastPostIdReadOfTopic(topicId);
  }

  private int getPageTopicRemember(String forumId) {
    if (pageTopicRemember.containsKey(forumId))
      return pageTopicRemember.get(forumId);
    return 1;
  }

  public void setUpdateForum(String categoryId, Forum forum, int page) throws Exception {
    this.forum = forum;
    this.forumId = forum.getId();
    this.categoryId = categoryId;
    this.pageSelect = page;
    if (page == 0)
      pageSelect = getPageTopicRemember(forumId);
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    isUseAjax = forumPortlet.isUseAjax();
    enableIPLogging = forumPortlet.isEnableIPLogging();
    forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath((categoryId + ForumUtils.SLASH + forumId));
    forumPortlet.updateAccessForum(forumId);
    cleanCheckedList();
    setForum(true);
  }

  public boolean getIsAutoPrune() throws Exception {
    return isShowActive;
  }

  public void updateByBreadcumbs(String categoryId, String forumId, boolean isBreadcumbs, int page) throws Exception {
    this.forumId = forumId;
    this.categoryId = categoryId;
    this.pageSelect = page;
    if (page == 0)
      pageSelect = getPageTopicRemember(forumId);
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    this.isUseAjax = forumPortlet.isUseAjax();
    enableIPLogging = forumPortlet.isEnableIPLogging();
    forumPortlet.updateAccessForum(forumId);
    if (!isBreadcumbs) {
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath((categoryId + ForumUtils.SLASH + forumId));
    }
    cleanCheckedList();
    setForum(true);
  }

  protected String getActionViewInfoUser(String linkType, String userName) {
    return getAncestorOfType(UIForumPortlet.class).getPortletLink(linkType, userName);
  }

  public boolean getCanAddNewThread() {
    return this.canAddNewThread;
  }

  public void setForum(boolean isSetModerator) throws Exception {
    this.forum = getForum();
    this.canAddNewThread = true;
    moderators = ForumServiceUtils.getUserPermission(forum.getModerators());
    String userId = getUserProfile().getUserId();
    isModerator = (userProfile.getUserRole() == 0 || (!userProfile.getIsBanned() && !moderators.isEmpty() && moderators.contains(userId))) ? true : false;
    boolean isCheck = true;
    List<String> ipBaneds = forum.getBanIP();
    if (ipBaneds != null && ipBaneds.contains(getRemoteIP()) || userProfile.getIsBanned()) {
      canAddNewThread = false;
      isCheck = false;
    }
    if (!isModerator && isCheck) {
      String[] strings = this.forum.getCreateTopicRole();
      boolean isEmpty = false;
      if (!ForumUtils.isArrayEmpty(strings)) {
        canAddNewThread = ForumServiceUtils.hasPermission(strings, userId);
      } else
        isEmpty = true;

      if (isEmpty || !canAddNewThread) {
        strings = getForumService().getPermissionTopicByCategory(categoryId, Utils.EXO_CREATE_TOPIC_ROLE);
        if (!ForumUtils.isArrayEmpty(strings)) {
          canAddNewThread = ForumServiceUtils.hasPermission(strings, userId);
        }
      }
    }
    UIForumContainer forumContainer = this.getParent();
    if (this.forum != null) {
      forumContainer.findFirstComponentOfType(UIForumInfos.class).setForum(this.forum);
    }
  }

  private Forum getForum() throws Exception {
    return getForumService().getForum(categoryId, forumId);
  }

  protected void initPage() throws Exception {
    setListWatches();
    objectId = forumId;
    if (getUserProfile() == null) {
      userProfile = new UserProfile();
    }
    
    TopicFilter filter = new TopicFilter(categoryId, forumId);
    filter.isAdmin(isModerator)
          .userLogin(userProfile.getUserId())
          .isApproved(forum.getIsModerateTopic());
    if (isModerator == false) {
      filter.viewers(ForumUtils.arraysMerge(forum.getViewer(), getForumService().getPermissionTopicByCategory(categoryId, Utils.EXO_VIEWER)));
    }

    ForumAdministration forumAdministration = getForumService().getForumAdministration();
    this.strOrderBy = forumAdministration.getTopicSortBy()+" "+forumAdministration.getTopicSortByType();
    filter.orderBy(strOrderBy);
    //
    this.topicListAccess = (TopicListAccess) getForumService().getTopics(filter);

    int pageSize = (int)this.userProfile.getMaxTopicInPage();
    topicListAccess.initialize(pageSize, pageSelect);
  }
  
  @Override
  public List<Integer> getInfoPage() throws Exception {
    List<Integer> temp = new ArrayList<Integer>();
    try {
      temp.add(topicListAccess.getPageSize());
      temp.add(topicListAccess.getCurrentPage());
      temp.add(topicListAccess.getSize());
      temp.add(topicListAccess.getTotalPages());
    } catch (Exception e) {
      temp.add(1);
      temp.add(1);
      temp.add(1);
      temp.add(1);
    }
    return temp;
  }


  private String getRemoteIP() throws Exception {
    if (enableIPLogging) {
      return WebUIUtils.getRemoteIP();
    }
    return ForumUtils.EMPTY_STR;
  }

  public String[] getActionMenuForum() throws Exception {
    String[] actions = new String[] { "EditForum", "SetUnLockForum", "SetLockedForum", "SetOpenForum", 
                                      "SetCloseForum", "MoveForum", "RemoveForum", "ExportForum", "WatchOption", "BanIpForumTools" };
    if (userProfile.getUserRole() > 0 || (userProfile.getUserRole() == 0 && 
        (!ForumUtils.isEmpty(getAncestorOfType(UIForumPortlet.class).getForumIdOfSpace())))) {
      actions = (String[]) ArrayUtils.removeElement(actions, "RemoveForum");
      actions = (String[]) ArrayUtils.removeElement(actions, "MoveForum");
    }
    return actions;
  }

  protected String getConfirm(String action) {
    String confirm;
    if (action.equals("MoveForum")) {
      confirm = WebUIUtils.getLabel(null, "UITopicContainer.confirm.MoveForum");
    } else {
      confirm = WebUIUtils.getLabel(null, "UITopicContainer.confirm.RemoveForum");
    }
    if (forumId.indexOf(Utils.FORUM_SPACE_ID_PREFIX) == 0) {
      confirm = new StringBuffer(confirm)
                .append(WebUIUtils.getLabel(null, "UICategory.confirm.in-space")).toString().replace("?", "") + " ?";
    }
    return confirm.replace("'", "\\47").replace("\"", "\\42");
  }
  
  public String[] getActionMenuTopic() throws Exception {
    String[] actions = { "EditTopic", "SetOpenTopic", "SetCloseTopic", "SetLockedTopic", "SetUnLockTopic", "SetStickTopic", "SetUnStickTopic", "SetMoveTopic", "SetDeleteTopic", "MergeTopic", "SetUnWaiting", "ApproveTopics", "ActivateTopics" };
    return actions;
  }

  public List<Topic> getTopicPageList() throws Exception{
    //
    topicListAccess.setCurrentPage(pageSelect);
    this.pageSelect = topicListAccess.getCurrentPage();

    maxPage = topicListAccess.getTotalPages();
    //
    topicList = Arrays.asList(topicListAccess.load(pageSelect));
    this.pageSelect = topicListAccess.getCurrentPage();
    
    
    pageTopicRemember.put(forumId, pageSelect);
    if (topicList == null)
      topicList = new ArrayList<Topic>();
    isShowActive = false;
    for (Topic topic : topicList) {
      if (!topic.getIsActive())
        isShowActive = true;
      if (getUICheckBoxInput(topic.getId()) != null) {
        getUICheckBoxInput(topic.getId()).setChecked(false);
      } else {
        addChild(new UICheckBoxInput(topic.getId(), topic.getId(), false));
      }
    }    
    return topicList;
  }

  private Topic getTopic(String topicId) throws Exception {
    return getForumService().getTopic(categoryId, forumId, topicId, null);
  }

  private Topic getTopicInForm(String topicId) {
    for (Topic topic : topicList) {
      if (topic.getId().equals(topicId)) {
        return topic;
      }
    }
    return null;
  }

  public long getSizePost(Topic topic) throws Exception {
    long maxPost = userProfile.getMaxPostInPage();
    if (maxPost <= 0) {
      maxPost = 10;
    }
    if (topic.getPostCount() >= maxPost) {
      long availablePost = 0;
      if (isModerator) {
        availablePost = topic.getPostCount() + 1;
      } else {
        String isApprove = ForumUtils.EMPTY_STR;
        String userLogin = userProfile.getUserId();
        if (this.forum.getIsModeratePost() || topic.getIsModeratePost()) {
          if (!(topic.getOwner().equals(userLogin))) {
            isApprove = "true";
          }
        }
        availablePost = getForumService().getAvailablePost(this.categoryId, this.forumId, topic.getId(), isApprove, "false", userLogin);
      }
      long value = (availablePost) / maxPost;
      if ((value * maxPost) < availablePost)
        value = value + 1;
      return value;
    } else
      return 1;
  }

  public String[] getStarNumber(Topic topic) throws Exception {
    double voteRating = topic.getVoteRating();
    return ForumUtils.getStarNumber(voteRating);
  }

  public boolean isModerator() {
    return isModerator;
  }

  static public class SearchFormActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      String path = uiTopicContainer.forum.getPath();
      UIFormStringInput formStringInput = uiTopicContainer.getUIStringInput(ForumUtils.SEARCHFORM_ID);
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
        if (uiTopicContainer.isModerator) {
          type.append("true,").append(Utils.TOPIC).append(ForumUtils.SLASH).append(Utils.POST);
        } else {
          type.append("false,").append(Utils.TOPIC).append(ForumUtils.SLASH).append(Utils.POST);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
        categoryContainer.updateIsRender(true);
        UICategories categories = categoryContainer.getChild(UICategories.class);
        categories.setIsRenderChild(true);
        List<ForumSearchResult> list = uiTopicContainer.getForumService().getQuickSearch(text, type.toString(), path, uiTopicContainer.getUserProfile().getUserId(), forumPortlet.getInvisibleCategories(), forumPortlet.getInvisibleForums(), null);
        UIForumListSearch listSearchEvent = categories.getChild(UIForumListSearch.class);
        listSearchEvent.setListSearchEvent(text, list, path.substring(path.indexOf(Utils.CATEGORY)));
        forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL);
        formStringInput.setValue(ForumUtils.EMPTY_STR);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UIQuickSearchForm.msg.checkEmpty");
      }
    }
  }

  static public class GoNumberPageActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer topicContainer, final String objectId) throws Exception {
      int idbt = Integer.parseInt(objectId);
      UIFormStringInput stringInput1 = topicContainer.getUIStringInput(ForumUtils.GOPAGE_ID_T);
      UIFormStringInput stringInput2 = topicContainer.getUIStringInput(ForumUtils.GOPAGE_ID_B);
      String numberPage = ForumUtils.EMPTY_STR;
      if (idbt == 1) {
        numberPage = stringInput1.getValue();
      } else {
        numberPage = stringInput2.getValue();
      }
      stringInput1.setValue(ForumUtils.EMPTY_STR);
      stringInput2.setValue(ForumUtils.EMPTY_STR);
      numberPage = ForumUtils.removeZeroFirstNumber(numberPage);
      if (!ForumUtils.isEmpty(numberPage)) {
        try {
          int page = Integer.parseInt(numberPage.trim());
          if (page < 0) {
            warning("NameValidator.msg.Invalid-number", new String[] { getLabel("GoPage") });
          } else {
            if (page == 0) {
              page = 1;
            } else if (page > topicContainer.topicListAccess.getTotalPages()) {
              page = topicContainer.topicListAccess.getTotalPages();
            }
            topicContainer.pageSelect = page;
            event.getRequestContext().addUIComponentToUpdateByAjax(topicContainer);
          }
        } catch (NumberFormatException e) {
          warning("NameValidator.msg.Invalid-number", new String[] { getLabel("GoPage") });
        }
      }
    }
  }
  
  static private abstract class BaseTopicContainerActionListener extends BaseForumEventListener<UITopicContainer> {
    @Override
    public boolean isValid(UITopicContainer component, String objectId) throws Exception {
      if (isExisting(component.categoryId) == false) {
        categoryNotExist();
        return false;
      }
      if (isExisting(component.forumId) == false) {
        forumNotExist(component.categoryId);
        return false;
      }
      return true;
    }

    public String getForumPath() {
      StringBuffer buffer = new StringBuffer(component.categoryId).append(ForumUtils.SLASH).append(component.forumId);
      return buffer.toString();
    }
    
    public void errorEvent() throws Exception {}
  }

  static public class AddTopicActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      UITopicForm topicForm = uiTopicContainer.openPopup(UITopicForm.class, "UIAddTopicContainer", 900, 520);
      topicForm.setTopicIds(uiTopicContainer.categoryId, uiTopicContainer.forumId, uiTopicContainer.forum);
      topicForm.setSpaceGroupId(uiTopicContainer.getAncestorOfType(UIForumPortlet.class).getSpaceGroupId());
      topicForm.setMod(uiTopicContainer.isModerator);
    }
  }

  static public class AddPollActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {

      UIPollForm pollForm = uiTopicContainer.openPopup(UIPollForm.class, 655, 455);
      pollForm.setAddTopic(uiTopicContainer.getForum().getPath());
      
    }
  }

  static public class OpenTopicActionListener extends BaseTopicContainerActionListener {
    private Topic topic;
    private Forum forum;
    private int    pageuNumber = 0;
    private String topicId     = "";
    private String postView    = "";

    @Override
    public boolean isValid(UITopicContainer component, String objectId) throws Exception {
      if (super.isValid(component, objectId) == true) {
        String params = objectId;
        if (ForumUtils.isEmpty(objectId) || objectId.indexOf(ForumUtils.COMMA) < 0) {
          params = new StringBuffer(component.openTopicId).append(ForumUtils.COMMA)
                     .append("1").append(ForumUtils.COMMA).append("false").toString();
        }
        String[] strs = params.split(ForumUtils.COMMA);
        topicId = strs[0];
        pageuNumber = Integer.parseInt(strs[1].trim());
        postView = strs[2];
        
        topic = forumService.getTopic(component.categoryId, component.forumId, topicId, null);
        if (topic == null) {
          topic = (Topic) forumService.getObjectNameById(topicId, Utils.TOPIC);
        }
        if (topic == null) {
          topicNotExist();
          return false;
        }
        forum = forumService.getForum(topic.getCategoryId(), topic.getForumId());
        boolean isModerator = (component.getUserProfile().getUserRole() == 0 || (component.getUserProfile().getUserRole() == 1 &&
                                  ForumServiceUtils.hasPermission(forum.getModerators(), component.getUserProfile().getUserId())));
        if (isModerator == false) {
          if (forum.getIsClosed()) {
            warning("UIForumPortlet.msg.do-not-permission", false);
            openCategory(component.categoryId);
            return false;
          }
          if (topic.getIsClosed() || topic.getIsWaiting() || !topic.getIsActive() || !topic.getIsActiveByForum()) {
            warning("UIForumPortlet.msg.do-not-permission", false);
            context.addUIComponentToUpdateByAjax(component);
            return false;
          }
        }
        return true;
      }
      return false;
    }

    @Override
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String idAndNumber) throws Exception {
      uiTopicContainer.openTopicId = ForumUtils.EMPTY_STR;
      UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
      UITopicDetailContainer uiTopicDetailContainer = uiForumContainer.getChild(UITopicDetailContainer.class);
      uiForumContainer.setIsRenderChild(false);
      UITopicDetail uiTopicDetail = uiTopicDetailContainer.getChild(UITopicDetail.class);
      uiTopicDetail.setUpdateForum(forum);
      uiTopicDetailContainer.getChild(UITopicPoll.class).updateFormPoll(topic.getCategoryId(), topic.getForumId(), topic.getId());
      if (postView.equals("true")) {
        uiTopicDetail.setIdPostView("lastpost");
      } else if (postView.equals("false")) {
        uiTopicDetail.setIdPostView("top");
      } else {
        uiTopicDetail.setIdPostView(postView);
        uiTopicDetail.setLastPostId(postView);
      }
      if (UserProfile.USER_GUEST.equals(uiTopicContainer.getUserProfile().getUserId()) == false) {
        forumService.updateTopicAccess(uiTopicContainer.getUserProfile().getUserId(), topic.getId());
      }
      uiTopicDetail.initInfoTopic(topic.getCategoryId(), topic.getForumId(), topic, pageuNumber);
      context.addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class EditForumActionListener extends BaseTopicContainerActionListener {
    @Override
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      String spaceGroupId = uiTopicContainer.getAncestorOfType(UIForumPortlet.class).getSpaceGroupId();
      UIForumForm forumForm = uiTopicContainer.openPopup(UIForumForm.class, "EditForumForm", 650, 480);
      if (uiTopicContainer.userProfile.getUserRole() == 1){
        forumForm.setMode(true);
      }
      forumForm.initForm(spaceGroupId);
      forumForm.setCategoryValue(uiTopicContainer.categoryId, false);
      forumForm.setForumValue(forum, true);
      forumForm.setForumUpdate(true);
    }
  }
  
  static private abstract class SetStatusForumListener extends BaseTopicContainerActionListener {
    private int modifyType;
    private boolean isLock = false;
    private boolean isClosed = false;
    private String action = "";

    public void onEvent(UITopicContainer uiTopicContainer, Forum forum) throws Exception {
      try {
        forum.setIsLock(isLock);
        forum.setIsClosed(isClosed);
        uiTopicContainer.getForumService().modifyForum(forum, modifyType);
        uiTopicContainer.setForum(true);
      } catch (Exception e) {
        warning(String.format("UITopicContainer.msg.fail-%s-forum", action), false);
        uiTopicContainer.log.debug(String.format("Failed to %s forum %s", action, uiTopicContainer.forumId), e);
      }
      context.addUIComponentToUpdateByAjax(uiTopicContainer);
    }

    public void initParams(String action, boolean isClosed, boolean isLock, int modifyType) {
      this.action = action;
      this.isClosed = isClosed;
      this.isLock = isLock;
      this.modifyType = modifyType;
    }
  }

  static public class SetLockedForumActionListener extends SetStatusForumListener {
    @Override
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      initParams("lock", forum.getIsClosed(), true, Utils.LOCK);
      onEvent(uiTopicContainer, forum);
    }
  }

  static public class SetUnLockForumActionListener extends SetStatusForumListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      initParams("unlock", forum.getIsClosed(), false, Utils.LOCK);
      onEvent(uiTopicContainer, forum);
    }
  }

  static public class SetOpenForumActionListener extends SetStatusForumListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      initParams("open", false, forum.getIsLock(), Utils.CLOSE);
      onEvent(uiTopicContainer, forum);
    }
  }

  static public class SetCloseForumActionListener extends SetStatusForumListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      initParams("close", true, forum.getIsLock(), Utils.CLOSE);
      onEvent(uiTopicContainer, forum);
    }
  }

  static public class MoveForumActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      List<Forum> forums = new ArrayList<Forum>();
      forums.add(forum);
      UIMoveForumForm moveForumForm = uiTopicContainer.openPopup(UIMoveForumForm.class, 315, 365);
      moveForumForm.setListForum(forums, uiTopicContainer.categoryId);
      moveForumForm.setForumUpdate(true);
    }
  }

  static public class RemoveForumActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      forumService.removeForum(uiTopicContainer.categoryId, forum.getId());
      openCategory(uiTopicContainer.categoryId);
    }
  }

  static public class ExportForumActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      UIExportForm exportForm = uiTopicContainer.openPopup(UIExportForm.class, 500, 160);
      exportForm.setObjectId(forum);
    }
  }

  static public class WatchOptionActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      uiTopicContainer.forum = uiTopicContainer.getForumService().getForum(uiTopicContainer.categoryId, uiTopicContainer.forumId);
      UIWatchToolsForm watchToolsForm = uiTopicContainer.openPopup(UIWatchToolsForm.class, 500, 365);
      watchToolsForm.setPath(uiTopicContainer.forum.getPath());
      watchToolsForm.setEmails(uiTopicContainer.forum.getEmailNotification());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  // ----------------------------------MenuThread---------------------------------
  static private abstract class ModifyTopicsActionListener extends BaseTopicContainerActionListener {
    protected List<String> topicIdNotExist = new ArrayList<String>();

    protected List<String> topicIdSelected() throws Exception {
      List<String> topicIds = new ArrayList<String>();
      topicIdNotExist.clear();
      for (String topicId : component.getIdSelected()) {
        String topicPath = new StringBuffer(getForumPath()).append(ForumUtils.SLASH).append(topicId).toString();
        if (isExisting(topicPath)) {
          topicIds.add(topicId);
        } else {
          topicIdNotExist.add(topicId);
        }
      }
      return topicIds;
    }

    protected List<Topic> topicSelected() throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = topicIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = component.getTopic(topicId);
        if (topic != null) {
          if (modifyTopic(topic) == true) {
            topics.add(topic);
          }
        }
      }
      return topics;
    }

    protected List<String> topicNameRemoveds() {
      List<String> topicNames = new ArrayList<String>();
      for (String topicId : topicIdNotExist) {
        Topic topic = component.getTopicInForm(topicId);
        if (topic != null) {
          topicNames.add(topic.getTopicName());
        }
      }
      return topicNames;
    }

    protected void modifyTopics(int modifyType, String errorMsg) throws Exception {
      List<Topic> topics = topicSelected();
      if (topics.size() > 0) {
        try {
          forumService.modifyTopic(topics, modifyType);
          context.addUIComponentToUpdateByAjax(component);
        } catch (Exception e) {
          warning(errorMsg, false);
          component.log.error(WebUIUtils.getLabel(null, errorMsg), e);
        }
      } else {
        if (showPopup() == false) {
          warningMessage();
        }
      }
    }

    protected void warningMessage() {
      if (topicIdNotExist.size() == 1) {
        warning("UIForumPortlet.msg.topicEmpty", false);
      } else if (topicIdNotExist.size() > 1) {
        warning("UIForumPortlet.msg.listTopicEmpty", false);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }

    protected abstract boolean showPopup() throws Exception;
    protected abstract boolean modifyTopic(Topic topic);
  }
  
  
  static public class ApproveTopicsActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      modifyTopics(Utils.APPROVE, "UITopicContainer.msg.fail-approve-topics");
    }

    @Override
    protected boolean showPopup() throws Exception {
      UIPageListTopicUnApprove pageList = component.openPopup(UIPageListTopicUnApprove.class, "PageListTopicUnApprove", 760, 450);
      pageList.setTypeApprove(Utils.APPROVE);
      pageList.setUpdateContainer(component.categoryId, component.forumId);
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsApproved() == false) {
        topic.setIsApproved(true);
        topic.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, topic.getId(), false));
        return true;
      }
      return false;
    }
  }

  static public class ActivateTopicsActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      modifyTopics(Utils.ACTIVE, "UITopicContainer.msg.fail-activate-topics");
    }

    @Override
    protected boolean showPopup() throws Exception {
      UIPageListTopicUnApprove pageListTopicUnApprove = component.openPopup(UIPageListTopicUnApprove.class, "PageListTopicInActive", 760, 450);
      pageListTopicUnApprove.setId("UIPageListTopicInActive");
      pageListTopicUnApprove.setTypeApprove(Utils.ACTIVE);
      pageListTopicUnApprove.setUpdateContainer(component.categoryId, component.forumId);
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsActive() == false) {
        topic.setIsActive(true);
        return true;
      }
      return false;
    }
  }

  static public class EditTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<String> topicIds = topicIdSelected();
      if (topicIds.size() > 0) {
        StringBuffer path = new StringBuffer(getForumPath());
        Topic topic = uiTopicContainer.getForumService().getTopicByPath(path.append(ForumUtils.SLASH).append(topicIds.get(0)).toString(), false);
        UITopicForm topicForm = uiTopicContainer.openPopup(UITopicForm.class, "UIEditTopicContainer", 900, 545);
        topicForm.setTopicIds(uiTopicContainer.categoryId, uiTopicContainer.forumId, uiTopicContainer.forum);
        topicForm.setUpdateTopic(topic, true);
        topicForm.setMod(uiTopicContainer.isModerator);
        topicForm.setSpaceGroupId(uiTopicContainer.getAncestorOfType(UIForumPortlet.class).getSpaceGroupId());
      } else {
        warningMessage();
      }
    }

    @Override
    protected boolean showPopup() throws Exception {
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      return false;
    }
  }

  static public class SetOpenTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiForm, final String objectId) throws Exception {
      modifyTopics(Utils.CLOSE, "UITopicContainer.msg.fail-open-topics");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsClosed() == true) {
        topic.setIsClosed(false);
        return true;
      }
      return false;
    }
  }

  static public class SetCloseTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiForm, final String objectId) throws Exception {
      modifyTopics(Utils.CLOSE, "UITopicContainer.msg.fail-close-topics");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsClosed() == false) {
        topic.setIsClosed(true);
        return true;
      }
      return false;
    }
  }

  static public class SetLockedTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiForm, final String objectId) throws Exception {
      modifyTopics(Utils.LOCK, "UITopicContainer.msg.fail-lock-topic");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsLock() == false) {
        topic.setIsLock(true);
        return true;
      }
      return false;
    }
  }

  static public class SetUnLockTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      if (uiTopicContainer.getForum().getIsLock()) {
        warning("UITopicContainer.sms.ForumIsLocked");
        return;
      }
      modifyTopics(Utils.LOCK, "UITopicContainer.msg.fail-unlock-topic");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsLock() == true) {
        topic.setIsLock(false);
        return true;
      }
      return false;
    }
  }

  static public class SetUnStickTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiform, final String objectId) throws Exception {
      modifyTopics(Utils.STICKY, "UITopicContainer.msg.fail-unstick-topic");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsSticky() == true) {
        topic.setIsSticky(false);
        return true;
      }
      return false;
    }
  }

  static public class SetStickTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiform, final String objectId) throws Exception {
      modifyTopics(Utils.STICKY, "UITopicContainer.msg.fail-stick-topic");
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsSticky() == false) {
        topic.setIsSticky(true);
        return true;
      }
      return false;
    }
  }

  static public class SetMoveTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiform, final String objectId) throws Exception {
      List<Topic> topics = topicSelected();
      if (topics.size() > 0) {
        UIMoveTopicForm moveTopicForm = uiform.openPopup(UIMoveTopicForm.class, 400, 420);
        moveTopicForm.updateTopic(uiform.forumId, topics, false);
      } else {
        warningMessage();
      }
      context.addUIComponentToUpdateByAjax(uiform);
    }

    @Override
    protected boolean showPopup() throws Exception {
      return false;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      return true;
    }
  }

  static public class MergeTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = topicSelected();
      if (topics.size() > 1) {
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        UIPopupAction popupAction = forumPortlet.getChild(UIPopupAction.class);
        UIMergeTopicForm mergeTopicForm = popupAction.createUIComponent(UIMergeTopicForm.class, null, null);
        mergeTopicForm.updateTopics(topics);
        popupAction.activate(mergeTopicForm, 560, 260);
        context.addUIComponentToUpdateByAjax(popupAction);
      } else {
        warningMessage();
      }
      context.addUIComponentToUpdateByAjax(uiTopicContainer);
    }

    @Override
    protected boolean showPopup() throws Exception {
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      return true;
    }
  }

  static public class SetDeleteTopicActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = topicSelected();
      if (topics.size() > 0) {
        for (Topic topic : topics) {
          try {
            forumService.removeTopic(uiTopicContainer.categoryId, uiTopicContainer.forumId, topic.getId());
          } catch (Exception e) {
            uiTopicContainer.log.error("Removing " + topic.getId() + " fail. \nCaused by: " + e.getCause());
          }
        }
        forumPortlet.removeCacheUserProfile();
      } else {
        warningMessage();
      }
      context.addUIComponentToUpdateByAjax(uiTopicContainer);
    }

    @Override
    protected boolean showPopup() throws Exception {
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      return true;
    }
  }

  static public class SetUnWaitingActionListener extends ModifyTopicsActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      modifyTopics(Utils.WAITING, "UITopicContainer.msg.fail-set-unwaiting-topic");
    }

    @Override
    protected boolean showPopup() throws Exception {
      UIPageListTopicUnApprove pageListTopicUnApprove = component.openPopup(UIPageListTopicUnApprove.class, "PageListTopicWaiting", 760, 450);
      pageListTopicUnApprove.setId("UIPageListTopicWaiting");
      pageListTopicUnApprove.setTypeApprove(Utils.WAITING);
      pageListTopicUnApprove.setUpdateContainer(component.categoryId, component.forumId);
      return true;
    }

    @Override
    protected boolean modifyTopic(Topic topic) {
      if (topic.getIsWaiting() == true) {
        topic.setIsWaiting(false);
        topic.setLink(ForumUtils.createdForumLink(ForumUtils.TOPIC, topic.getId(), false));
        return true;
      }
      return false;
    }
  }

  static public class SetOrderByActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String path) throws Exception {
      uiTopicContainer.strOrderBy = ForumUtils.getSQLOrderBy(uiTopicContainer.strOrderBy, path);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class AddBookMarkActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String topicId) throws Exception {
      if (!ForumUtils.isEmpty(topicId)) {
        StringBuffer buffer = new StringBuffer();
        if (topicId.equals("forum")) {
          buffer.append("uiIconUIForms//").append(uiTopicContainer.forum.getForumName()).append("//").append(uiTopicContainer.forumId);
        } else {
          Topic topic = uiTopicContainer.getTopic(topicId);
          buffer.append("uiIconForumTopic//").append(topic.getTopicName()).append("//").append(topicId);
        }
        String userName = uiTopicContainer.userProfile.getUserId();
        uiTopicContainer.getForumService().saveUserBookmark(userName, buffer.toString(), true);
      }
    }
  }

  static public class AddWatchingActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String path) throws Exception {
      StringBuffer buffer = new StringBuffer(getForumPath());
      if (path.equals("forum") == false) {
        buffer.append(ForumUtils.SLASH).append(path);
      }
      uiTopicContainer.addWatch(buffer.toString());
      context.addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class UnWatchActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String path) throws Exception {
      StringBuffer buffer = new StringBuffer(getForumPath());
      if (path.equals("forum") == false) {
        buffer.append(ForumUtils.SLASH).append(path);
      }
      uiTopicContainer.unWatch(buffer.toString());
      context.addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class AdvancedSearchActionListener extends EventListener<UITopicContainer> {
    public void execute(Event<UITopicContainer> event) throws Exception {
      UITopicContainer uiForm = event.getSource();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.updateIsRendered(ForumUtils.FIELD_SEARCHFORUM_LABEL);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL);
      UISearchForm searchForm = forumPortlet.getChild(UISearchForm.class);
      searchForm.setPath(uiForm.forum.getPath());
      searchForm.setSelectType(Utils.TOPIC);
      searchForm.setSearchOptionsObjectType(Utils.TOPIC);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class BanIpForumToolsActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      UIBanIPForumManagerForm ipForumManager = uiTopicContainer.openPopup(UIBanIPForumManagerForm.class, "BanIPForumManagerForm", 430, 500);
      ipForumManager.setForumId(getForumPath());
    }
  }

  static public class RSSActionListener extends BaseTopicContainerActionListener {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiForm, final String forumId) throws Exception {
      if (!uiForm.getUserProfile().getUserId().equals(UserProfile.USER_GUEST)) {
        uiForm.getForumService().addWatch(-1, forumId, null, uiForm.getUserProfile().getUserId());
      }
    }
  }
}
