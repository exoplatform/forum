/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.common;

import org.exoplatform.container.xml.InitParams;

public class InitParamsValue {

  /**
   * Gets the String value of InitParams by input key
   * 
   * @param params The InitParams
   * @param key The key to get value
   * @param defaultValue The default value
   * @return String value
   */
  public static String getString(InitParams params, String key, String defaultValue) {
    try {
      return params.getValueParam(key).getValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets the Long value of InitParams by input key
   * 
   * @param params The InitParams
   * @param key The key to get value
   * @param defaultValue The default value
   * @return Long value
   */
  public static long getLong(InitParams params, String key, long defaultValue) {
    try {
      return Long.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets the Double value of InitParams by input key
   * 
   * @param params The InitParams
   * @param key The key to get value
   * @param defaultValue The default value
   * @return Double value
   */
  public static double getDouble(InitParams params, String key, double defaultValue) {
    try {
      return Double.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets the Integer value of InitParams by input key
   * 
   * @param params The InitParams
   * @param key The key to get value
   * @param defaultValue The default value
   * @return Integer value
   */
  public static int getInteger(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Gets the Boolean value of InitParams by input key
   * 
   * @param params The InitParams
   * @param key The key to get value
   * @return Boolean value
   */
  public static boolean getBoolean(InitParams params, String key) {
    try {
      return Boolean.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return false;
    }
  }
}
