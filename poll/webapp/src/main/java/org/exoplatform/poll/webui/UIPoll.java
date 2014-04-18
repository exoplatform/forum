/***************************************************************************
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.poll.webui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIForumCheckBoxInput;
import org.exoplatform.forum.common.webui.UIPollRadioBoxInput;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.poll.Utils;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollNodeTypes;
import org.exoplatform.poll.webui.popup.UIPollForm;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/poll/webui/UIPoll.gtmpl",
    events = {
        @EventConfig(listeners = UIPoll.VoteActionListener.class),
        @EventConfig(listeners = UIPoll.EditPollActionListener.class),
        @EventConfig(listeners = UIPoll.RemovePollActionListener.class, confirm = "UIPoll.msg.confirm-RemovePoll"),
        @EventConfig(listeners = UIPoll.ClosedPollActionListener.class),
        @EventConfig(listeners = UIPoll.VoteAgainPollActionListener.class) 
    }
)
public class UIPoll extends BasePollForm {
  private final String        POLL_OPTION_ID     = "option";

  private final static String POLL_OPTION_VALUE = "pollOption";
  
  private Poll         poll_;

  private String       pollId, userId;

  private boolean      isAgainVote    = false;

  private boolean      isEditPoll     = false;

  private boolean      hasPermission  = true;

  private boolean      isAdmin        = false;

  private String[] dateUnit      = new String[] { "Never", "Closed", "day(s)", "hour(s)", "minutes" };

  public UIPoll() {
    dateUnit = new String[] { getLabel("Never"), getLabel("Closed"), getLabel("day"), getLabel("hour"), getLabel("minutes") };
  }

  public void setPollId() throws Exception {
    UIPollPortlet pollPortlet = getAncestorOfType(UIPollPortlet.class);
    isAdmin = pollPortlet.isAdmin();
    userId = pollPortlet.getUserId();

    //By default, admin chose what poll to display but when access poll by url,
    //we check if it's a correct poll's id then display this poll
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    String url = ((HttpServletRequest) portalContext.getRequest()).getRequestURL().toString();
    String id = url.substring(url.lastIndexOf("/") + 1);
    Poll poll = getPollService().getPoll(id);
    if (poll != null) {
      updatePollById(id);
      return;
    }
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    pollId = portletPref.getValue(Utils.POLL_ID_SHOW, StringUtils.EMPTY);
    if (Utils.isEmpty(pollId)) {
      List<String> list = getPollService().getPollSummary(null).getPollId();
      if (!list.isEmpty()) {
        pollId = list.iterator().next();
      }
    }
    this.isEditPoll = true;
  }

  public boolean hasUserInGroup(String groupId, String userId) throws Exception {
    try {
      OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
      for (Object object : organizationService.getGroupHandler().findGroupsOfUser(userId)) {
        if (((Group) object).getId().equals(groupId)) {
          return true;
        }
      }
    } catch (Exception e) {
      log.debug("Failed to check user permission by OrganizationService !", e);
    }
    return false;
  }

  private boolean checkPermission() throws Exception {
    String path = poll_.getParentPath();
    if (path.indexOf(PollNodeTypes.APPLICATION_DATA) > 0) {
      String group = path.substring(path.indexOf("/", 3), path.indexOf(PollNodeTypes.APPLICATION_DATA) - 1);
      hasPermission = hasUserInGroup(group, userId);
    } else {
      hasPermission = true;
    }
    return hasPermission;
  }

  public void updateFormPoll(Poll poll) throws Exception {
    if (poll.getIsClosed())
      poll.setExpire(Utils.getExpire(-1, poll.getModifiedDate(), dateUnit));
    else
      poll.setExpire(Utils.getExpire(poll.getTimeOut(), poll.getModifiedDate(), dateUnit));
    poll_ = poll;
    this.isEditPoll = false;
    checkPermission();
  }

  public void updatePollById(String pollId) throws Exception {
    this.pollId = pollId;
    this.isEditPoll = true;
  }

  private void removeChilren() {
    List<UIComponent> children = getChildren();
    List<String> childrenIds = new ArrayList<String>(children.size());
    for (UIComponent child : children) {
      if (child instanceof UIPollRadioBoxInput || child instanceof UIForumCheckBoxInput) {
        childrenIds.add(child.getId());
      }
    }
    for (String childId : childrenIds) {
      removeChildById(childId);
    }
  }

  private void init() throws Exception {
    removeChilren();
    if (poll_ != null) {
      if (!poll_.getIsMultiCheck()) {
        List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
        for (int i = 0; i < poll_.getOption().length; ++i) {
          options.add(new SelectItemOption<String>(poll_.getOption()[i], getOptionId(POLL_OPTION_VALUE, i)));
        }
        UIPollRadioBoxInput input = new UIPollRadioBoxInput(POLL_OPTION_ID, POLL_OPTION_ID, options);
        input.setAlign(1);
        addUIFormInput(input);
      } else {
        String[] options = poll_.getOption();
        for (int i = 0; i < options.length; i++) {
          UIForumCheckBoxInput checkBoxInput = new UIForumCheckBoxInput(getOptionId(POLL_OPTION_ID, i), 
                                                                        getOptionId(POLL_OPTION_ID, i), options[i], false);
          addUIFormInput(checkBoxInput.setInTable(true));
        }
      }
    }
  }

  private String getOptionId(String prefix, int index) {
    return new StringBuffer(prefix).append(index).toString();
  }

  private Poll getPoll() throws Exception {
    if (isEditPoll || poll_ == null) {
      poll_ = getPollService().getPoll(pollId);
      if (poll_ != null) {
        checkPermission();
        if (poll_.getIsClosed())
          poll_.setExpire(Utils.getExpire(-1, poll_.getModifiedDate(), dateUnit));
        else
          poll_.setExpire(Utils.getExpire(poll_.getTimeOut(), poll_.getModifiedDate(), dateUnit));
      } else
        hasPermission = false;
    }
    this.init();
    return poll_;
  }

  protected boolean getIsEditPoll() {
    return isEditPoll;
  }

  public void setEditPoll(boolean isEditPoll) {
    this.isEditPoll = isEditPoll;
  }

  protected boolean getCanViewEditMenu() {
    return isAdmin;
  }

  protected boolean isGuestPermission() throws Exception {
    if (poll_.getIsClosed())
      return true;
    if (Utils.isEmpty(userId))
      return true;
    if (poll_.getTimeOut() > 0) {
      Date today = new Date();
      if ((today.getTime() - this.poll_.getCreatedDate().getTime()) >= poll_.getTimeOut() * 86400000)
        return true;
    }
    if (this.isAgainVote) {
      return false;
    }
    String[] userVotes = poll_.getUserVote();
    for (String string : userVotes) {
      string = string.substring(0, string.indexOf(org.exoplatform.poll.service.Utils.COLON));
      if (string.equalsIgnoreCase(userId))
        return true;
    }
    return false;
  }

  protected String[] getInfoVote() throws Exception {
    Poll poll = poll_;
    String[] voteNumber = poll.getVote();
    String[] userVotes = poll.getUserVote();
    long size = 0, temp = 1;
    if (!poll.getIsMultiCheck()) {
      size = userVotes.length;
    } else {
      for (int i = 0; i < userVotes.length; i++) {
        size += userVotes[i].split(org.exoplatform.poll.service.Utils.COLON).length - 1;
      }
    }
    temp = size;
    if (size == 0)
      size = 1;
    int l = voteNumber.length;
    String[] infoVote = new String[(l + 1)];
    for (int j = 0; j < l; j++) {
      String string = voteNumber[j];
      double tmp = Double.parseDouble(string);
      double k = (tmp * size) / 100;
      int t = (int) Math.round(k);
      string = String.valueOf((double) t * 100 / size);
      infoVote[j] = string + org.exoplatform.poll.service.Utils.COLON + String.valueOf(t);
    }
    infoVote[l] = String.valueOf(temp);
    if (poll.getIsMultiCheck()) {
      infoVote[l] = String.valueOf(userVotes.length);
    }
    return infoVote;
  }

  static public class VoteActionListener extends EventListener<UIPoll> {
    public void execute(Event<UIPoll> event) throws Exception {
      UIPoll topicPoll = event.getSource();
      StringBuffer values = new StringBuffer();
      List<UIComponent> children = topicPoll.getChildren();
      topicPoll.poll_ = topicPoll.getPollService().getPoll(topicPoll.pollId);
      int maxOption = topicPoll.poll_.getOption().length;
      boolean isFailed = false;
      int i = 0;
      if (topicPoll.poll_.getIsMultiCheck() == false) {
        for (UIComponent child : children) {
          if (child instanceof UIPollRadioBoxInput) {
            //
            String indexChecked = ((UIPollRadioBoxInput) child).getValue();
            if (CommonUtils.isEmpty(indexChecked) == false) {
              values.append(indexChecked.replaceFirst(POLL_OPTION_VALUE, CommonUtils.EMPTY_STR));
            } else {
              isFailed = true;
            }

            //
            break;
          }
        }
        // multichoice when vote
      } else {
        for (UIComponent child : children) {
          if (child instanceof UIForumCheckBoxInput) {
            if (((UIForumCheckBoxInput) child).isChecked()) {
              if (i >= maxOption) {
                isFailed = true;
                break;
              }
              values.append(((values.length() > 0) ? org.exoplatform.poll.service.Utils.COLON : StringUtils.EMPTY) + String.valueOf(i));
            }
            ++i;
          }
        }
      }
      if(!isFailed) {
        if (!Utils.isEmpty(values.toString())) {
          Poll poll = org.exoplatform.poll.service.Utils.calculateVote(topicPoll.poll_, topicPoll.userId, values.toString());
          topicPoll.getPollService().savePoll(poll, false, true);
        } else {
          topicPoll.warning("UIPoll.msg.notCheck", false);
        }
      } else {
        topicPoll.warning("UIPoll.msg.voteFailed", false);
      }
      topicPoll.isAgainVote = false;
      event.getRequestContext().addUIComponentToUpdateByAjax(topicPoll.getParent());
    }
  }

  static public class EditPollActionListener extends BaseEventListener<UIPoll> {
    public void onEvent(Event<UIPoll> event, UIPoll topicPoll, final String objectId) throws Exception {
      UIPollPortlet pollPortlet = topicPoll.getAncestorOfType(UIPollPortlet.class);
      UIPopupAction popupAction = pollPortlet.getChild(UIPopupAction.class);
      UIPopupContainer popupContainer = popupAction.createUIComponent(UIPopupContainer.class, null, null);
      UIPollForm pollForm = popupContainer.getChild(UIPollForm.class);
      if (pollForm == null)
        pollForm = popupContainer.addChild(UIPollForm.class, null, null);
      popupContainer.setId("UIEditPollForm");
      topicPoll.isEditPoll = true;
      topicPoll.poll_ = topicPoll.getPoll();
      pollForm.setUpdatePoll(topicPoll.poll_, true);
      popupAction.activate(popupContainer, 655, 455, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  static public class RemovePollActionListener extends EventListener<UIPoll> {
    public void execute(Event<UIPoll> event) throws Exception {
      UIPoll topicPoll = event.getSource();
      topicPoll.getPollService().removePoll(topicPoll.pollId);
      topicPoll.removeChilren();
      UIPollPortlet pollPortlet = topicPoll.getAncestorOfType(UIPollPortlet.class);
      topicPoll.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(pollPortlet);
    }
  }

  static public class VoteAgainPollActionListener extends EventListener<UIPoll> {
    public void execute(Event<UIPoll> event) throws Exception {
      UIPoll topicPoll = event.getSource();
      topicPoll.isAgainVote = true;
      topicPoll.isEditPoll = true;
      topicPoll.poll_ = topicPoll.getPoll();
      event.getRequestContext().addUIComponentToUpdateByAjax(topicPoll);
    }
  }

  static public class ClosedPollActionListener extends BaseEventListener<UIPoll> {
    public void onEvent(Event<UIPoll> event, UIPoll topicPoll, final String id) throws Exception {
      if (id.equals("true")) {
        topicPoll.poll_.setIsClosed(false);
        topicPoll.poll_.setTimeOut(0);
      } else {
        topicPoll.poll_.setIsClosed(!topicPoll.poll_.getIsClosed());
      }
      topicPoll.getPollService().setClosedPoll(topicPoll.poll_);
      topicPoll.isEditPoll = true;
      topicPoll.isAgainVote = false;
      event.getRequestContext().addUIComponentToUpdateByAjax(topicPoll);
    }
  }
}
