/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.forum.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.observation.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import org.exoplatform.commons.testing.AssertUtils;
import org.exoplatform.commons.testing.KernelUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.conf.RoleRulesPlugin;
import org.exoplatform.forum.common.jcr.*;
import org.exoplatform.forum.common.jcr.KSDataLocation.Locations;
import org.exoplatform.forum.membership.AbstractJCRTestCase;
import org.exoplatform.forum.service.*;
import org.exoplatform.services.jcr.util.JCRMockUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
// TODO :
// * Fix tests to not have to specify the order of execution like this
// * The order of tests execution changed in Junit 4.11 (https://github.com/KentBeck/junit/blob/master/doc/ReleaseNotes4.11.md)
@FixMethodOrder(MethodSorters.JVM)
public class JCRDataStorageTestCase extends AbstractJCRTestCase {
  
  protected Log log = ExoLogger.getLogger(JCRDataStorageTestCase.class);

  private JCRDataStorage storage;
  @Override
  public void beforeRunBare() {
    try {
      super.beforeRunBare();
    } catch (Exception e) {
      log.error(e);
    }
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    storage = new JCRDataStorage();
    storage.setDataLocator(dataLocation);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConstructor() {
    KSDataLocation location = new KSDataLocation("bar");
    JCRDataStorage storage = new JCRDataStorage(location);
    // assertEquals(storage.getRepository(), "foo");
    assertEquals(storage.getWorkspace(), "bar");
    assertEquals(storage.getPath(), location.getForumHomeLocation());
  }

  public void testPlugins() {
    KSDataLocation location = new KSDataLocation("bar");
    JCRDataStorage storage = new JCRDataStorage(location);
    storage.getDefaultPlugins();
  }

  public void testUpdateModeratorInForum() throws Exception {
    String moderatorsPropName = "exo:moderators";
    String[] moderators = new String[] { "foo", "zed" };
    JCRDataStorage storage = new JCRDataStorage();

    Node node = JCRMockUtils.mockNode();
    JCRMockUtils.stubProperty(node, moderatorsPropName, "foo", "bar");
    String[] actual = storage.updateModeratorInForum(node, moderators);
    AssertUtils.assertContains(actual, "foo", "bar", "zed");

    Node node2 = JCRMockUtils.mockNode();
    JCRMockUtils.stubNullProperty(node2, moderatorsPropName);
    String[] actual2 = storage.updateModeratorInForum(node2, moderators);
    AssertUtils.assertContains(actual2, "foo", "zed");

    Node node3 = JCRMockUtils.mockNode();
    JCRMockUtils.stubProperty(node3, moderatorsPropName, " ", "bar");
    String[] actual3 = storage.updateModeratorInForum(node3, moderators);
    AssertUtils.assertContains(actual3, "foo", "zed");
  }

  public void testSetDefaultAvatar() throws Exception {

    addNode(storage.getDataLocation().getAvatarsLocation());

    String avatarLocation = storage.getDataLocation().getAvatarsLocation() + "/username";
    assertNodeNotExists(avatarLocation);
    storage.setDefaultAvatar("username");
    assertNodeNotExists(avatarLocation);

    addFile(avatarLocation);
    assertNodeExists(avatarLocation);
    storage.setDefaultAvatar("username");
    assertNodeNotExists(avatarLocation);
  }

  public void testGetAvatar() throws Exception {
    String avatarLocation = storage.getDataLocation().getAvatarsLocation() + "/username2";
    // assertNull(storage.getUserAvatar("username2"));
    addFile(avatarLocation);

    ForumAttachment attachment = storage.getUserAvatar("username2");
    assertNotNull(attachment);
    assertEquals("avatar.plain", attachment.getName());
    assertEquals("text/plain", attachment.getMimeType());
    assertEquals("/" + getWorkspace() + "/" + avatarLocation, attachment.getPath()); // /portal-test/ksUserAvatar/username
    assertEquals("stuff", stringOf(attachment.getInputStream()));
  }

  public void testSaveAvatar() throws Exception {
    String avatarLocation = storage.getDataLocation().getAvatarsLocation() + "/username3";
    assertNull(storage.getUserAvatar("username3"));
    addFile(avatarLocation);

    storage.saveUserAvatar("username3", new TextForumAttachment("updated content"));

    Node node = getNode(avatarLocation);
    assertEquals("updated content", stringOf(node.getNode("jcr:content").getProperty("jcr:data").getStream()));

  }

  public void testAddPlugin() throws Exception {
    // fixture
    addNode(storage.getDataLocation().getForumCategoriesLocation(), "exo:categoryHome");

    // null plugin
    storage.addPlugin(null);
    assertNull(storage.getServerConfig().get("foo"));

    // not EmailNotifyPlugin
    ComponentPlugin plugin = mock(ComponentPlugin.class);
    storage.addPlugin(plugin);
    assertNull(storage.getServerConfig().get("foo"));

    // with EmailNotifyPlugin
    InitParams params = new InitParams();
    Map<String, String> map = new HashMap<String, String>();
    map.put("foo", "bar");
    KernelUtils.addPropertiesParam(params, "email.configuration.info", map);
    EmailNotifyPlugin notifPlugin = new EmailNotifyPlugin(params);
    storage.addPlugin(notifPlugin);

    assertEquals("bar", storage.getServerConfig().get("foo"));
  }

  public void testAddRolePlugin() throws Exception {
    storage.addRolePlugin(null);
    AssertUtils.assertEmpty(storage.getRulesPlugins());

    // not RoleRulesPlugin
    ComponentPlugin plugin = mock(ComponentPlugin.class);
    storage.addRolePlugin(plugin);
    AssertUtils.assertEmpty(storage.getRulesPlugins());

    InitParams params = new InitParams();
    KernelUtils.addValueParam(params, "role", "ADMIN");
    KernelUtils.addValuesParam(params, "rules", "rule1", "rule2");
    storage.addRolePlugin(new RoleRulesPlugin(params));
    AssertUtils.assertNotEmpty(storage.getRulesPlugins());
  }

  private <T extends EventListener> boolean hasListenerOfType(ObservationManager manager, Class<T> clazz) throws RepositoryException {
    EventListenerIterator it = manager.getRegisteredEventListeners();
    boolean found = false;
    while (it.hasNext()) {
      EventListener listener = it.nextEventListener();
      if (listener.getClass() == clazz) {
        found = true;
        break;
      }
    }
    return found;
  }

  public void testSaveForumAdministration() throws Exception {
    adminNodeFixture();
    // test create
    ForumAdministration admin = new ForumAdministration();
    admin.setCensoredKeyword("4letterword");
    admin.setEnableHeaderSubject(true);
    admin.setForumSortBy("name");
    admin.setForumSortByType("descending");
    admin.setHeaderSubject("header");
    admin.setNotifyEmailContent("content");
    admin.setNotifyEmailMoved("moved");
    admin.setTopicSortBy("postCount");
    admin.setTopicSortByType("ascending");
    storage.saveForumAdministration(admin);
    assertAdminSaved(admin);

    // test update
    admin.setCensoredKeyword("censored");
    admin.setEnableHeaderSubject(false);
    admin.setForumSortBy("forum");
    admin.setForumSortByType("ascending");
    admin.setHeaderSubject("subject");
    admin.setNotifyEmailContent("c");
    admin.setNotifyEmailMoved("m");
    admin.setTopicSortBy("topic");
    admin.setTopicSortByType("descending");
    storage.saveForumAdministration(admin);

    assertAdminSaved(admin);
  }

  public void testGetForumAdministration() throws Exception {

    // fixture
    String adminPath = adminNodeFixture();

    Session session = getSession();
    Node adminNode = session.getRootNode().getNode(adminPath);
    adminNode.setProperty("exo:forumSortBy", "a");
    adminNode.setProperty("exo:forumSortByType", "b");
    adminNode.setProperty("exo:topicSortBy", "c");
    adminNode.setProperty("exo:topicSortByType", "d");
    adminNode.setProperty("exo:censoredKeyword", "e");
    adminNode.setProperty("exo:enableHeaderSubject", true);
    adminNode.setProperty("exo:headerSubject", "f");
    adminNode.setProperty("exo:notifyEmailContent", "g");
    adminNode.setProperty("exo:notifyEmailMoved", "h");
    session.save();

    // call method and check values
    ForumAdministration admin = storage.getForumAdministration();
    assertEquals("a", admin.getForumSortBy());
    assertEquals("b", admin.getForumSortByType());
    assertEquals("c", admin.getTopicSortBy());
    assertEquals("d", admin.getTopicSortByType());
    assertEquals("e", admin.getCensoredKeyword());
    assertEquals(true, admin.getEnableHeaderSubject());
    assertEquals("f", admin.getHeaderSubject());
    assertEquals("g", admin.getNotifyEmailContent());
    assertEquals("h", admin.getNotifyEmailMoved());

  }

  public void testUpdatePostCount() throws Exception {
    //
    initDefaultData();
    // default data
    Forum forum = storage.getForum(categoryId, forumId);
    assertNotNull(forum);
    assertEquals(1, forum.getPostCount());
    // Test auto update by JCR listener
    Topic topic = createdTopic(USER_DEMO);
    storage.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    assertEquals(2, storage.getForum(categoryId, forumId).getPostCount());
    assertEquals(1, storage.getUserInfo(USER_ROOT).getTotalPost());
    
    // add more 3 posts
    storage.savePost(categoryId, forumId, topic.getId(), createdPost(), true, new MessageBuilder());
    storage.savePost(categoryId, forumId, topic.getId(), createdPost(), true, new MessageBuilder());
    storage.savePost(categoryId, forumId, topic.getId(), createdPost(), true, new MessageBuilder());
    //
    assertEquals(5, storage.getForum(categoryId, forumId).getPostCount());
    assertEquals(3, storage.getTopic(categoryId, forumId, topic.getId(), "").getPostCount());
    assertEquals(4, storage.getUserInfo(USER_ROOT).getTotalPost());
    
    // manual call JCRDataStorage#updatePostCount
    // save new post on default topic
    Post p = createdPost();
    storage.savePost(categoryId, forumId, topicId, p, true, new MessageBuilder());
    assertEquals(6, storage.getForum(categoryId, forumId).getPostCount());
    assertEquals(1, storage.getTopic(categoryId, forumId, topicId, "").getPostCount());
    assertEquals(5, storage.getUserInfo(USER_ROOT).getTotalPost());
    //
    for (int i = 0; i < 3; i++) {
      storage.updatePostCount(p.getPath(), USER_ROOT);
    }
    assertEquals(9, storage.getForum(categoryId, forumId).getPostCount());
    assertEquals(4, storage.getTopic(categoryId, forumId, topicId, "").getPostCount());
    // method JCRDataStorage#updatePostCount() can not update user profile
    assertEquals(5, storage.getUserInfo(USER_ROOT).getTotalPost());
  }
  
  private String adminNodeFixture() throws Exception {
    String adminPath = storage.getDataLocation().getAdministrationLocation();
    if (!getSession().getRootNode().hasNode(adminPath)) {
      addNode(adminPath, "exo:administrationHome");
    }
    adminPath += "/forumAdministration";
    addNode(adminPath, "exo:administration");
    return adminPath;
  }

  private void assertAdminSaved(ForumAdministration admin) {
    String adminPath = storage.getDataLocation().getAdministrationLocation() + "/forumAdministration";
    assertNodeExists(adminPath);
    Node adminNode = getNode(adminPath);
    assertPropertyEquals(admin.getForumSortBy(), adminNode, "exo:forumSortBy");
    assertPropertyEquals(admin.getForumSortByType(), adminNode, "exo:forumSortByType");
    assertPropertyEquals(admin.getTopicSortBy(), adminNode, "exo:topicSortBy");
    assertPropertyEquals(admin.getTopicSortByType(), adminNode, "exo:topicSortByType");
    assertPropertyEquals(admin.getCensoredKeyword(), adminNode, "exo:censoredKeyword");
    assertPropertyEquals(admin.getEnableHeaderSubject(), adminNode, "exo:enableHeaderSubject");
    assertPropertyEquals(admin.getHeaderSubject(), adminNode, "exo:headerSubject");
    assertPropertyEquals(admin.getNotifyEmailContent(), adminNode, "exo:notifyEmailContent");
    assertPropertyEquals(admin.getNotifyEmailMoved(), adminNode, "exo:notifyEmailMoved");
  }

  static class TextForumAttachment extends ForumAttachment {
    private String text;

    public TextForumAttachment(String content) {
      this.text = content;
      setMimeType("text/plain");
      super.setSize(content.getBytes().length);
    }

    @Override
    public InputStream getInputStream() throws Exception {
      return new ByteArrayInputStream(text.getBytes("UTF-8"));
    }
  }

  private String stringOf(InputStream inputStream) {
    try {
      return new BufferedReader(new InputStreamReader(inputStream, "UTF-8")).readLine();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void _testDefaultAvatarWithMocks() throws Exception {
    JCRDataStorage storage = new JCRDataStorage();
    KSDataLocation locator = new KSDataLocation("wp");
    JCRSessionManager sessionManager = stubJCRSessionManager();
    locator.setSessionManager(sessionManager);
    storage.setDataLocator(locator);

    Node avatarHome = stubNodeForPath(Locations.KS_USER_AVATAR, sessionManager);
    storage.setDefaultAvatar("foo");
    verify(avatarHome).hasNode("foo"); // verify we tried to load the node

    Node avatar = stubChild(avatarHome, "foo", "nt:file");
    storage.setDefaultAvatar("foo");
    verify(avatar).remove();
    verify(avatarHome).save();
  }

  @SuppressWarnings("unchecked")
  private JCRSessionManager stubJCRSessionManager() {
    JCRSessionManager sessionManager = mock(JCRSessionManager.class);
    when(sessionManager.executeAndSave(any(JCRTask.class))).thenCallRealMethod();
    when(sessionManager.execute(any(JCRTask.class))).thenCallRealMethod();
    return sessionManager;
  }

  private Node stubChild(Node parent, String name, String type) throws Exception {
    Node child = mock(Node.class);
    when(child.isNodeType(type)).thenReturn(true);
    when(child.getName()).thenReturn(name);
    when(parent.getNode(name)).thenReturn(child);
    when(parent.hasNode(name)).thenReturn(true);
    return child;
  }

  private Node stubNodeForPath(String path, JCRSessionManager manager) throws Exception {
    Node node = mock(Node.class);
    Node root = mock(Node.class);
    when(root.getNode(path)).thenReturn(node);
    Session session = mock(Session.class);
    when(session.getRootNode()).thenReturn(root);
    when(manager.createSession()).thenReturn(session);
    when(manager.openSession()).thenReturn(session);
    when(manager.getCurrentSession()).thenReturn(session);
    return node;
  }

}
