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
package org.exoplatform.forum.service.ws.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Tag;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.ws.AbstractResourceTest;
import org.exoplatform.forum.service.ws.BanIP;
import org.exoplatform.forum.service.ws.BeanToJsons;
import org.exoplatform.forum.service.ws.ForumWebservice;
import org.exoplatform.forum.service.ws.MessageBean;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;

public class ForumWebserviceTestCase extends AbstractResourceTest {
  ForumWebservice     forumWebservice;

  static final String baseURI = "/ks/forum";
  static final String fullURI = "http://localhost:8080".concat(baseURI);
  

  public void setUp() throws Exception {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
    super.setUp();
    forumWebservice = (ForumWebservice) getService(ForumWebservice.class);
    registry(forumWebservice);
    initDefaultData();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public ContainerResponse performTestCase(String path) throws Exception {
    MultivaluedMap<String, String> h = new MultivaluedMapImpl();
    ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
    ContainerResponse response = service("GET", fullURI.concat(path), "", h, null, writer);
    return response;
  }

  public void testGetMessage() throws Exception {
    loginUser(USER_ROOT);
    for (int i = 0; i < 10; i++) {
      Post post = createdPost();
      post.setName("post " + i);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    String eventURI = "/getmessage/5";
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    
    // assert data
    MessageBean bean = (MessageBean)response.getEntity();
    assertEquals(bean.getData().size(), 5);

  }
  
  public void testGetPulicMessage() throws Exception {
    loginUser(USER_DEMO);
    for (int i = 0; i < 10; i++) {
      Post post = createdPost();
      post.setOwner(USER_DEMO);
      post.setName("post of demo " + i);
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
    }
    String eventURI = "/getpublicmessage/5";
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    
    // assert data
    MessageBean bean = (MessageBean)response.getEntity();
    assertEquals(bean.getData().size(), 5);
  }
  
  /**
   * @throws Exception
   */
  public void testFilterIps() throws Exception {
    forumService_.addBanIP("192.168.1.10");
    forumService_.addBanIP("192.168.1.11");
    
    //Test with all
    String eventURI = "/filter/all";
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    
    BeanToJsons<BanIP> bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(2,bean.getJsonList().size());
    assertEquals("192.168.1.11",bean.getJsonList().get(1).getIp());
    
    //Test with an ip exact
    eventURI="/filter/192.168.1.11";
    response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(1,bean.getJsonList().size());
    assertEquals("192.168.1.11",bean.getJsonList().get(0).getIp());
    
    //Test with an ip that don't exist in the banip's list
    eventURI="/filter/192.168.1.12";
    response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(0,bean.getJsonList().size());
  }
  
  /**
   * @throws Exception
   */
  public void testFilterIpBanForum() throws Exception {
    forumService_.addBanIPForum("192.168.0.1", categoryId+"/"+forumId);
    forumService_.addBanIPForum("192.168.0.2", categoryId+"/"+forumId);
    forumService_.addBanIPForum("192.168.0.3", categoryId+"/"+forumId);
    
    //Test with an ip that don't exist in the banip's list
    String eventURI = "/filterIpBanforum/"+categoryId+"."+forumId+"/192.168.0.10";
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    BeanToJsons<BanIP> bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(0,bean.getJsonList().size());
    
    //Test with an ip exact
    eventURI="/filterIpBanforum/"+categoryId+"."+forumId+"/192.168.0.1";
    response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(1,bean.getJsonList().size());
    assertEquals("192.168.0.1",bean.getJsonList().get(0).getIp());
    
    //Test with all
    eventURI="/filterIpBanforum/"+categoryId+"."+forumId+"/all";
    response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    bean = (BeanToJsons<BanIP>) response.getEntity();
    assertEquals(3,bean.getJsonList().size());
    assertEquals("192.168.0.3",bean.getJsonList().get(2).getIp());
    
  }
  
  public void testFilterTagNameForum() throws Exception {
    List<Tag> tags = new ArrayList<Tag>(10);
    for (int i = 0; i < 10; i++) {
      Tag tag = new Tag();
      tag.setName("newfoo" + i);
      tags.add(tag);
    }
    Topic A = forumService_.getTopic(categoryId, forumId, topicId, USER_ROOT);
    forumService_.addTag(tags, USER_ROOT, A.getPath());
    tags = forumService_.getAllTags();
    // when user click on add Tag, list all tags 
    String userAndTopicId = USER_DEMO + "," + topicId;
    String eventURI = "/filterTagNameForum/"+userAndTopicId+"/onclickForm";
    
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    BeanToJsons<BanIP> results =  (BeanToJsons<BanIP>) response.getEntity();
    assertNotNull(results);
    
    // When other user filter tags on topic A
    userAndTopicId = USER_ROOT + "," + (new Topic()).getId();
    eventURI = "/filterTagNameForum/"+userAndTopicId+"/foo";
    
    response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    results =  (BeanToJsons<BanIP>) response.getEntity();
    assertNotNull(results);
  }

  public void testViewrss() throws Exception {
    String eventURI = "/rss/" + topicId;
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    InputStream ip = (InputStream) response.getEntity();
    assertNotNull(ip);
  }
  
  public void testCheckPublicRss() throws Exception {
    forumService_.addWatch(-1, forumId, null, USER_JOHN);
    // save watch rss by john and test
    String eventURI = "/rss/user/john";
    ContainerResponse response = performTestCase(eventURI);
    assertNotNull(response);
    assertEquals(response.getStatus(), 200);
    InputStream ip = (InputStream) response.getEntity();
    assertNotNull(ip);
  }
}
