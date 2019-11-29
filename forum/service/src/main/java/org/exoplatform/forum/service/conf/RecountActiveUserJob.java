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

import java.util.Calendar;

import org.quartz.*;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class RecountActiveUserJob implements Job {
  private static final Log LOG = ExoLogger.getLogger("job.forum.RecountActiveUserJob");

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer oldContainer = ExoContainerContext.getCurrentContainer();
    ExoContainer container = CommonUtils.getExoContainer(context);

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      ForumService forumService = container.getComponentInstanceOfType(ForumService.class);
      if (forumService != null) {
        JobDataMap jdatamap = context.getJobDetail().getJobDataMap();
        String lastPost = jdatamap.getString("lastPost");
        if (lastPost != null && lastPost.length() > 0) {
          int days = Integer.parseInt(lastPost);
          if (days > 0) {
            long oneDay = 86400000; // milliseconds of one day
            Calendar calendar = Calendar.getInstance();
            long currentDay = calendar.getTimeInMillis();
            currentDay = currentDay - (days * oneDay);
            calendar.setTimeInMillis(currentDay);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("//element(*,")
                         .append(Utils.USER_PROFILES_TYPE)
                         .append(")[")
                         .append("@exo:lastPostDate >= xs:dateTime('")
                         .append(ISO8601.format(calendar))
                         .append("')]");
            forumService.evaluateActiveUsers(stringBuilder.toString());
            if (LOG.isDebugEnabled()) {
              LOG.debug("\n\n The RecoundActiveUserJob have been done");
            }
          }
        }
      }
    } catch (NumberFormatException e) {
      LOG.warn("Value of days is not Integer number.", e);
    } catch (Exception e) {
      LOG.warn("An error occurred when recounting active users", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(oldContainer);
    }
  }
}
