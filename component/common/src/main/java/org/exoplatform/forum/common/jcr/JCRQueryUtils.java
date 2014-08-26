/***************************************************************************
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.forum.common.jcr;

public class JCRQueryUtils {
  
  private static final String SINGLE_QUOTE = "'";
  
  private static final String DOUBLE_QUOTE = "''";

  /**
   * replace simple quote by double quote before process xPath query 
   * 
   * @param value the value to search with xPath
   * @return string after process
   */
  public static String escapeSimpleQuoteCharacter(String value) {
    return value.replace(SINGLE_QUOTE, DOUBLE_QUOTE);
  }
}
