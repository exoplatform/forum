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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.exoplatform.forum.base.BaseForumServiceTestCase;

public class SearchTestCase extends BaseForumServiceTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public Date getDate(int y, int m, int d) throws ParseException{
    Calendar cal = new GregorianCalendar();
    cal.set(y,m,d);
    return cal.getTime();
  }
  
  public Calendar getCalendar(Date date){
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    return c;
  }

  public void initData() throws Exception{
    
    //Root create a category
    Category cat = new Category();
    cat.setOwner("root");
    cat.setCategoryName("social");
    cat.setDescription("about your team");
    forumService_.saveCategory(cat, true);
    //Root create a forum
    Forum forum = new Forum();
    forum.setOwner("root");
    forum.setForumName("general question");
    forum.setDescription("all yours questions here");
    String[] mods = {"john"};
    forum.setModerators(mods);
    forumService_.saveForum(cat.getId(), forum, true);
    //Root create a topic
    Topic topic = new Topic();
    topic.setOwner("root");
    topic.setTopicName("where do you go?");
    topic.setDescription("the next week");
    forumService_.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());
    //Root post a message
    Post post = new Post();
    post.setOwner("root");
    post.setName("new post");
    post.setMessage("go to the hell");
    forumService_.savePost(cat.getId(), forum.getId(), topic.getId(), post, true, new MessageBuilder());
    
    //John post a message
    Post postJohn = new Post();
    postJohn.setOwner("john");
    postJohn.setName("second post");
    postJohn.setMessage("go to the home of mary");
    forumService_.savePost(cat.getId(), forum.getId(), topic.getId(), postJohn, true, new MessageBuilder());
    
    //Mary create new topic
    Topic topicMary = new Topic();
    topicMary.setOwner("mary");
    topicMary.setTopicName("what do you do?");
    topicMary.setDescription("kill you");
    forumService_.saveTopic(cat.getId(), forum.getId(), topicMary, true, false, new MessageBuilder());
 
    //set topic closed
    topicMary.setIsClosed(true);
    topicMary.setViewCount(50);
    forumService_.saveTopic(cat.getId(), forum.getId(), topicMary, false, false, new MessageBuilder());
    
    //root create new forum and lock it
    Forum forumNew = new Forum();
    forumNew.setOwner("root");
    forumNew.setForumName("new forum");
    forumNew.setDescription("new description");
    forumNew.setIsLock(true);
    forumService_.saveForum(cat.getId(), forumNew, true);
  }

  public void testQuickSearch() throws Exception {
    
    //Init data
    initData();
    
    String pathQuery = "";  //We will search in categoryHome
    String type = "true,all"; //isAdmin=true
    
    //Search all (category,forum,topic,post) concerns the value of textQuery : owner, message, description, name ...
    String textQuery = "john"; 
    
    List<ForumSearchResult> forumSearchs = 
        forumService_.getQuickSearch(textQuery, type, pathQuery, "root", null, null, null); 
    //here we found 3 elements : the post of john, the topic contains his post and the forum moderated by john
    assertEquals(forumSearchs.size(),3);
    
    textQuery = "root"; 
    forumSearchs = forumService_.getQuickSearch(textQuery, type, pathQuery, "root", null, null, null); 
    //here we found 5 elements
    assertEquals(forumSearchs.size(),5);
    
    textQuery = "mary"; 
    forumSearchs = forumService_.getQuickSearch(textQuery, type, pathQuery, "root", null, null, null); 
    //here we found 2 elements : mary's topic and the post of john that contains "mary"
    assertEquals(forumSearchs.size(),2);
  }
  
public void testAdvancedSearch() throws Exception {
    
    //Init data
    initData();
    
    List<String> users = new ArrayList<String>(); 
    users.add("root");
    users.add("john");
    users.add("mary");
    users.add("demo");
    
    ForumEventQuery eventQuery = new ForumEventQuery(); 
    eventQuery.setListOfUser(users); 
    eventQuery.setUserPermission(0); 
    eventQuery.setType(Utils.CATEGORY) ; //search only category
    eventQuery.setKeyValue("root") ; //keyword for search
    eventQuery.setValueIn("entire") ; //search all : owner, title, content, message ... (not only title)
    eventQuery.setPath("") ; //search in categoryHome
    eventQuery.setByUser(""); //filter by owner (all in this case)
    eventQuery.setIsLock("") ; //search a locked topic(forum) or not
    eventQuery.setIsClose("") ; //search a closed topic(forum) or not
    eventQuery.setTopicCountMin("0") ; //filter by number minimum of topics
    eventQuery.setPostCountMin("0") ; //filter by number minimum of posts
    eventQuery.setViewCountMin("0") ; //filter by number minimum of views
    eventQuery.setModerator("") ; //filter by moderator
    
    //Test search "root" in all category
    List<ForumSearchResult> forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null); 
    assertNotNull(forumSearchs);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"social");
    
    //Test search "root" in all forum
    eventQuery.setType(Utils.FORUM) ;
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),2);
    assertEquals(forumSearchs.get(0).getName(),"general question");
    assertEquals(forumSearchs.get(1).getName(),"new forum");
    
    //Test search "mary" in all topic
    eventQuery.setKeyValue("mary");
    eventQuery.setType(Utils.TOPIC) ;
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"what do you do?");
    
    //Test search "mary" in all post
    eventQuery.setType(Utils.POST) ;
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"second post");
    
    //Test search "mary" in all post's title and not the post'content
    eventQuery.setValueIn("title");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),0);
    
    //Test search all post of mary
    eventQuery.setKeyValue("");
    eventQuery.setByUser("mary");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),0);
    
    //Test search all post of john
    eventQuery.setByUser("john");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"second post");
    
    //Test search all forum with john as moderator
    eventQuery.setType(Utils.FORUM) ;
    eventQuery.setByUser("");
    eventQuery.setModerator("john");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"general question");
    
    //Test search all closed topic
    eventQuery.setType(Utils.TOPIC) ;
    eventQuery.setModerator("");
    eventQuery.setIsClose("true");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"what do you do?");
    
    //Test search all locked forum
    eventQuery.setType(Utils.FORUM) ;
    eventQuery.setIsClose("");  
    eventQuery.setIsLock("true");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"new forum");
    
    //Test search all forum that have number of topic min = 1
    eventQuery.setType(Utils.FORUM) ;
    eventQuery.setIsLock("");
    eventQuery.setTopicCountMin("1");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"general question");
    
    //Test search all forum that have number of topic min = 3
    eventQuery.setTopicCountMin("3");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),0);
    
    //Test search all topic that have number of post min = 2
    eventQuery.setType(Utils.TOPIC) ;
    eventQuery.setTopicCountMin("0");
    eventQuery.setPostCountMin("2");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),1);
    assertEquals(forumSearchs.get(0).getName(),"where do you go?");
    
    //Test search all topic that have number of post min = 2 and number of view min = 20
    eventQuery.setViewCountMin("20");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),0);
    
    //Test search all post before the current date
    eventQuery.setToDateCreated(GregorianCalendar.getInstance());
    eventQuery.setType(Utils.POST) ;
    eventQuery.setIsLock("");
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),2);
    
    //Test search all forum after the current date
    eventQuery.setFromDateCreated(GregorianCalendar.getInstance());
    eventQuery.setType(Utils.FORUM) ;
    forumSearchs = forumService_.getAdvancedSearch(eventQuery, null, null);
    assertEquals(forumSearchs.size(),0);
    
  }

}
