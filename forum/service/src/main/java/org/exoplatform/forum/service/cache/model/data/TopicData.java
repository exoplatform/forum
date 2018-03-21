package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.Topic;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class TopicData implements CachedData<Topic> {

  public final static TopicData NULL = new TopicData(new Topic());

  private final String id;
  private final String owner;
  private final String path;
  private final Date createdDate;
  private final String modifiedBy;
  private final Date modifiedDate;
  private final String editReason;
  private final String lastPostBy;
  private final Date lastPostDate;
  private final String name;
  private final String description;
  private final long postCount;
  private final long viewCount;
  private final String icon;
  private final String link;
  private final String remoteAddr;
  private final String topicType;
  private final long numberAttachments;
  private final boolean isModeratePost;
  private final String isNotifyWhenAddPost;
  private final boolean isClosed;
  private final boolean isLock;
  private final boolean isApproved;
  private final boolean isSticky;
  private final boolean isPoll;
  private final boolean isWaiting;
  private final boolean isActive;
  private final boolean isActiveByForum;
  private final String[] canView;
  private final String[] canPost;
  private final String[] userVoteRating;
  private final String[] tagId;
  private final String[] emailNotification;
  private final Double voteRating;
  private final ForumAttachment[] attachments;

  public TopicData(Topic topic) {
    this.id = topic.getId();
    this.owner = topic.getOwner();
    this.path = topic.getPath();
    this.createdDate = topic.getCreatedDate();
    this.modifiedBy = topic.getModifiedBy();
    this.modifiedDate = topic.getModifiedDate();
    this.editReason = topic.getEditReason();
    this.lastPostBy = topic.getLastPostBy();
    this.lastPostDate = topic.getLastPostDate();
    this.name = topic.getTopicName();
    this.description = topic.getDescription();
    this.postCount = topic.getPostCount();
    this.viewCount = topic.getViewCount();
    this.icon = topic.getIcon();
    this.link = topic.getLink();
    this.remoteAddr = topic.getRemoteAddr();
    this.topicType = topic.getTopicType();
    this.numberAttachments = topic.getNumberAttachment();
    this.isModeratePost = topic.getIsModeratePost();
    this.isNotifyWhenAddPost = topic.getIsNotifyWhenAddPost();
    this.isClosed = topic.getIsClosed();
    this.isLock = topic.getIsLock();
    this.isApproved = topic.getIsApproved();
    this.isSticky = topic.getIsSticky();
    this.isPoll = topic.getIsPoll();
    this.isWaiting = topic.getIsWaiting();
    this.isActive = topic.getIsActive();
    this.isActiveByForum = topic.getIsActiveByForum();
    this.canView = topic.getCanView();
    this.canPost = topic.getCanPost();
    this.userVoteRating = topic.getUserVoteRating();
    this.tagId = topic.getTagId();
    this.emailNotification = topic.getEmailNotification();
    this.voteRating = topic.getVoteRating();
    if (topic.getAttachments() != null) {
      this.attachments = topic.getAttachments().toArray(new ForumAttachment[]{});
    } else {
      this.attachments = null;
    }
  }

  public Topic build() {

    if (this == NULL) {
      return null;
    }

    Topic topic = new Topic();
    topic.setId(this.id);
    topic.setOwner(this.owner);
    topic.setPath(this.path);
    topic.setCreatedDate(this.createdDate);
    topic.setModifiedBy(this.modifiedBy);
    topic.setModifiedDate(this.modifiedDate);
    topic.setEditReason(this.editReason);
    topic.setLastPostBy(this.lastPostBy);
    topic.setLastPostDate(this.lastPostDate);
    topic.setTopicName(this.name);
    topic.setDescription(this.description);
    topic.setPostCount(this.postCount);
    topic.setViewCount(this.viewCount);
    topic.setIcon(this.icon);
    topic.setLink(this.link);
    topic.setRemoteAddr(this.remoteAddr);
    topic.setTopicType(this.topicType);
    topic.setNumberAttachment(this.numberAttachments);
    topic.setIsModeratePost(this.isModeratePost);
    topic.setIsNotifyWhenAddPost(this.isNotifyWhenAddPost);
    topic.setIsClosed(this.isClosed);
    topic.setIsLock(this.isLock);
    topic.setIsApproved(this.isApproved);
    topic.setIsSticky(this.isSticky);
    topic.setIsPoll(this.isPoll);
    topic.setIsWaiting(this.isWaiting);
    topic.setIsActive(this.isActive);
    topic.setIsActiveByForum(this.isActiveByForum);
    topic.setCanView(this.canView);
    topic.setCanPost(this.canPost);
    topic.setUserVoteRating(this.userVoteRating);
    topic.setTagId(this.tagId);
    topic.setEmailNotification(this.emailNotification);
    topic.setVoteRating(this.voteRating);
    if (this.attachments != null) {
      topic.setAttachments(Arrays.asList(this.attachments));
    }
    return topic;

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicData topicData = (TopicData) o;
    return postCount == topicData.postCount &&
            viewCount == topicData.viewCount &&
            numberAttachments == topicData.numberAttachments &&
            isModeratePost == topicData.isModeratePost &&
            isClosed == topicData.isClosed &&
            isLock == topicData.isLock &&
            isApproved == topicData.isApproved &&
            isSticky == topicData.isSticky &&
            isPoll == topicData.isPoll &&
            isWaiting == topicData.isWaiting &&
            isActive == topicData.isActive &&
            isActiveByForum == topicData.isActiveByForum &&
            Objects.equals(id, topicData.id) &&
            Objects.equals(owner, topicData.owner) &&
            Objects.equals(path, topicData.path) &&
            Objects.equals(createdDate, topicData.createdDate) &&
            Objects.equals(modifiedBy, topicData.modifiedBy) &&
            Objects.equals(modifiedDate, topicData.modifiedDate) &&
            Objects.equals(editReason, topicData.editReason) &&
            Objects.equals(lastPostBy, topicData.lastPostBy) &&
            Objects.equals(lastPostDate, topicData.lastPostDate) &&
            Objects.equals(name, topicData.name) &&
            Objects.equals(description, topicData.description) &&
            Objects.equals(icon, topicData.icon) &&
            Objects.equals(link, topicData.link) &&
            Objects.equals(remoteAddr, topicData.remoteAddr) &&
            Objects.equals(topicType, topicData.topicType) &&
            Objects.equals(isNotifyWhenAddPost, topicData.isNotifyWhenAddPost) &&
            Arrays.equals(canView, topicData.canView) &&
            Arrays.equals(canPost, topicData.canPost) &&
            Arrays.equals(userVoteRating, topicData.userVoteRating) &&
            Arrays.equals(tagId, topicData.tagId) &&
            Arrays.equals(emailNotification, topicData.emailNotification) &&
            Objects.equals(voteRating, topicData.voteRating) &&
            Arrays.equals(attachments, topicData.attachments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, owner, path, createdDate, modifiedBy, modifiedDate, editReason, lastPostBy,
            lastPostDate, name, description, postCount, viewCount, icon, link, remoteAddr, topicType,
            numberAttachments, isModeratePost, isNotifyWhenAddPost, isClosed, isLock, isApproved, isSticky, isPoll,
            isWaiting, isActive, isActiveByForum, canView, canPost, userVoteRating, tagId, emailNotification, voteRating, attachments);
  }
}
