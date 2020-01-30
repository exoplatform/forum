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
package org.exoplatform.forum.bbcode.base;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.bbcode.api.BBCode;
import org.exoplatform.forum.bbcode.api.BBCodeService;
import org.exoplatform.forum.bbcode.core.BBCodeServiceImpl;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.common.jcr.SessionManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.bbcode.component.core.test.dependencies-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.bbcode.component.core.test.configuration.xml"),
})
public abstract class BaseBBcodeTestCase extends BaseExoTestCase {

  protected BBCodeServiceImpl bbcodeServiceImpl;

  protected BBCodeService     bbcodeService;

  protected KSDataLocation    locator;

  protected SessionManager    sessionManager;

  protected String            bbcodesPath;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    //
    begin();
    
    bbcodeServiceImpl = new BBCodeServiceImpl();
    locator = getService(KSDataLocation.class);
    bbcodeService = getService(BBCodeService.class);
    
    bbcodeServiceImpl.setDataLocator(locator);
    sessionManager = locator.getSessionManager();
    bbcodesPath = bbcodeServiceImpl.getDataLocator().getBBCodesLocation();
    
  }

  @Override
  public void tearDown() throws Exception {
    for (BBCode bbcode : bbcodeService.getAll()) {
      bbcodeService.delete(bbcode.getId());
    }
    //
    end();
  }
  
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
  
  public BBCode createBBCode(String tag, String replacement, String description, String example, boolean option, boolean active) {
    BBCode bbc = new BBCode();
    bbc.setTagName(tag);
    bbc.setReplacement(replacement);
    bbc.setDescription(description);
    bbc.setExample(example);
    bbc.setOption(option);
    bbc.setActive(active);
    return bbc;
  }
  
  public boolean nodeExists(String path) {
    boolean isExist = false;
    Node node = getNode(path);
    isExist = (node != null) ? true : false;
    if (isExist) {
      try {
        node.getSession().logout();
      } catch (RepositoryException e) {
      }
    }
    return isExist;
  }

  public void assertNodeExists(String path) {
    assertTrue(nodeExists(path));
  }

  public void assertNodeNotExists(String path) {
    assertFalse(nodeExists(path));
  }

  public Node getNode(String path) {
    SessionProvider provider = SessionProvider.createSystemProvider();
    if (path.indexOf("/") == 0) {
      path = path.substring(0);
    }
    Session session = sessionManager.getSession(provider);
    try {
      return session.getRootNode().getNode(path);
    } catch (Exception e) {
      session.logout();
      return null;
    }
  }
  
  public void assertContains(List<String> actual, String... strs) {
    for (int i = 0; i < strs.length; i++) {
      if(!actual.contains(strs[i])) {
        assertTrue(false);
      }
    }
    assertTrue(true);
  }

}
