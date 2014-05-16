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
package org.exoplatform.forum.service.updater;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.ForumNodeTypes;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


@Managed
@NameTemplate( { @Property(key = "product.group.id", value = "org.exoplatform.forum") })
@ManagedDescription("Plugin that allows to migration category has add new node-type.")
public class ForumServiceUpdaterPlugin extends UpgradeProductPlugin {
  private static Log LOG      = ExoLogger.getLogger(ForumServiceUpdaterPlugin.class);
  
  public ForumServiceUpdaterPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    try {
      registerNodeTypes("jar:/conf/portal/forum-nodetypes.xml", ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
      registerNodeTypes("jar:/conf/portal/forum-migrate-nodetypes.xml", ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
      //
      upgradeCategorySpace();
      LOG.info(String.format("Successfully to migrate forum from %s to %s", oldVersion, newVersion));
    } catch (Exception e) {
      LOG.warn(String.format("Failed to migrate forum from %s to %s", oldVersion, newVersion), e);
    }
  }

  private void upgradeCategorySpace() throws Exception {
    KSDataLocation dataLocator = CommonUtils.getComponent(KSDataLocation.class);
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Session session = dataLocator.getSessionManager().getSession(sProvider);
    Node cateHome = session.getRootNode().getNode(dataLocator.getForumCategoriesLocation());
    NodeIterator iter = cateHome.getNodes();
    while (iter.hasNext()) {
      addMixinForumCategory(iter.nextNode());
    }
    //
    session.save();
  }

  private void addMixinForumCategory(Node cateNode) {
    try {
      if (!cateNode.isNodeType(ForumNodeTypes.MIXIN_FORUM_CATEGORY)) {
        cateNode.addMixin(ForumNodeTypes.MIXIN_FORUM_CATEGORY);
        cateNode.setProperty(ForumNodeTypes.EXO_INCLUDED_SPACE, cateNode.getName().equals(Utils.CATEGORY_SPACE_ID_PREFIX));
      }
    } catch (Exception e) {
      LOG.warn(String.format("Upgrade the category %s is unsuccessful", cateNode.toString()), e);
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
  
  private static void registerNodeTypes(String nodeTypeFilesName, int alreadyExistsBehaviour) throws Exception {
    ConfigurationManager configurationService = CommonUtils.getComponent(ConfigurationManager.class);
    InputStream isXml = configurationService.getInputStream(nodeTypeFilesName);
    RepositoryService repositoryService = CommonUtils.getComponent(RepositoryService.class);
    ExtendedNodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager();
    ntManager.registerNodeTypes(isXml, alreadyExistsBehaviour, NodeTypeDataManager.TEXT_XML);
  }

}
