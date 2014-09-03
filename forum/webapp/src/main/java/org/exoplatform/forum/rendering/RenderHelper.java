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
package org.exoplatform.forum.rendering;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.common.webui.BuildRendering.AbstractRenderDelegate;
import org.exoplatform.forum.service.Post;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class RenderHelper {
  protected static final Log LOG = ExoLogger.getLogger(RenderHelper.class);
  private MarkupRenderingService markupRenderingService;
  public RenderHelper() {
  }

  private static final AbstractRenderDelegate<Post> POST_DELEGATE = new AbstractRenderDelegate<Post>() {
    @Override
    public String getMessage(Post post) {
      return post.getMessage();
    }
  };
  
  /**
   * Render markup for a forum Post
   * 
   * @param post
   * @return
   */
  public String renderPost(Post post) {
    try {
      return getMarkupRenderingService().delegateRendering(POST_DELEGATE, post);
    } catch (Exception e) {
      LOG.warn("Failed to render post: " + post.getId());
      LOG.debug(e.getMessage(), e);
      return  post.getMessage();
    }
  }
  
  public MarkupRenderingService getMarkupRenderingService() {
    if (markupRenderingService == null) {
      markupRenderingService = CommonsUtils.getService(MarkupRenderingService.class);
    }
    return markupRenderingService;
  }

  public void setMarkupRenderingService(MarkupRenderingService service) {
    this.markupRenderingService = service;
  }
}
