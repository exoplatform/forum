/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.activity;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.forum.bbcode.api.BBCode;
import org.exoplatform.forum.bbcode.core.BBCodeRenderer;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.rendering.MarkupRenderingService;
import org.exoplatform.forum.rendering.core.SupportedSyntaxes;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 10, 2013  
 */
public class ForumActivityBuilder {

  public static final String FORUM_ACTIVITY_TYPE = "ks-forum:spaces";

  public static final String FORUM_ID_KEY      = "ForumId";

  public static final String CATE_ID_KEY       = "CateId";

  public static final String POST_TYPE         = "Post";

  public static final String POST_ID_KEY       = "PostId";

  public static final String POST_OWNER_KEY    = "PostOwner";

  public static final String POST_LINK_KEY     = "PostLink";

  public static final String POST_NAME_KEY     = "PostName";

  public static final String TOPIC_ID_KEY      = "TopicId";

  public static final String TOPIC_LINK_KEY    = "TopicLink";

  public static final String TOPIC_OWNER_KEY   = "TopicOwner";

  public static final String TOPIC_POST_COUNT_KEY    = "NumberOfReplies";
  
  public static final String TOPIC_VOTE_RATE_KEY    = "TopicVoteRate";

  private static final int NUMBER_CHARS    = 430;
  
  public static final String SPACE_GROUP_ID  = "SpaceGroupId";
  
  private ForumActivityBuilder() {
    
  }
  
  public static ExoSocialActivity createActivityComment(Post post, ForumActivityContext ctx) {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    return updateActivityComment(activity, post, ctx);
  }

  public static ExoSocialActivity updateActivityComment(ExoSocialActivity activity, Post post, ForumActivityContext ctx) {
    String message = post.getMessage();
    message = getMessageWithoutQuotedPost(post, post.getModifiedBy() != null);
    String title = processContent(message, 3);

    //activity.setUserId(post.getOwner());
    activity.setTitle(title);
    activity.setBody(CommonUtils.processBBCode(message));
    activity.isComment(true);
    activity.setType(FORUM_ACTIVITY_TYPE);

    //activity.setTitleId(title); => Resource Bundle Key

    //
    Map<String, String> templateParams = new HashMap<String, String>();

    templateParams.put(POST_ID_KEY, post.getId());
    templateParams.put(POST_LINK_KEY, post.getLink());
    templateParams.put(POST_NAME_KEY, post.getName());
    templateParams.put(POST_OWNER_KEY, post.getOwner());
    //
    templateParams.put(FORUM_ID_KEY, post.getForumId());
    templateParams.put(CATE_ID_KEY, post.getCategoryId());
    templateParams.put(TOPIC_ID_KEY, post.getTopicId());
    activity.setTemplateParams(templateParams);
    return activity;
  }

  private static String getMessageWithoutQuotedPost(Post post, boolean edit) {
    String parentCommentId = post.getQuotedPostId();
    String parentMessage;
    String fullName;
    try {
      if (edit) {
        ExoSocialActivity postCommentActivity = ForumActivityUtils.getCommentOfPost(post.getPath());
        if (postCommentActivity != null && postCommentActivity.getParentCommentId() != null) {
          ExoSocialActivity parentActivity = ForumActivityUtils.getActivityManager()
                                                               .getActivity(postCommentActivity.getParentCommentId());
          if (parentActivity != null) {
            parentMessage = parentActivity.getBody();
            Identity posterIdentity = ForumActivityUtils.getIdentityManager().getIdentity(parentActivity.getPosterId(), true);
            fullName = posterIdentity.getProfile().getFullName();
          } else {
            parentMessage = null;
            fullName = null;
          }
        } else {
          parentMessage = null;
          fullName = null;
        }
      } else if (parentCommentId != null) {
        Post parentPost = ForumActivityUtils.getForumService().getPost(post.getCategoryId(),
                                                                       post.getForumId(),
                                                                       post.getTopicId(),
                                                                       parentCommentId);
        fullName = ForumActivityUtils.getForumService().getScreenName(parentPost.getOwner());
        parentMessage = parentPost.getMessage();
      } else {
        parentMessage = null;
        fullName = null;
      }
    } catch (Exception e) {
      parentMessage = null;
      fullName = null;
    }

    String message = post.getMessage();
    if (parentMessage != null && fullName != null) {
      parentMessage = StringCommonUtils.decodeSpecialCharToHTMLnumber(parentMessage);
      parentMessage = CommonUtils.processBBCode(parentMessage);
      parentMessage = TransformHTML.cleanHtmlCode(StringEscapeUtils.unescapeHtml(TransformHTML.getPlainText(parentMessage)), null);

      message = TransformHTML.cleanHtmlCode(StringEscapeUtils.unescapeHtml(TransformHTML.getPlainText(post.getMessage())), null);
      message = message.replaceFirst(fullName + ":((\\r)?(\\n)?( )*)*" + parentMessage + "((\\r)?(\\n)?( )*)*", "").trim();
      message = message.replaceAll("((\\r|\\n)+( )*)+", "<br/>");
    }
    return message;
  }

  public static ExoSocialActivity createActivityComment(Topic topic, ForumActivityContext ctx) {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    String body = processContent(topic.getDescription(), 4);
    
    //activity.setUserId(topic.getOwner());
    String title = StringCommonUtils.decodeSpecialCharToHTMLnumber(topic.getTopicName());
    activity.setTitle(title);
    activity.setBody(body);
    activity.isComment(true);
    activity.setType(FORUM_ACTIVITY_TYPE);

    return activity;
  }
  
