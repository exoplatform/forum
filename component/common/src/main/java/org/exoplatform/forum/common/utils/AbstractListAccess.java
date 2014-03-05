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
package org.exoplatform.forum.common.utils;

import org.exoplatform.commons.utils.ListAccess;

public abstract class AbstractListAccess<E> implements ListAccess<E> {
  private int   currentPage = 1;

  private int   totalPage   = 1;

  private int   pageSize    = 0;

  private int   from        = 0;

  private int   to          = 0;

  protected int size        = -1;

  public abstract E[] load(int pageSelect) throws Exception, IllegalArgumentException;

  /**
   * Re-calculate the value of currentPage
   * @param offset
   * @param limit
   */
  public void reCalculate(int offset, int limit) {
    if (offset >= 0) {
      currentPage = (offset / limit) + 1;
    }
  }

  /**
   * Initialize page list access
   * @param pageSize
   * @param pageSelect
   * @throws Exception
   */
  public void initialize(int pageSize, int pageSelect) throws Exception {
    this.setPageSize(pageSize);
    this.getTotalPages();
    this.setCurrentPage(pageSelect);
  }

  /**
   *  Get total page of page list access
   * @return
   * @throws Exception
   */
  public int getTotalPages() throws Exception {
    this.totalPage = getSize() / pageSize;
    if (getSize() % pageSize > 0) {
      totalPage++;
    }
    return this.totalPage;
  }

  /**
   * Get the page size of page list access
   * @return
   */
  public int getPageSize() {
    to = from + pageSize - 1;
    if (to > size) {
      to = size - 1;
    }
    return pageSize;
  }

  /**
   * Set the page size of page list access
   * @param pageSize
   */
  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Get the currentPage of page list access
   * @return
   */
  public int getCurrentPage() {
    return currentPage;
  }

  /**
   * Set the currentPage of page list access
   * @param page
   */
  public void setCurrentPage(int page) {
    if (page > totalPage && totalPage > 0) {
      currentPage = totalPage;
    } else if (page <= 0) {
      currentPage = 1;
    } else {
      currentPage = page;
    }
  }

  /**
   * Get the offset by page
   * @param page
   * @return
   */
  public int getOffset(int page) {
    return (page - 1) * pageSize;
  }

  /**
   * Get current offset
   * @return
   */
  public int getOffset() {
    from = getOffset(currentPage);
    return from;
  }

  /**
   * Get fromIndex of page
   * @return
   */
  public int getFrom() {
    return from;
  }

  /**
   * Get toIndex of page
   * @return
   */
  public int getTo() {
    return to;
  }
}