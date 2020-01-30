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
package org.exoplatform.forum.service.user;

import org.quartz.*;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class AutoPruneJob implements Job {
  private static final Log LOG = ExoLogger.getLogger(AutoPruneJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer oldContainer = ExoContainerContext.getCurrentContainer();
    ExoContainer container = CommonUtils.getExoContainer(context);

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String desc = context.getJobDetail().getDescription();
      ForumService forumService = container.getComponentInstanceOfType(ForumService.class);
      RepositoryService repositoryService = container.getComponentInstanceOfType(RepositoryService.class);
      repositoryService.setCurrentRepositoryName(context.getJobDetail().getJobDataMap().getString(Utils.CACHE_REPO_NAME));
      forumService.runPrune(desc);
      if (LOG.isDebugEnabled()) {
        LOG.debug("\n\nAuto prune has worked on " + desc + " forum");
      }
    } catch (Exception e) {
      LOG.debug("\n\n >>>>>> AutoPrune Job error" + e.getMessage());
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(oldContainer);
    }
  }

}
