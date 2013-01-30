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
package org.exoplatform.forum.create;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Jan 3, 2013  
 */
public class CreateUtils {

  public static List<String> getAllPortalNames() {
    UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                                                                                       .getComponentInstanceOfType(UserPortalConfigService.class);
    try {
      return dataStorage.getAllPortalNames();
    } catch (Exception e) {
      return new ArrayList<String>();
    }
  }

  public static String getCurrentPortalName() {
    UIPortal uiPortal = Util.getUIPortal();
    SiteKey siteKey = uiPortal.getSiteKey();
    if(siteKey.getType() == SiteType.PORTAL) {
      return siteKey.getName();
    }
    return SiteType.GROUP.name();
  }
}
