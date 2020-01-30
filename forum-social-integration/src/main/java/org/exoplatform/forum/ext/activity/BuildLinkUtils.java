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
package org.exoplatform.forum.ext.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
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
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

public class BuildLinkUtils {
  public static final String SPACES_GROUP = SpaceUtils.SPACE_GROUP.substring(1);

  public static final String CATEGORY     = "category";

  private static final Map<String, String> forumLinkData = new ConcurrentHashMap<String, String>();

  public enum PORTLET_INFO {
    FORUM("ForumPortlet", "Forum Portlet", "forum"),
    POLL("PollPortlet", "Polls Portlet", "poll"),
    ANSWER("AnswersPortlet", "Answers Portlet", "answers");
    final private String name, description, pageId;

    PORTLET_INFO(String name, String description, String pageId) {
      this.name = name;
      this.pageId = pageId;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getPageId() {
      return pageId;
    }

    public String getDescription() {
      return description;
    }
    
  }

  private static String getType(String objectId) {
    if (objectId.indexOf(Utils.POST) == 0)
      return Utils.POST;
    if (objectId.indexOf(Utils.TOPIC) == 0)
      return Utils.TOPIC;
    if (objectId.indexOf(Utils.FORUM) == 0)
      return Utils.FORUM;
    if (objectId.indexOf(Utils.POLL) == 0)
      return Utils.POLL;
    return "";
  }

  /**
   * Check object to rendering is on space or not
   * 
   * @param objectId The objectId 
   * + if objectId contain the id of the group on space then return true
   * + if objectId is id of the forum on space then return true
   * + if objectId is id of the forum category on space then return true
   * + if objectId is id of the answer category on space then return true
   * @return
   */
  private static boolean isInSpace(String objectId) {
    if (objectId.indexOf(SpaceUtils.SPACE_GROUP) >= 0)
      return true;
    if (objectId.indexOf(Utils.FORUM_SPACE_ID_PREFIX) == 0)
      return true;
    if (objectId.indexOf(Utils.CATEGORY_SPACE_ID_PREFIX) == 0)
      return true;
    return false;
  }
  
  private static String getGroupId(String parentObjectId, PORTLET_INFO portletInfo) {
    StringBuffer spaceGroupId = new StringBuffer();
    if (parentObjectId.indexOf(SpaceUtils.SPACE_GROUP) == 0) {
      spaceGroupId.append(parentObjectId);
    } else {
      spaceGroupId.append("/").append(SPACES_GROUP).append("/");
      spaceGroupId.append(parentObjectId.replace(Utils.FORUM_SPACE_ID_PREFIX, ""));
    }
    return spaceGroupId.toString();
  }

  public static String buildLink(String parentObjectId, String objectId, PORTLET_INFO portletInfo) {
    return buildLink(parentObjectId, objectId, portletInfo, CommonsUtils.getCurrentSite().getName());
  }

  public static String buildLink(String parentObjectId, String objectId, PORTLET_INFO portletInfo, String siteName) {
    if(CommonUtils.isEmpty(objectId)) {
      return "#";
    }
    try {
      String link = "";
      String objectType = getType(objectId);
      //
      if (isInSpace(parentObjectId)) {
        link = buildSpaceLink(getGroupId(parentObjectId, portletInfo), objectType, objectId, portletInfo);
      } else {
        //caching the topic link
        String forumLink = getPublicForumLink(getType(objectType));
        
        if (PORTLET_INFO.FORUM.equals(portletInfo) && forumLink != null && forumLink.length() > 0) {
          return new StringBuffer(forumLink).append(buildLink_(objectType, objectId, portletInfo)).toString();
        }
        
        PortalRequestContext prc = Util.getPortalRequestContext();
        if (!CommonUtils.isEmpty(siteName) && !prc.getSiteKey().getName().equals(siteName)) {
          SiteKey siteKey = SiteKey.portal(siteName);
          String nodeURI = getSiteName(siteKey, portletInfo);

          //
          if (!CommonUtils.isEmpty(nodeURI)) {
            link = new StringBuffer(getSiteHomeURL(siteName, nodeURI)).append(buildLink_(objectType, objectId, portletInfo)).toString();
          }
        } else {
          UserPortal userPortal = prc.getUserPortal();
          UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
          UserNode rootNode = userPortal.getNode(userNav, Scope.ALL, null, null);

          UserNode portletNode = getPortletNode(rootNode, portletInfo);
          if (portletNode != null) {
            forumLink = getNodeURL(portletNode);
            //
            setPublicForumLink(getType(objectType), forumLink);
            link = new StringBuffer(forumLink).append(buildLink_(objectType, objectId, portletInfo)).toString();
          }
        }
        
      }
      //
      return link;
    } catch (Exception ex) {
      return "";
    }
  }
  
