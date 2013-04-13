/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.service.ws;

import java.util.List;

public class BeanToJsons<T> {
  private List<T> jsonList;

  public BeanToJsons(List<T> list) {
    this.jsonList = list;
  }

  public List<T> getJsonList() {
    return jsonList;
  }

  public void setJsonList(List<T> objectList) {
    this.jsonList = objectList;
  }
}
