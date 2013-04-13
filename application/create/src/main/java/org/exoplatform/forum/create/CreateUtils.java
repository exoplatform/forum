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
package org.exoplatform.forum.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

public class CreateUtils {
  
  public static final String  SPACES_GROUP           = "spaces";

  public static final String  CATEGORY               = "category";

  private static final String FORUM_PAGE_NAGVIGATION = "forum";

  private static final String FORUM_PORTLET_NAME     = "ForumPortlet";

  public static List<String> getAllPortalNames() {
    UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                                                                                       .getComponentInstanceOfType(UserPortalConfigService.class);
    try {
      return dataStorage.getAllPortalNames();
    } catch (Exception e) {
      return new ArrayList<String>();
    }
  }

  public static String getCurrentPortalName() {
    UIPortal uiPortal = Util.getUIPortal();
    SiteKey siteKey = uiPortal.getSiteKey();
    if(siteKey.getType() == SiteType.PORTAL) {
      return siteKey.getName();
    }
    return SiteType.GROUP.name();
  }
  

  private static String getType(String categoryId, String forumId, String topicId) {
    if(!CommonUtils.isEmpty(topicId)) return Utils.TOPIC;
    if(!CommonUtils.isEmpty(forumId)) return Utils.FORUM;
    if(!CommonUtils.isEmpty(categoryId)) return CATEGORY;
    return "";
  }

  private static String getObjectId(String categoryId, String forumId, String topicId) {
    if(!CommonUtils.isEmpty(topicId)) return topicId;
    if(!CommonUtils.isEmpty(forumId)) return forumId;
    if(!CommonUtils.isEmpty(categoryId)) return categoryId;
    return "";
  }
  
  /**
   * 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @return
   */
  public static String buildLink(String categoryId, String forumId, String topicId) {
    return buildLink(categoryId, forumId, topicId, null);
  }

  /**
   * 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @return
   */
  public static String buildLink(String categoryId, String forumId, String topicId, String siteName) {
    try {
      String link = "";

      String objectType = getType(categoryId, forumId, topicId);
      String objectId = getObjectId(categoryId, forumId, topicId);
      //
      if (categoryId.indexOf(SPACES_GROUP) > 0 && !objectType.equals(CATEGORY)) {
        String prefixId = Utils.FORUM_SPACE_ID_PREFIX;
        String spaceGroupId = "/".concat(SPACES_GROUP).concat("/").concat(forumId.replaceFirst(prefixId, ""));
        link = buildSpaceLink(spaceGroupId, objectType, objectId);
      } else {
        PortalRequestContext prc = Util.getPortalRequestContext();

        if (!CommonUtils.isEmpty(siteName) && !prc.getSiteKey().getName().equals(siteName)) {
          SiteKey siteKey = SiteKey.portal(siteName);

          String nodeURI = getSiteName(siteKey);

          //
          if (!CommonUtils.isEmpty(nodeURI)) {
            String siteHomeLink = getSiteHomeURL(siteName, nodeURI);
            link = String.format("%s/%s/%s", siteHomeLink, objectType, objectId);
          }
        } else {
          UserPortal userPortal = prc.getUserPortal();
          UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
          UserNode userNode = userPortal.getNode(userNav, Scope.ALL, null, null);

          //
          UserNode forumNode = userNode.getChild(FORUM_PAGE_NAGVIGATION);
          if (forumNode != null) {
            String forumURI = getNodeURL(forumNode);
            link = String.format("%s/%s/%s", forumURI, objectType, objectId);
          }
        }
      }

      //
      return link;
    } catch (Exception ex) {
      return "";
    }
  }

  private static String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }

  /**
   * 
   * @param spaceGroupId
   * @param objectType
   * @param objectId
   * @return
   * @throws Exception
   */
  public static String buildSpaceLink(String spaceGroupId, String objectType, String objectId) throws Exception {

    String nodeURI = getSiteName(SiteKey.group(spaceGroupId));
    
    if (!CommonUtils.isEmpty(nodeURI)) {
      String spaceLink = getSpaceHomeURL(spaceGroupId);
      String objectLink = String.format("%s/%s/%s/%s", spaceLink, nodeURI, objectType, objectId);
      return objectLink;
    }

    return CommonUtils.EMPTY_STR;
  }

  public static String getSiteName(SiteKey siteKey) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    NavigationService navService = (NavigationService) container.getComponentInstance(NavigationService.class);
    NavigationContext nav = navService.loadNavigation(siteKey);
    NodeContext<NodeContext<?>> parentNodeCtx = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);

    if (parentNodeCtx.getSize() >= 1) {
      Collection<NodeContext<?>> children = parentNodeCtx.getNodes();
      if (siteKey.getType() == SiteType.GROUP) {
        children = parentNodeCtx.get(0).getNodes();
      }
      Iterator<NodeContext<?>> it = children.iterator();

      NodeContext<?> child = null;
      while (it.hasNext()) {
        child = it.next();
        if (FORUM_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(FORUM_PORTLET_NAME) >= 0) {
          break;
        }
      }
      return child.getName();
    }
    return CommonUtils.EMPTY_STR;
  }
  
  /**
   * 
   * @param portalName
   * @param nodeURI
   * @return
   */
  public static String getSiteHomeURL(String portalName, String nodeURI) {

    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, portalName, nodeURI);

    return nodeURL.setResource(resource).toString();
  }
  
  /**
   * Gets the space home url of a space.
   * 
   * @param spaceGroupId
   * @return
   * @since 4.0
   */
  public static String getSpaceHomeURL(String spaceGroupId) {
    String permanentSpaceName = spaceGroupId.split("/")[2];

    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, spaceGroupId, permanentSpaceName);

    return nodeURL.setResource(resource).toString();
  }
}
