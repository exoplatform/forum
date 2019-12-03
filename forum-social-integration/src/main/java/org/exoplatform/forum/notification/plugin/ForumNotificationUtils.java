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

import java.util.Map;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.ext.activity.BuildLinkUtils.PORTLET_INFO;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.model.Space;

public class ForumNotificationUtils {
  public final static ArgumentLiteral<String> ACTIVITY_ID = new ArgumentLiteral<String>(String.class, "activityId");
  public final static ArgumentLiteral<String> POSTER = new ArgumentLiteral<String>(String.class, "poster");
  public final static ArgumentLiteral<String> SENDER = new ArgumentLiteral<String>(String.class, "sender");
  public final static ArgumentLiteral<ExoSocialActivity> ACTIVITY = new ArgumentLiteral<ExoSocialActivity>(ExoSocialActivity.class, "activity");
  public final static ArgumentLiteral<Profile> PROFILE = new ArgumentLiteral<Profile>(Profile.class, "profile");
  public final static ArgumentLiteral<Space> SPACE = new ArgumentLiteral<Space>(Space.class, "space");
  public final static ArgumentLiteral<String> REMOTE_ID = new ArgumentLiteral<String>(String.class, "remoteId");
  public final static ArgumentLiteral<String> SPACE_ID = new ArgumentLiteral<String>(String.class, "spaceId");
  public final static ArgumentLiteral<String> PRETTY_NAME = new ArgumentLiteral<String>(String.class, "prettyName");
  public final static ArgumentLiteral<Relationship> RELATIONSHIP = new ArgumentLiteral<Relationship>(Relationship.class, "relationship");
  public final static ArgumentLiteral<String> RELATIONSHIP_ID = new ArgumentLiteral<String>(String.class, "relationshipId");
  
  public static String buildTopicLink(ExoSocialActivity activity) {

    String topicId = getActivityParamValue(activity, ForumActivityBuilder.TOPIC_ID_KEY);
    String forumId = getActivityParamValue(activity, ForumActivityBuilder.FORUM_ID_KEY);
    try {
      return CommonsUtils.getCurrentDomain() + BuildLinkUtils.buildLink(forumId, topicId, PORTLET_INFO.FORUM);
    } catch (Exception ex) {
      return "";
    }
  }

  public static String getActivityParamValue(ExoSocialActivity activity, String key) {
    String value = null;
    Map<String, String> params = activity.getTemplateParams();
    if (params != null) {
      value = params.get(key);
    }
    return value != null ? value : "";
  }
}