  public static String decodeHTMLInput(String message) {
    message = StringCommonUtils.decodeSpecialCharToHTMLnumber(message);
    String[] tab = TransformHTML.getPlainText(message).replaceAll("(?m)^\\s*$[\n\r]{1,}", "").split("\\r?\\n");
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<tab.length; i++) {
      sb.append(StringEscapeUtils.unescapeHtml(tab[i]));
      sb.append("<br/>");
    }
    return sb.toString();
  }
  
  /**
   * No more than nbOfLines lines
   * No more than 430 characters
   * If the content is larger than these limits, we add "..." at the end of the abstract.
   * @param content
   * @param nbOfLines
   * @return
   */
  public static String processContent(String content, int nbOfLines) {
    content = content.replaceAll("&nbsp;", "");
    String[] tab = TransformHTML.getPlainText(content).replaceAll("(?m)^\\s*$[\n\r]{1,}", "").split("\\r?\\n");
    //
    int numberOfLine = Math.min(nbOfLines, tab.length);
    StringBuilder sb = new StringBuilder();
    //
    for (int i=0; i<numberOfLine; i++) {
      sb.append(tab[i]);
      //
      if(i < numberOfLine - 1) {
        sb.append("BR_");
      }
    }
    String str = TransformHTML.cleanHtmlCode(sb.toString(), null).trim();

    str = trunc(str.replaceAll("BR_", "<br/>"), NUMBER_CHARS, tab.length > nbOfLines);
    str = CommonUtils.processBBCode(str);

    // The string could be truncated at middle of an BB Tag
    // So there may be an incomplete BB code at string after process, we need to clean them
    str = cleanupBBCode(str);
    return str;
  }
  
  /**
   * Truncates large Strings showing a portion of the String's head and tail
   * with the head cut out and replaced with '...'.
   * 
   * @param str
   *            the string to truncate
   * @param head
   *            the amount of the head to display
   * @return the head truncated string
   */
  public static final String trunc(String str, int head, boolean needTail) {
    StringBuffer buf = null;

    // Return as-is if String is smaller than or equal to the head plus the
    // tail plus the number of characters added to the trunc representation
    // plus the number of digits in the string length.
    buf = new StringBuffer();
    
    if (str.length() <= (head + 7 + str.length() / 10)) {
      buf.append(str);
      if (needTail) {
        buf.append("...");
      }
      
      return buf.toString();
    }

    //
    buf.append(str.substring(0, head)).append("...");
    return buf.toString();
  }

  private static String cleanupBBCode(String str) {
    MarkupRenderingService markupRenderingService = CommonUtils.getComponent(MarkupRenderingService.class);
    BBCodeRenderer r = (BBCodeRenderer)markupRenderingService.getRenderer(SupportedSyntaxes.bbcode.name());
    if (r != null) {
      String tag = null;
      for (BBCode bbcode : r.getBbcodes()) {
        tag = bbcode.getTagName();
        String pattern = "\\[" + tag + "[^\\]]*\\]";
        str = str.replaceAll(pattern, "");
      }
    }
    return str;
  }
  
  public static ExoSocialActivity createActivity(Topic topic, ForumActivityContext ctx) {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    String body = processContent(topic.getDescription(), 4);
    
    
    //processing in execute of task.
    //avoid get Identity here to write UT
    //activity.setUserId(topic.getOwner());
    activity.setTitle(StringCommonUtils.decodeSpecialCharToHTMLnumber(topic.getTopicName()));
    activity.setBody(body);
    activity.isComment(false);
    activity.isHidden(!topic.getIsApproved());
    activity.setType(FORUM_ACTIVITY_TYPE);
    
    //
    Map<String, String> templateParams = new HashMap<String, String>();
    
    templateParams.put(TOPIC_POST_COUNT_KEY, "" + topic.getPostCount());
    templateParams.put(TOPIC_VOTE_RATE_KEY, "" + topic.getVoteRating());
    templateParams.put(TOPIC_ID_KEY, topic.getId());
    templateParams.put(TOPIC_OWNER_KEY, topic.getOwner());
    //
    templateParams.put(TOPIC_LINK_KEY, CommonUtils.getURI(topic.getLink()));
    //
    templateParams.put(FORUM_ID_KEY, topic.getForumId());
    templateParams.put(CATE_ID_KEY, topic.getCategoryId());
    
    if (ForumActivityUtils.hasSpace(topic.getForumId())) {
      templateParams.put(SPACE_GROUP_ID, ForumActivityUtils.getSpaceGroupId(topic.getForumId()));
    }
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  public static ExoSocialActivity updateNumberOfReplies(Topic topic, ExoSocialActivity activity) {
    //
    Map<String, String> templateParams = activity.getTemplateParams();
    
    templateParams.put(TOPIC_POST_COUNT_KEY, "" + topic.getPostCount());
    return activity;
  }
  
  public static ExoSocialActivity updateNumberOfReplies(ExoSocialActivity activity, boolean isDelete) {
    //
    Map<String, String> templateParams = activity.getTemplateParams();
    int nbReplies = Integer.parseInt(templateParams.get(TOPIC_POST_COUNT_KEY));
    if (isDelete == true) {
      templateParams.put(TOPIC_POST_COUNT_KEY, "" + (nbReplies - 1));
    } else {
      templateParams.put(TOPIC_POST_COUNT_KEY, "" + (nbReplies + 1));
    }
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  public static ExoSocialActivity updateVoteRate(Topic topic, ExoSocialActivity activity) {
    //
    Map<String, String> templateParams = activity.getTemplateParams();
    templateParams.put(TOPIC_VOTE_RATE_KEY, "" + topic.getVoteRating());
    return activity;
  }
  
}