  /**
   * Try to find the node that contains the application match with the id
   * 
   * @param rootNode
   * @param id id of the portlet to find
   * @return the usernode associated to the app's id
   */
  private static UserNode getPortletNode(UserNode rootNode, PORTLET_INFO portletInfo) {
    DataStorage dataStorage = CommonsUtils.getService(DataStorage.class);
    for (UserNode node : rootNode.getChildren()) {
      Page page;
      try {
        page = dataStorage.getPage(node.getPageRef().format());
      } catch (Exception e) {
        continue;
      }
      List<ComponentData> dataModels = new ArrayList<ComponentData>();
      for (ModelObject model : page.getChildren()) {
        ModelData modelData = model.build();
        if (modelData instanceof ComponentData) {
          dataModels.add((ComponentData) modelData);
        }
      }
      if (getUserNode(dataModels, portletInfo)) {
        return node;
      }

      if (node.getChildrenSize() > 0) {
        UserNode child = getPortletNode(node, portletInfo);
        if (child != null) return child;
      }
    }
    return null;
  }

  private static Boolean getUserNode(List<ComponentData> models, PORTLET_INFO portletInfo) {
    for (ModelData modelData : models) {
      if (modelData instanceof ApplicationData) {
        ApplicationData<?> applicationData = (ApplicationData<?>) modelData;
        if ((applicationData.getDescription() != null 
            && applicationData.getDescription().equals(portletInfo.getDescription()))
            || (applicationData.getTitle() != null
            && applicationData.getTitle().equals(portletInfo.getDescription()))) {
          return true;
        }
      } else if (modelData instanceof ContainerData) {
        ContainerData containerData = (ContainerData) modelData;
        List<ComponentData> models_ = containerData.getChildren();
        return getUserNode(models_, portletInfo);
      }
    }
    return false;
  }

  private static String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }

  private static String buildLink_(String objectType, String objectId, PORTLET_INFO portletInfo) {
    StringBuffer buffer = new StringBuffer();
    return buffer.append("/").append(objectType).append("/").append(objectId).toString();
  }

  private static String buildSpaceLink(String spaceGroupId, String objectType, String objectId, PORTLET_INFO portletInfo) throws Exception {
    String nodeURI = getSiteName(SiteKey.group(spaceGroupId), portletInfo);
    if (!CommonUtils.isEmpty(nodeURI)) {      
      Space space = ForumActivityUtils.getSpaceService().getSpaceByGroupId(spaceGroupId);
      String spaceLink = org.exoplatform.social.webui.Utils.getSpaceHomeURL(space);
      StringBuffer buffer = new StringBuffer(spaceLink).append("/").append(nodeURI);
      return buffer.append(buildLink_(objectType, objectId, portletInfo)).toString();
    }

    return CommonUtils.EMPTY_STR;
  }

  private static String getSiteName(SiteKey siteKey, PORTLET_INFO portletInfo) {
    NavigationService navService = CommonsUtils.getService(NavigationService.class);
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
        if (portletInfo.getPageId().indexOf(child.getName()) == 0 || 
            child.getName().indexOf(portletInfo.getName()) >= 0) {
          return child.getName();
        }
      }
    }
    return CommonUtils.EMPTY_STR;
  }

  private static String getSiteHomeURL(String siteName, String nodeURI) {
    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, siteName, nodeURI);

    return nodeURL.setResource(resource).toString();
  }

  private static String getPublicForumLink(String portletType) {
    String currentRepositoryName = SessionProviderService.getRepository().getConfiguration().getName();
    return forumLinkData.get(portletType + "_" + currentRepositoryName);
  }

  private static void setPublicForumLink(String portletType, String forumLink) {
    String currentRepositoryName = SessionProviderService.getRepository().getConfiguration().getName();
    forumLinkData.put(portletType + "_" + currentRepositoryName, forumLink);
  }
}
