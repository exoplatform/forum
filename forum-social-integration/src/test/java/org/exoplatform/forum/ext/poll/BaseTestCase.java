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
package org.exoplatform.forum.ext.poll;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.common.jcr.SessionManager;
import org.exoplatform.poll.service.PollService;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.search.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.component.service.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.test.portal-configuration.xml")
})

public abstract class BaseTestCase extends BaseExoTestCase {
  protected KSDataLocation    dataLocation;

  protected PollService       pollService;

  private final static String categoryId = "forumCategoryabctest";

  private final static String forumId    = "forumtheidforumoftest";

  private final static String topicId    = "topicthetopicoftest";

  public String               topicPath  = null;

  @Override
  public void setUp() throws Exception {
    //
    begin();
    dataLocation = (KSDataLocation) getService(KSDataLocation.class);
    pollService = (PollService) getService(PollService.class);
    initForumdata();
  }

  @Override
  public void tearDown() throws Exception {
    removeForumData();
    //
    end();
  }

  public PollService getPollService() {
    return pollService;
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  /**
   * Create new forum node, new topic node
   */
  private void initForumdata() {
    SessionManager manager = dataLocation.getSessionManager();
    try {
      Session session = manager.openSession();
      Node nodeHome = session.getRootNode().getNode(dataLocation.getForumCategoriesLocation());

      if (!nodeHome.hasNode(categoryId)) {
        Node catN = nodeHome.addNode(categoryId, "exo:forumCategory");
        Node forNode = catN.addNode(forumId, "exo:forum");
        Node topicNode = forNode.addNode(topicId, "exo:topic");
        topicPath = topicNode.getPath();
        session.save();
      } else {
        Node topicNode = nodeHome.getNode(categoryId + "/" + forumId + "/" + topicId);
        topicPath = topicNode.getPath();
      }

      assertNotNull(session.getItem(topicPath));
    } catch (Exception e) {
    } finally {
      manager.closeSession();
    }
  }

  private void removeForumData() {
    SessionManager manager = dataLocation.getSessionManager();
    try {
      Session session = manager.openSession();
      Node nodeHome = session.getRootNode().getNode(dataLocation.getForumCategoriesLocation());

      if (nodeHome.hasNode(categoryId)) {
        nodeHome.getNode(categoryId).remove();
        session.save();
      }
    } catch (Exception e) {
    } finally {
      manager.closeSession();
    }
  }
}
