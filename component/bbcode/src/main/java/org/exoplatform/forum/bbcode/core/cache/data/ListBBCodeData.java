/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.bbcode.core.cache.data;

import java.util.List;

import org.exoplatform.forum.bbcode.core.cache.key.BBCodeKey;
import org.exoplatform.forum.common.cache.model.AbstractListData;
public class ListBBCodeData extends AbstractListData<BBCodeKey> {
  private static final long serialVersionUID = 1L;

  public ListBBCodeData(List<BBCodeKey> ids) {
    super(ids);
  }

}
