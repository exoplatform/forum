package org.exoplatform.forum.service.ws;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * Handles REST request to process Forum.
 * 
 * @anchor ForumWebservice
 * 
 */
@Path("ks/forum")
public class ForumWebservice implements ResourceContainer {

  protected final static String JSON_CONTENT_TYPE = MediaType.APPLICATION_JSON;

  final public static String    APP_TYPE          = "forum".intern();

  private String                strQuery;

  private List<BanIP>           ipsToJson         = new ArrayList<BanIP>();

  private static Log            log               = ExoLogger.getLogger(ForumWebservice.class);

  private static final CacheControl         cc;
  static {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }
  
  public ForumWebservice() {
  }

  /**
   * Gets recent posts for users which are limited by the maximum number of posts rendered.
   * 
   * @param maxcount The maximum number of posts rendered in the gadget.
   * @param sc The security context which gets the information of the current user.
   * @param uriInfo The URI information which gets the information of the current user.
   * 
   * @anchor ForumWebservice.getMessage
   * 
   * @return JSON data content list of the recent posts for user.
   * 
   * @throws Exception the exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("getmessage/{maxcount}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMessage(@PathParam("maxcount") int maxcount, @Context SecurityContext sc,
                                                                  @Context UriInfo uriInfo) throws Exception {
    try {
      String userName = getUserId(sc, uriInfo);
      MessageBean data = getNewPosts(userName, maxcount);
      return Response.ok(data, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      log.debug("Failed to get new post by user.");
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Gets recent public posts which are limited by the maximum number of posts rendered.
   * 
   * @param maxcount The maximum number of posts rendered in the gadget.
   * 
   * @anchor ForumWebservice.getPulicMessage
   * 
   * @return JSON data content list of the recent public posts.
   * 
   * @throws Exception the exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("getpublicmessage/{maxcount}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPulicMessage(@PathParam("maxcount") int maxcount) throws Exception {
    MessageBean data = getNewPosts(null, maxcount);
    return Response.ok(data, MediaType.APPLICATION_JSON).cacheControl(cc).build();
  }
  
  /**
   * Filters banned IPs in all forums of eXo Platform.
   * 
   * @param str The keyword to search the banned IP from the IPs list.
   * 
   * @anchor ForumWebservice.filterIps
   * 
   * @return The response is JSON data.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("filter/{strIP}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterIps(@PathParam("strIP") String str) throws Exception {
    ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    if (str.equals("all")) {
      ipsToJson.clear();
      List<String> banIps = forumService.getBanList();
      for (String ip : banIps) {
        ipsToJson.add(new BanIP(ip));
      }
    } else if (!str.equals(strQuery)) {
      ipsToJson.clear();
      List<String> banIps = forumService.getBanList();
      for (String ip : banIps) {
        if (ip.startsWith(str))
          ipsToJson.add(new BanIP(ip));
      }
    }
    strQuery = str;
    return Response.ok(new BeanToJsons<BanIP>(ipsToJson), JSON_CONTENT_TYPE).cacheControl(cc).build();
  }

  /**
   * Filters banned IPs in a given forum.
   * 
   * @param forumId The forum Id.
   * @param str The keyword to search the banned IP from the IPs list.
   * 
   * @anchor ForumWebservice.filterIpBanForum
   * 
   * @return The response is JSON data.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("filterIpBanforum/{strForumId}/{strIP}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterIpBanForum(@PathParam("strForumId") String forumId, @PathParam("strIP") String str) throws Exception {
    ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    if (str.equals("all")) {
      ipsToJson.clear();
      List<String> banIps = forumService.getForumBanList(forumId);
      for (String ip : banIps) {
        ipsToJson.add(new BanIP(ip));
      }
    } else if (!str.equals(strQuery)) {
      ipsToJson.clear();
      List<String> banIps = forumService.getForumBanList(forumId);
      for (String ip : banIps) {
        if (ip.startsWith(str))
          ipsToJson.add(new BanIP(ip));
      }
    }
    strQuery = str;
    return Response.ok(new BeanToJsons<BanIP>(ipsToJson), JSON_CONTENT_TYPE).cacheControl(cc).build();
  }

  /**
   * Filters tags of the forum by name.
   * 
   * @param str The keyword to search the tag name in a topic.
   * @param userAndTopicId The Id of users and topics which are separated by a comma.
   * 
   * @anchor ForumWebservice.filterTagNameForum
   * 
   * @return The response is JSON data.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("filterTagNameForum/{userAndTopicId}/{strTagName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterTagNameForum(@PathParam("strTagName") String str, @PathParam("userAndTopicId") String userAndTopicId) throws Exception {
    ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    if (str.equals(" ")) {
      ipsToJson.clear();
    } else if (str.equals("onclickForm")) {
      ipsToJson.clear();
      List<String> banIps = forumService.getTagNameInTopic(userAndTopicId);
      for (String ip : banIps) {
        ipsToJson.add(new BanIP(ip));
      }
    } else {
      ipsToJson.clear();
      List<String> banIps = forumService.getAllTagName(str, userAndTopicId);
      for (String ip : banIps) {
        if (ip.startsWith(str))
          ipsToJson.add(new BanIP(ip));
      }
    }
    return Response.ok(new BeanToJsons<BanIP>(ipsToJson), JSON_CONTENT_TYPE).cacheControl(cc).build();
  }

  /**
   * Gets RSS feeds of a category, forum or topic.
   *
   * @param resourceid The object Id to get RSS feeds.
   * 
   * @anchor ForumWebservice.viewrss
   * 
   * @return The response is xml data which contains returned RSS.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("rss/{resourceid}")
  @Produces(MediaType.APPLICATION_XML)
  public Response viewrss(@PathParam("resourceid") String resourceid) throws Exception {
    try {
      ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
      InputStream is = forumService.createForumRss(resourceid, "http://www.exoplatform.com");
      return Response.ok(is, MediaType.APPLICATION_XML).cacheControl(cc).build();
    } catch (Exception e) {
      log.trace("\nView RSS fail: " + e.getMessage() + "\n" + e.getCause());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Gets RSS feeds of a user.
   * 
   * @param resourceid The user Id to get RSS feeds.
   * 
   * @anchor ForumWebservice.userrss
   * 
   * @return The response is xml data which contains returned RSS.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("rss/user/{resourceid}")
  @Produces(MediaType.TEXT_XML)
  public Response userrss(@PathParam("resourceid") String resourceid) throws Exception {
    try {
      ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
      InputStream is = forumService.createUserRss(resourceid, "http://www.exoplatform.com");
      return Response.ok(is, MediaType.APPLICATION_XML).cacheControl(cc).build();
    } catch (Exception e) {
      log.trace("\nGet UserRSS fail: ", e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Filters forums by name and returned results are limited by the _maxSize_ parameter.
   * 
   * @param forumName The keyword input to search for forums by name.
   * @param maxSize The maximum returned results.
   * @param sc The security context which gets the information of the current user.
   * @param uriInfo The URI information which gets the information of the current user.
   *
   * @anchor ForumWebservice.filterForum
   * 
   * @return The response is JSON data which contains forums filtered by name.
   * 
   * @throws Exception
   * 
   * @LevelAPI Platform
   */
  @GET
  @Path("filterforum/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterForum(@QueryParam("name") String forumName, 
                               @QueryParam("maxSize") String maxSize,
                               @Context SecurityContext sc,
                               @Context UriInfo uriInfo) throws Exception {
    try {
      List<CategoryFilter> categoryFilters = new ArrayList<CategoryFilter>();
      if(!Utils.isEmpty(forumName)) {
        ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
        String userName = getUserId(sc, uriInfo);
        int maxSize_ = 0;
        if(!Utils.isEmpty(maxSize)) {
          try {
            maxSize_ = Integer.parseInt(maxSize.trim());
          } catch (NumberFormatException e) {
            maxSize_ = 0;
          }
        }
        categoryFilters.addAll(forumService.filterForumByName(forumName, userName, maxSize_));
        Collections.sort(categoryFilters, new Utils.CategoryNameComparator());
      }
      return Response.ok(categoryFilters, JSON_CONTENT_TYPE).cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private MessageBean getNewPosts(String userName, int maxcount) throws Exception {
    ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    List<Post> list = forumService.getRecentPostsForUser(userName, maxcount);
    if (list != null) {
      for (Post post : list) {
        post.setLink(post.getLink() + "/" + post.getId());
        post.setAttachments(new ArrayList<>());
      }
    }
    MessageBean data = new MessageBean();
    data.setData(list);
    return data;
  }
  
  private String getUserId(SecurityContext sc, UriInfo uriInfo) {
    try {
      return sc.getUserPrincipal().getName();
    } catch (Exception e) {
      log.debug("Failed to get user id", e);
      return null;
    }
  }
}
