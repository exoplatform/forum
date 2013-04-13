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
package org.exoplatform.forum;

import java.io.IOException;
import java.io.InputStream;

import org.exoplatform.download.DownloadResource;
import org.exoplatform.forum.service.BufferAttachment;

public class ForumDownloadResource extends DownloadResource {
  
  private BufferAttachment attachment;

  public ForumDownloadResource(String downloadType, String resourceMimeType) {
    super(downloadType, resourceMimeType);
  }

  public BufferAttachment getAttachment() {
    return attachment;
  }

  public void setAttachment(BufferAttachment attachment) {
    this.attachment = attachment;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    try {
      return attachment.getInputStream();
    } catch (Exception e) {
      return null;
    }
  }

}
