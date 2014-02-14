/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.forum.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.forum.service.impl.model.PostListAccess;

public class PostTestCase extends BaseForumServiceTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }
  
  @Override
  public void tearDown() throws Exception {
    //
    super.tearDown();
  }
  public void testPost() throws Exception {
    // set Data
    initDefaultData();

    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 25; ++i) {
      Post post = createdPost();
      posts.add(post);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    // getPost
    assertNotNull(forumService_.getPost(categoryId, forumId, topicId, posts.get(0).getId()));
    assertEquals(25, forumService_.getTopic(categoryId, forumId, topicId, "").getPostCount());

    // get ListPost
    JCRPageList pagePosts = forumService_.getPosts(categoryId, forumId, topicId, "", "", "", "root");
    assertEquals(posts.size() + 1, pagePosts.getAvailable());// size = 26 (first post and new postList)
    List page1 = pagePosts.getPage(1);
    assertEquals(page1.size(), 10);
    List page3 = pagePosts.getPage(3);
    assertEquals(page3.size(), 6);
    // getPost by Ip
    JCRPageList pageIpPosts = forumService_.getListPostsByIP("192.168.1.11", null);
    assertEquals(pageIpPosts.getAvailable(), 25);// size = 25 (not content first post)
    // update Post First
    Post newPost = (Post) pagePosts.getPage(1).get(1);
    newPost.setMessage("New message");
    forumService_.savePost(categoryId, forumId, topicId, newPost, false, new MessageBuilder());
    assertEquals("New message", forumService_.getPost(categoryId, forumId, topicId, newPost.getId()).getMessage());

    // test movePost
    Topic topicnew = createdTopic("root");
    forumService_.saveTopic(categoryId, forumId, topicnew, true, false, new MessageBuilder());
    topicnew = forumService_.getTopic(categoryId, forumId, topicnew.getId(), "root");

    forumService_.movePost(new String[] { newPost.getPath() }, topicnew.getPath(), false, "test mail content", "");
    assertEquals(1, forumService_.getTopic(categoryId, forumId, topicnew.getId(), "").getPostCount());
    assertNotNull(forumService_.getPost(categoryId, forumId, topicnew.getId(), newPost.getId()));

    // test remove Post return post
    assertNotNull(forumService_.removePost(categoryId, forumId, topicnew.getId(), newPost.getId()));
    assertNull(forumService_.getPost(categoryId, forumId, topicnew.getId(), newPost.getId()));
    assertEquals(24, forumService_.getTopic(categoryId, forumId, topicId, "").getPostCount());
    assertEquals(0, forumService_.getTopic(categoryId, forumId, topicnew.getId(), "").getPostCount());

    // getViewPost
  }
  
  public void testPostListAccess() throws Exception {
    // set Data
    initDefaultData();
    
    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 25; ++i) {
      Post post = createdPost();
      posts.add(post);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    // getPost
    assertNotNull(forumService_.getPost(categoryId, forumId, topicId, posts.get(0).getId()));

    // get ListPost
    PostListAccess listAccess = (PostListAccess) forumService_.getPosts(new PostFilter(categoryId, forumId, topicId, "", "", "", "root"));
    listAccess.initialize(10, 1);
    assertEquals(listAccess.getSize(), posts.size() + 1);// size = 26 (first post and new postList)
    
    //Page 1
    List<Post> got = Arrays.asList(listAccess.load(1));
    assertEquals(got.size(), 10);
    
    //Page 2
    got = Arrays.asList(listAccess.load(2));
    assertEquals(got.size(), 10);
    
    //Page 3
    got = Arrays.asList(listAccess.load(3));
    assertEquals(got.size(), 6);
  }
  
  public void testGetPostByUser() throws Exception {
    // set Data
    initDefaultData();
    
    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 25; ++i) {
      Post post = createdPost();
      posts.add(post);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    // getPost
    assertNotNull(forumService_.getPost(categoryId, forumId, topicId, posts.get(0).getId()));

    JCRPageList pageList = forumService_.getPagePostByUser("root", "root", true, "exo:createdDate");
    pageList.setPageSize(10);
    List<Post> got = pageList.getPage(0);
    assertEquals(10, got.size());
    //
    got = pageList.getPage(1);
    assertEquals(10, got.size());
  }
  
  public void testGetPostByUserAbnormal() throws Exception {
   
    // get ListPost
    JCRPageList pageList;
    try {
      pageList = forumService_.getPagePostByUser(null, "root", true, "exo:createdDate");
      fail("userName");
    } catch (Exception e) {
    }
    
    try {
      pageList = forumService_.getPagePostByUser("root", null, true, "exo:createdDate");
      fail("userLogin");
    } catch (Exception e) {
    }
    
    try {
      pageList = forumService_.getPagePostByUser(null, null, true, "exo:createdDate");
      fail();
    } catch (Exception e) {
    }
    
   
  }
  
  public void testGetPostForSplitTopic() throws Exception {
    initDefaultData();
    
    List<Post> posts = new ArrayList<Post>();
    for (int i = 0; i < 5; ++i) {
      Post post = createdPost();
      posts.add(post);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    String topicPath = categoryId + "/" + forumId + "/" + topicId;
    JCRPageList pageList = forumService_.getPostForSplitTopic(topicPath);
    assertEquals(5, pageList.getAvailable());
  }
  
  public void testCensoredPost() throws Exception {
    initDefaultData();
    Topic topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    
    //number of posts is 0
    assertEquals(0, topic.getPostCount());
    
    //add new post
    Post post = createdPost();
    forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    assertEquals(1, topic.getPostCount());
    
    //add new pending post
    post = createdPost();
    post.setIsWaiting(true);
    forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    assertEquals(1, topic.getPostCount());
    
    //approve the pending post
    post.setIsWaiting(false);
    List<Post> posts = new ArrayList<Post>();
    posts.add(post);
    forumService_.modifyPost(posts, 5);
    topic = forumService_.getTopic(categoryId, forumId, topicId, "");
    assertEquals(2, topic.getPostCount());
  }
  
}
