package org.exoplatform.forum.integration.gamification;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.servlet.http.HttpServletRequest;
/*
 *
 *  * Copyright (C) 2003-2016 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */



public class ForumUtils {
    protected static Log       log                     = ExoLogger.getLogger(ForumUtils.class);

    public static final String POST                    = "post".intern();

    public static final String TAG                     = "Tag".intern();

    public static final String SLASH                   = "/".intern();

    public static final String EMPTY_STR               = "".intern();



    private static String buildForumLink(String url, String type, String id) {
        StringBuilder link = new StringBuilder(url);
        if (!isEmpty(type) && !isEmpty(id)) {
            if (link.lastIndexOf(SLASH) == (link.length() - 1))
                link.append(type);
            else
                link.append(SLASH).append(type);
            if (!id.equals(Utils.FORUM_SERVICE))
                link.append(SLASH).append(id);
        }
        return link.toString();
    }

    public static String createdForumLink(String type, String id, boolean isPrivate) {
        try {
            PortalRequestContext portalContext = Util.getPortalRequestContext();
            String fullUrl = ((HttpServletRequest) portalContext.getRequest()).getRequestURL().toString();
            String host = fullUrl.substring(0, fullUrl.indexOf(SLASH, 8));
            return new StringBuffer(host).append(createdSubForumLink(type, id, isPrivate)).toString();
        } catch (Exception e) {
            return id;
        }
    }

    public static String createdSubForumLink(String type, String id, boolean isPrivate) {
        try {
            String containerName = CommonsUtils.getService(ExoContainerContext.class).getPortalContainerName();
            String pageNodeSelected = Util.getUIPortal().getSelectedUserNode().getURI();
            PortalRequestContext portalContext = Util.getPortalRequestContext();
            return buildLink(portalContext.getPortalURI(), containerName, pageNodeSelected, type, id, isPrivate);
        } catch (Exception e) {
            return id;
        }
    }

    public static String buildLink(String portalURI, String containerName, String selectedNode, String type, String id, boolean isPrivate){
        StringBuilder sb = new StringBuilder();
        portalURI = portalURI.concat(selectedNode).concat(SLASH);
        if (!isPrivate) {
            sb.append(buildForumLink(portalURI, type, id));
        } else {
            String host = portalURI.substring(0, portalURI.indexOf(containerName) -1);
            sb.append(host)
                    .append(SLASH)
                    .append(containerName)
                    .append(SLASH)
                    .append("login?&initialURI=")
                    .append(buildForumLink(portalURI.replaceFirst(host, EMPTY_STR), type, id))
                    .toString();
        }
        return sb.toString();
    }





    public static boolean isEmpty(String str) {
        if (str == null || str.trim().length() == 0)
            return true;
        else
            return false;
    }

    public static boolean isArrayEmpty(String[] strs) {
        if (strs == null || strs.length == 0 || (strs.length == 1 && strs[0].trim().length() <= 0))
            return true;
        return false;
    }







  }
