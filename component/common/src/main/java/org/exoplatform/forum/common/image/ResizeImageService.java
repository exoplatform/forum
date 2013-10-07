/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common.image;

import java.io.InputStream;

/**
 * Resizes the dimension of images. Images can be resized by a specific width or height or both. 
 *
 */
public interface ResizeImageService {

  /**
   * Resizes an image to given dimensions.
   * 
   * @param imageName Name of the resized image.
   * @param is The input image.
   * @param requestWidth New width of the image.
   * @param requestHeight New height of the image.
   * @param keepAspectRatio The image's aspect ratio is kept or not.
   * @return The resized image.
   * @LevelAPI Platform
   */
  public InputStream resizeImage(String imageName,
                                 InputStream is,
                                 int requestWidth,
                                 int requestHeight,
                                 boolean keepAspectRatio) throws FileNotSupportedException;

  /**
   * Resizes an image to a given width.
   * 
   * @param imageName Name of the image to be resized.
   * @param is The input image.
   * @param requestWidth New width of the image.
   * @LevelAPI Platform
   */
  public InputStream resizeImageByWidth(String imageName, InputStream is, int requestWidth) throws FileNotSupportedException;

  /**
   * Resizes an image to a given height.
   * 
   * @param imageName Name of the image to be resized.
   * @param is The input image.
   * @param requestHeight New height of the image.
   * @return The resized image.
   * @LevelAPI Platform
   */
  public InputStream resizeImageByHeight(String imageName, InputStream is, int requestHeight) throws FileNotSupportedException;
}
