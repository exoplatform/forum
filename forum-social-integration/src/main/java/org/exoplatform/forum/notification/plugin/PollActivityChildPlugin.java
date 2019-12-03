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
package org.exoplatform.forum.notification.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class PollActivityChildPlugin extends AbstractNotificationChildPlugin {

  public static final String ID = "ks-poll:spaces";
  private ExoSocialActivity activity = null;

  private ForumService forumService;

  public PollActivityChildPlugin(InitParams initParams, ForumService forumService) {
    super(initParams);
    this.forumService = forumService;
  }

  @Override
  public String makeContent(NotificationContext ctx) {
    try {
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = new TemplateContext(ID, language);

      String activityId = notification.getValueOwnerParameter(ForumNotificationUtils.ACTIVITY_ID.getKey());
      activity = ForumActivityUtils.getActivityManager().getActivity(activityId);
      if (activity.isComment()) {
        activity = ForumActivityUtils.getActivityManager().getParentActivity(activity);
      }
      templateContext.put("ACTIVITY", activity.getTitle());
      templateContext.put("ACTIVITY_URL", CommonsUtils.getCurrentDomain() +
              forumService.getTopicByPath(activity.getTemplateParams().get("PollLink"), false).getLink());
      //
      String content = TemplateUtils.processGroovy(templateContext);
      return content;
    } catch (Exception e) {
      return (activity != null) ? activity.getTitle() : "";
    }
  }

  public String getActivityParamValue(String key) {
    return ForumNotificationUtils.getActivityParamValue(activity, key);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return false;
  }

}
