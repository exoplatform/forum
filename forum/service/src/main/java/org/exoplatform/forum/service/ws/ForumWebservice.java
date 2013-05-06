package org.exoplatform.forum.service.ws;

import java.io.InputStream;
import java.net.URI;
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
 * Handles rest request to process Forum. It can be get and filtered, process with RSS are also provided.
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
   * Gets recent posts for user and limited by number post.
   * 
   * @param maxcount is max number post for render in gadget
   * @param sc is SecurityContext for get userId login when we use rest link to render gadget.
   * @param uriInfo is UriInfo for get userId login when we render gadget via gadgets service
   * 
   * @anchor ForumWebservice.getMessage
   * 
   * @return the response is json-data content list recent post for user.
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
   * Gets recent public post limited by number post.
   * 
   * @param maxcount is max number post for render in gadget
   * 
   * @anchor ForumWebservice.getPulicMessage
   * 
   * @return the response is json-data content list recent public post.
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
   * Filters ips.
   * 
   * @param str ip to filter.
   * 
   * @anchor ForumWebservice.filterIps
   * 
   * @return the response is json-data
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
   * Filters banned IP.
   * 
   * @param forumId id of forum
   * @param str banned ip to filter
   * 
   * @anchor ForumWebservice.filterIpBanForum
   * 
   * @return the response is json-data
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
   * Filters tag of forum by name.
   * 
   * @param str tag name to filter
   * @param userAndTopicId id of user and topic
   * 
   * @anchor ForumWebservice.filterTagNameForum
   * 
   * @return the response is json-data
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
   * Gets forum rss information.
   * 
   * @param resourceid source to get rss.
   * 
   * @anchor ForumWebservice.viewrss
   * 
   * @return the response is xml-data contain returned rss.
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
   * Gets user rss information.
   * 
   * @param resourceid source to get rss
   * 
   * @anchor ForumWebservice.userrss
   * 
   * @return the response is xml-data contains returned rss
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
   * Filters forum by name and returned result by provided size.
   * 
   * @param forumName the name to be filtered
   * @param maxSize limit of returned result.
   * @param sc security context to get request information
   * @param uriInfo The resquest information
   * 
   * @anchor ForumWebservice.filterForum
   * 
   * @return the response is json-data contain forum filtered by name.
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
        categoryFilters = forumService.filterForumByName(forumName, userName, maxSize_);
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
      }
    }
    MessageBean data = new MessageBean();
    data.setData(list);
    return data;
  }
  
  private String getUserId(SecurityContext sc, UriInfo uriInfo) {
    try {
      return sc.getUserPrincipal().getName();
    } catch (NullPointerException e) {
      return getViewerId(uriInfo);
    } catch (Exception e) {
      log.debug("Failed to get user id", e);
      return null;
    }
  }
  
  private String getViewerId(UriInfo uriInfo) {
    URI uri = uriInfo.getRequestUri();
    String requestString = uri.getQuery();
    if (requestString == null) return null;
    String[] queryParts = requestString.split("&");
    for (String queryPart : queryParts) {
      if (queryPart.startsWith("opensocial_viewer_id")) {
        return queryPart.substring(queryPart.indexOf("=") + 1, queryPart.length());
      }
    }
    return null;
  }
}
