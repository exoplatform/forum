/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 **/
package org.exoplatform.forum.service.conf;

import org.quartz.*;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class LoginJob implements Job {
  private static final Log LOG = ExoLogger.getLogger(LoginJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer oldContainer = ExoContainerContext.getCurrentContainer();
    ExoContainer container = CommonUtils.getExoContainer(context);

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      ForumService forumService = container.getComponentInstanceOfType(ForumService.class);
      String masterHost = System.getProperty("tenant.masterhost");
      if (masterHost == null) {
        forumService.updateLoggedinUsers();
      } else {
        ManageableRepository repository = SessionProviderService.getRepository();
        String currentLoginRepo = repository.getConfiguration().getName();
        forumService.updateLoggedinUsers(currentLoginRepo);
      }
      if (LOG.isDebugEnabled()) {
        LOG.info("\n\nForum Statistic has been updated for logged in user by a period login job");
      }
    } catch (Exception e) {
      LOG.warn("Period login job can not execute ...", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(oldContainer);
    }
  }
}
