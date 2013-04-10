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

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.forum.rendering.api.Renderer;
import org.exoplatform.forum.rendering.api.UnsupportedSyntaxException;
import org.exoplatform.forum.rendering.spi.MarkupRenderDelegate;
import org.exoplatform.forum.rendering.spi.RendererPlugin;

/**
 * @version $Revision$
 */
public class MarkupRenderingService {

  protected Map<String, Renderer> rendererRegistry;

  public MarkupRenderingService() {
    rendererRegistry = new HashMap<String, Renderer>();
  }

  public void registerRenderer(RendererPlugin plugin) {
    registerRenderer(plugin.getRenderer());
  }

  /**
   * Registers a render.
   * 
   * @param renderer
   * @LevelAPI Platform
   */
  public void registerRenderer(Renderer renderer) {
    rendererRegistry.put(renderer.getSyntax(), renderer);
  }

  /**
   * Gets Render by provided syntax.
   * 
   * @param syntax The syntax to register.
   * @return
   * @LevelAPI Platform
   */
  public Renderer getRenderer(String syntax) {
    Renderer r = rendererRegistry.get(syntax);
    if (r == null) {
      throw new UnsupportedSyntaxException("No renderer has been registered for syntax " + syntax);
    }
    return r;
  }

  /**
   * Convenience method to delegate markup of an object.
   * 
   * @param delegate
   * @param target
   * @return
   * @LevelAPI Platform
   */
  public <T> String delegateRendering(MarkupRenderDelegate<T> delegate, T target) {
    String message = delegate.getMarkup(target);
    if (message != null) {
      Renderer renderer = getRenderer(delegate.getSyntax(target));
      return renderer.render(message);
    }
    return null;
  }

}
