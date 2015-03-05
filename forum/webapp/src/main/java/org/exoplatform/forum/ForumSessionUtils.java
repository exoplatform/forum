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
package org.exoplatform.forum;

import java.util.List;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.user.CommonContact;
import org.exoplatform.forum.common.user.ContactProvider;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ForumSessionUtils {

  static private final Log   LOG            = ExoLogger.getLogger(ForumSessionUtils.class);

  public final static String DEFAULT_AVATAR = "/eXoSkin/skin/images/system/UserAvtDefault.png";

  /**
   * Create an avatar link for user. 
   * Firstly, the function tries to load avatar resource from user profile of forum.
   * if the resource is not found, try to get it from {@link ContactProvider}
   * else, return default URL: <a>/eXoSkin/skin/images/Default/social/skin/ShareImages/UserAvtDefault.png</a>.
   * @param userName
   * @param forumService
   * @return
   */
  public static String getUserAvatarURL(String userName, ForumService forumService) {
    String url = null;
    try {
      if (forumService == null) {
        forumService = CommonUtils.getComponent(ForumService.class);
      }
      ForumAttachment attachment = forumService.getUserAvatar(userName);
      if (attachment != null) {
        url = CommonUtils.getImageUrl(attachment.getPath()) + "?size=" + attachment.getSize();
      }
    } catch (Exception e) {
      LOG.warn(String.format("can not load avatar of [%s] as file resource", userName));
      LOG.debug(e);
    }
    if (CommonUtils.isEmpty(url)) {
      url = getPersonalContact(userName).getAvatarUrl();
    }
    return CommonUtils.isEmpty(url) ? DEFAULT_AVATAR : url;
  }

  public static CommonContact getPersonalContact(String userId) {
    try {
      if (userId.indexOf(Utils.DELETED) > 0)
        return new CommonContact();
      ContactProvider provider = CommonUtils.getComponent(ContactProvider.class);
      return provider.getCommonContact(userId);
    } catch (Exception e) {
      return new CommonContact();
    }
  }

  public static String getBreadcumbUrl(String link, String componentName, String actionName, String objectId) throws Exception {
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    String url = portalContext.getRequest().getRequestURL().toString();
    url = url.substring(0, url.indexOf(ForumUtils.SLASH, 8));
    link = link.replaceFirst(componentName, "UIBreadcumbs").replaceFirst(actionName, "ChangePath").replace("pathId", objectId).replaceAll("&amp;", "&");
    return (url + link);
  }
  
  public static List<Forum> getForumsOfCategory(String categoryId, UserProfile userProfile) throws Exception {
    return CommonUtils.getComponent(ForumService.class).getForums(new ForumFilter(categoryId, true).userId(userProfile.getUserId()));
  }
}
