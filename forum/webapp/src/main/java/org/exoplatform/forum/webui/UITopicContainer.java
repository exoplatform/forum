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

import javax.portlet.ActionResponse;
import javax.portlet.PortletSession;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.info.ForumParameter;
import org.exoplatform.forum.service.Forum;
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
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
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
    @EventConfig(listeners = UITopicContainer.OpenTopicsTagActionListener.class ),// Menu
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

  private void setForumModeratorPortlet(){
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletSession portletSession = pcontext.getRequest().getPortletSession();
    ActionResponse actionRes = null;
    if (pcontext.getResponse() instanceof ActionResponse) {
      actionRes = (ActionResponse) pcontext.getResponse();
    }
    ForumParameter param = new ForumParameter();
    param.setRenderModerator(true);
    param.setModerators(moderators);
    param.setRenderRule(true);
    List<String> list = param.getInfoRules();
    boolean isLock = forum.getIsClosed();
    if (!isLock)
      isLock = forum.getIsLock();
    if (!isLock)
      isLock = !canAddNewThread;
    list.set(0, String.valueOf(isLock));
    param.setInfoRules(list);
    if (actionRes != null) {
      actionRes.setEvent(new QName("ForumModerateEvent"), param);
      actionRes.setEvent(new QName("ForumRuleEvent"), param);
    } else {
      portletSession.setAttribute(UIForumPortlet.FORUM_MODERATE_EVENT_PARAMS, param, PortletSession.APPLICATION_SCOPE);
      portletSession.setAttribute(UIForumPortlet.RULE_EVENT_PARAMS, param, PortletSession.APPLICATION_SCOPE);      
    }
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
    if (isSetModerator)
      setForumModeratorPortlet();
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
    for (Topic topic : topicList) {
      if (topic.getId().equals(topicId)) {
        return topic;
      }
    }
    return getForumService().getTopic(categoryId, forumId, topicId, userProfile.getUserId());
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

  static public class AddTopicActionListener extends BaseEventListener<UITopicContainer> {
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

  static public class OpenTopicsTagActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.updateIsRendered(ForumUtils.TAG);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(objectId);
      forumPortlet.getChild(UITopicsTag.class).setIdTag(objectId);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class OpenTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String idAndNumber) throws Exception {
      idAndNumber = (ForumUtils.isEmpty(idAndNumber) || idAndNumber.indexOf(ForumUtils.COMMA) < 0) ? 
                      uiTopicContainer.openTopicId + ForumUtils.COMMA + "1" + ForumUtils.COMMA + "false" : idAndNumber;
      uiTopicContainer.openTopicId = ForumUtils.EMPTY_STR;
      String[] temp = idAndNumber.split(ForumUtils.COMMA);
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      try {

        Topic topic = uiTopicContainer.getTopic(temp[0]);
        if(topic != null) {
          topic = uiTopicContainer.getForumService().getTopicUpdate(topic, false);
        } else {
          uiTopicContainer.getForumService().getTopic(uiTopicContainer.categoryId, uiTopicContainer.forumId, temp[0], uiTopicContainer.getUserProfile().getUserId());
        }
        
        if (topic != null) {
          uiTopicContainer.forum = uiTopicContainer.getForumService().getForum(uiTopicContainer.categoryId, uiTopicContainer.forumId);
          if (uiTopicContainer.forum != null) {
            if(!uiTopicContainer.isModerator){
              if(uiTopicContainer.forum.getIsClosed()) {
                forumPortlet.renderForumHome();
                warning("UIForumPortlet.msg.do-not-permission", false);
                event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
                return;
              }
              if(topic.getIsClosed() || topic.getIsWaiting() || !topic.getIsActive() 
                  || !topic.getIsActiveByForum()) {
                warning("UIForumPortlet.msg.do-not-permission", false);
                event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
                return;
              }
            }
            UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
            UITopicDetailContainer uiTopicDetailContainer = uiForumContainer.getChild(UITopicDetailContainer.class);
            uiForumContainer.setIsRenderChild(false);
            UITopicDetail uiTopicDetail = uiTopicDetailContainer.getChild(UITopicDetail.class);
            uiTopicDetail.setUpdateForum(uiTopicContainer.forum);
            uiTopicDetailContainer.getChild(UITopicPoll.class).updateFormPoll(uiTopicContainer.categoryId, uiTopicContainer.forumId, topic.getId());
            if (temp[2].equals("true")) {
              uiTopicDetail.setIdPostView("lastpost");
            } else if (temp[2].equals("false")) {
              uiTopicDetail.setIdPostView("top");
            } else {
              uiTopicDetail.setIdPostView(temp[2]);
              uiTopicDetail.setLastPostId(temp[2]);
            }
            if (!forumPortlet.getUserProfile().getUserId().equals(UserProfile.USER_GUEST)) {
              uiTopicContainer.getForumService().updateTopicAccess(forumPortlet.getUserProfile().getUserId(), topic.getId());
            }
            uiTopicDetail.initInfoTopic(uiTopicContainer.categoryId, uiTopicContainer.forumId, topic, Integer.parseInt(temp[1]));
            WebuiRequestContext context = event.getRequestContext();
            context.addUIComponentToUpdateByAjax(uiForumContainer);
            context.addUIComponentToUpdateByAjax(forumPortlet.getChild(UIBreadcumbs.class));
          } else {
            forumPortlet.renderForumHome();
            warning("UITopicContainer.msg.forum-deleted", false);
            event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
          }
        } else {
          warning("UIForumPortlet.msg.topicEmpty", false);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
        }
      } catch (Exception e) {
        event.getSource().log.error("\nCould not open " + uiTopicContainer.getTopic(temp[0]) + " topic\n", e);
      }
    }
  }

  static public class EditForumActionListener extends BaseEventListener<UITopicContainer> {
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

  static public class SetLockedForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      try {
        forum.setIsLock(true);
        uiTopicContainer.getForumService().modifyForum(forum, Utils.LOCK);
        uiTopicContainer.setForum(true);
      } catch (Exception e) {
        warning("UITopicContainer.msg.fail-lock-forum", false);
        event.getSource().log.debug(String.format("Failed to lock forum %s", forum.getForumName()), e);
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class SetUnLockForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      try {
        forum.setIsLock(false);
        uiTopicContainer.getForumService().modifyForum(forum, Utils.LOCK);
        uiTopicContainer.setForum(true);
      } catch (Exception e) {
        warning("UITopicContainer.msg.fail-unlock-forum", false);
        event.getSource().log.debug(String.format("Failed to unlock forum %s", forum.getForumName()), e);
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class SetOpenForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      try {
        forum.setIsClosed(false);
        uiTopicContainer.getForumService().modifyForum(forum, Utils.CLOSE);
        uiTopicContainer.setForum(true);
      } catch (Exception e) {
        warning("UITopicContainer.msg.fail-open-forum", false);
        event.getSource().log.debug(String.format("Failed to open forum %s", forum.getForumName()), e);
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class SetCloseForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      try {
        forum.setIsClosed(true);
        uiTopicContainer.getForumService().modifyForum(forum, Utils.CLOSE);
        uiTopicContainer.setForum(true);
      } catch (Exception e) {
        warning("UITopicContainer.msg.fail-close-forum", false);
        event.getSource().log.debug(String.format("Failed to close forum %s", forum.getForumName()), e);
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class MoveForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      List<Forum> forums = new ArrayList<Forum>();
      forums.add(forum);
      UIMoveForumForm moveForumForm = uiTopicContainer.openPopup(UIMoveForumForm.class, 315, 365);
      moveForumForm.setListForum(forums, uiTopicContainer.categoryId);
      moveForumForm.setForumUpdate(true);
    }
  }

  static public class RemoveForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      if (forum == null) {
        warning("UITopicContainer.msg.forum-deleted", false);
        forumPortlet.renderForumHome();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        return;
      }
      uiTopicContainer.getForumService().removeForum(uiTopicContainer.categoryId, forum.getId());
      UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
      forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
      categoryContainer.updateIsRender(false);
      categoryContainer.getChild(UICategory.class).updateByBreadcumbs(uiTopicContainer.categoryId);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(uiTopicContainer.categoryId);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class ExportForumActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      Forum forum = uiTopicContainer.getForum();
      if (forum == null) {
        warning("UITopicContainer.msg.forum-deleted", false);
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        forumPortlet.renderForumHome();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
        return;
      }
      UIExportForm exportForm = uiTopicContainer.openPopup(UIExportForm.class, 500, 160);
      exportForm.setObjectId(forum);
    }
  }

  static public class WatchOptionActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      try {
        uiTopicContainer.forum = uiTopicContainer.getForumService().getForum(uiTopicContainer.categoryId, uiTopicContainer.forumId);
        UIWatchToolsForm watchToolsForm = uiTopicContainer.openPopup(UIWatchToolsForm.class, 500, 365);
        watchToolsForm.setPath(uiTopicContainer.forum.getPath());
        watchToolsForm.setEmails(uiTopicContainer.forum.getEmailNotification());
      } catch (Exception e) {
        warning("UITopicContainer.msg.forum-deleted", false);
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
        forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
        categoryContainer.updateIsRender(false);
        categoryContainer.getChild(UICategory.class).updateByBreadcumbs(uiTopicContainer.categoryId);
        forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(uiTopicContainer.categoryId);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  // ----------------------------------MenuThread---------------------------------
  static public class ApproveTopicsActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<String> topicIds = uiTopicContainer.getIdSelected();
      List<Topic> topics = new ArrayList<Topic>();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsApproved())
            continue;
          topic.setIsApproved(true);
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        uiTopicContainer.getForumService().modifyTopic(topics, Utils.APPROVE);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
      } else {
        UIPageListTopicUnApprove pageListTopicUnApprove = uiTopicContainer.openPopup(UIPageListTopicUnApprove.class, "PageListTopicUnApprove", 760, 450);
        pageListTopicUnApprove.setTypeApprove(Utils.APPROVE);
        pageListTopicUnApprove.setUpdateContainer(uiTopicContainer.categoryId, uiTopicContainer.forumId);
      }
    }
  }

  static public class ActivateTopicsActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<String> topicIds = uiTopicContainer.getIdSelected();
      List<Topic> topics = new ArrayList<Topic>();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsActive())
            continue;
          topic.setIsActive(true);
          topics.add(topic);
        }
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      if (topics.size() > 0) {
        uiTopicContainer.getForumService().modifyTopic(topics, Utils.ACTIVE);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        UIPageListTopicUnApprove pageListTopicUnApprove = uiTopicContainer.openPopup(UIPageListTopicUnApprove.class, "PageListTopicInActive", 760, 450);
        pageListTopicUnApprove.setId("UIPageListTopicInActive");
        pageListTopicUnApprove.setTypeApprove(Utils.ACTIVE);
        pageListTopicUnApprove.setUpdateContainer(uiTopicContainer.categoryId, uiTopicContainer.forumId);
      }
    }
  }

  static public class EditTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic = null;
      boolean checked = false;
      String path = uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getForumService().getTopicByPath(path + ForumUtils.SLASH + topicId, false);
        if (topic != null) {
          checked = true;
          break;
        }
      }
      if (checked) {
        UITopicForm topicForm = uiTopicContainer.openPopup(UITopicForm.class, "UIEditTopicContainer", 900, 545);
        topicForm.setTopicIds(uiTopicContainer.categoryId, uiTopicContainer.forumId, uiTopicContainer.forum);
        topicForm.setUpdateTopic(topic, true);
        topicForm.setMod(uiTopicContainer.isModerator);
        topicForm.setSpaceGroupId(uiTopicContainer.getAncestorOfType(UIForumPortlet.class).getSpaceGroupId());
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetOpenTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<String> topicIds = uiTopicContainer.getIdSelected();
      List<Topic> topics = new ArrayList<Topic>();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (!topic.getIsClosed())
            continue;
          topic.setIsClosed(false);
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.CLOSE);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-open-topics", false);
          event.getSource().log.debug("Failed to open topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetCloseTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsClosed())
            continue;
          topic.setIsClosed(true);
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.CLOSE);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-close-topics", false);
          event.getSource().log.debug("Failed to close topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetLockedTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsLock())
            continue;
          topic.setIsLock(true);
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.LOCK);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-lock-topic", false);
          event.getSource().log.debug("Failed to lock topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetUnLockTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      try {
        if (uiTopicContainer.getForum().getIsLock()) {         
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UITopicContainer.sms.ForumIsLocked",
                                                                                         new Object[] {},
                                                                                         ApplicationMessage.WARNING));          
          return;
        }
      } catch (Exception e) {
        event.getSource().log.error("Setting unlock a topic fail. \n Caused by: " + e.getCause());
        return;
      }
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (!topic.getIsLock())
            continue;
          topic.setIsLock(false);
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.LOCK);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-unlock-topic", false);
          event.getSource().log.debug("Failed to unlock topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetUnStickTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsSticky()) {
            topic.setIsSticky(false);
            topics.add(topic);
          }
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.STICKY);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-unstick-topic", false);
          event.getSource().log.debug("Failed to unstick topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetStickTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (!topic.getIsSticky()) {
            topic.setIsSticky(true);
            topics.add(topic);
          }
        }
      }
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.STICKY);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-stick-topic", false);
          event.getSource().log.debug("Failed to stick topics", e);
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetMoveTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        UIMoveTopicForm moveTopicForm = uiTopicContainer.openPopup(UIMoveTopicForm.class, 400, 420);
        moveTopicForm.updateTopic(uiTopicContainer.forumId, topics, false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
      } else {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class MergeTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          topics.add(topic);
        }
      }
      if (topics.size() > 1) {
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        UIPopupAction popupAction = forumPortlet.getChild(UIPopupAction.class);
        UIMergeTopicForm mergeTopicForm = popupAction.createUIComponent(UIMergeTopicForm.class, null, null);
        // UIMergeTopicForm mergeTopicForm = uiTopicContainer.openPopup(UIMergeTopicForm.class, 560, 260) ;
        mergeTopicForm.updateTopics(topics);
        popupAction.activate(mergeTopicForm, 560, 260);
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
      } else {
        warning("UITopicContainer.sms.notCheckThreads", false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class SetDeleteTopicActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          topics.add(topic);
        }
      }
      if (topics.size() > 0) {
        for (Topic topic_ : topics) {
          try {
            uiTopicContainer.getForumService().removeTopic(uiTopicContainer.categoryId, uiTopicContainer.forumId, topic_.getId());
          } catch (Exception e) {
            event.getSource().log.error("Removing " + topic_.getId() + " fail. \nCaused by: " + e.getCause());
          }
        }
        UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
        forumPortlet.removeCacheUserProfile();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      } else if (topics.size() == 0) {
        warning("UITopicDetail.msg.notCheckTopic");
      }
    }
  }

  static public class SetUnWaitingActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      List<Topic> topics = new ArrayList<Topic>();
      List<String> topicIds = uiTopicContainer.getIdSelected();
      Topic topic;
      for (String topicId : topicIds) {
        topic = uiTopicContainer.getTopic(topicId);
        if (topic != null) {
          if (topic.getIsWaiting()) {
            topic.setIsWaiting(false);
            topics.add(topic);
          }
        }
      }
      UIForumPortlet forumPortlet = uiTopicContainer.getAncestorOfType(UIForumPortlet.class);
      if (topics.size() > 0) {
        try {
          uiTopicContainer.getForumService().modifyTopic(topics, Utils.WAITING);
        } catch (Exception e) {
          warning("UITopicContainer.msg.fail-set-unwaiting-topic", false);
          event.getSource().log.debug("Failed to set unwaiting topics", e);
        }
      } else {
        UIPageListTopicUnApprove pageListTopicUnApprove = uiTopicContainer.openPopup(UIPageListTopicUnApprove.class, "PageListTopicWaiting", 760, 450);
        pageListTopicUnApprove.setId("UIPageListTopicWaiting");
        pageListTopicUnApprove.setTypeApprove(Utils.WAITING);
        pageListTopicUnApprove.setUpdateContainer(uiTopicContainer.categoryId, uiTopicContainer.forumId);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class SetOrderByActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String path) throws Exception {
      uiTopicContainer.strOrderBy = ForumUtils.getOrderBy(uiTopicContainer.strOrderBy, path);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class AddBookMarkActionListener extends BaseEventListener<UITopicContainer> {
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

  static public class AddWatchingActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String path) throws Exception {
      if (path.equals("forum")) {
        path = uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId;
      } else {
        path = uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId + ForumUtils.SLASH + path;
      }
      uiTopicContainer.addWatch(path, uiTopicContainer.userProfile);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class UnWatchActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, String path) throws Exception {
      if (path.equals("forum")) {
        path = uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId;
      } else {
        path = uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId + ForumUtils.SLASH + path;
      }
      uiTopicContainer.unWatch(path, uiTopicContainer.userProfile);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTopicContainer);
    }
  }

  static public class AdvancedSearchActionListener extends EventListener<UITopicContainer> {
    public void execute(Event<UITopicContainer> event) throws Exception {
      UITopicContainer uiForm = event.getSource();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.updateIsRendered(ForumUtils.FIELD_SEARCHFORUM_LABEL);
      forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(ForumUtils.FIELD_EXOFORUM_LABEL);
      UISearchForm searchForm = forumPortlet.getChild(UISearchForm.class);
      searchForm.setUserProfile(forumPortlet.getUserProfile());
      searchForm.setPath(uiForm.forum.getPath());
      searchForm.setSelectType(Utils.TOPIC);
      searchForm.setSearchOptionsObjectType(Utils.TOPIC);
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class BanIpForumToolsActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiTopicContainer, final String objectId) throws Exception {
      UIBanIPForumManagerForm ipForumManager = uiTopicContainer.openPopup(UIBanIPForumManagerForm.class, "BanIPForumManagerForm", 430, 500);
      ipForumManager.setForumId(uiTopicContainer.categoryId + ForumUtils.SLASH + uiTopicContainer.forumId);
    }
  }

  static public class RSSActionListener extends BaseEventListener<UITopicContainer> {
    public void onEvent(Event<UITopicContainer> event, UITopicContainer uiForm, final String forumId) throws Exception {
      if (!uiForm.userProfile.getUserId().equals(UserProfile.USER_GUEST)) {
        uiForm.getForumService().addWatch(-1, forumId, null, uiForm.userProfile.getUserId());
      }
    }
  }
}
