package org.exoplatform.forum.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SortSettings {

  private SortField field     = SortField.ORDER; // Name , Order, createdDate, laspostDate, postCount, topicCount, isLock, lastPostDate, viewCount, numberAttachments

  private Direction direction = Direction.ASC;  // ascending or descending

  public SortSettings(String field, String direction) {
    this.field = toField(field);
    this.direction = toDirection(direction);
  }

  public SortSettings(SortField field, Direction direction) {
    this.field = field;
    this.direction = direction;
  }

  public SortField getField() {
    return field;
  }

  public Direction getDirection() {
    return direction;
  }

  private SortField toField(String sortBy) {
    if (SortField.NAME.toString().equalsIgnoreCase(sortBy))
      return SortField.NAME;
    else if (SortField.CREATED.toString().equalsIgnoreCase(sortBy))
      return SortField.CREATED;
    else if (SortField.ISLOCK.toString().equalsIgnoreCase(sortBy))
      return SortField.ISLOCK;
    else if (SortField.TOPICCOUNT.toString().equalsIgnoreCase(sortBy))
      return SortField.TOPICCOUNT;
    else if (SortField.LASTPOST.toString().equalsIgnoreCase(sortBy))
      return SortField.LASTPOST;
    else if (SortField.POSTCOUNT.toString().equalsIgnoreCase(sortBy))
      return SortField.POSTCOUNT;
    else if (SortField.VIEWCOUNT.toString().equalsIgnoreCase(sortBy))
      return SortField.VIEWCOUNT;
    else if (SortField.ATTACHMENTS.toString().equalsIgnoreCase(sortBy))
      return SortField.ATTACHMENTS;
    else if (SortField.MODIFIEDDATE.toString().equalsIgnoreCase(sortBy))
      return SortField.MODIFIEDDATE;
    else
      return SortField.ORDER;
  }

  private Direction toDirection(String direction) {
    return (Direction.DESCENDING.toString().indexOf(direction.toLowerCase()) == 0) ? Direction.DESC : Direction.ASC;
  }

  public enum Direction {
    ASC("ASC"), ASCENDING("ascending"), DESC("DESC"), DESCENDING("descending");
    private final String name;

    Direction(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }

    public static String[] toValues() {
      int length = Direction.values().length;
      String[] values = new String[length];
      for (int i = 0; i < length; ++i) {
        values[i] = Direction.values()[i].toString();
      }
      return values;
    }
  };

  public enum SortField {
    ORDER("forumOrder"), NAME("name"), ISLOCK("isLock"), CREATED("createdDate"), MODIFIEDDATE("modifiedDate"),
    LASTPOST("lastPostDate"), TOPICCOUNT("topicCount"), POSTCOUNT("postCount"),
    VIEWCOUNT("viewCount"), ATTACHMENTS("numberAttachments");

    private final String name;

    SortField(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }

    public static String[] toValues() {
      int length = SortField.values().length;
      String[] values = new String[length];
      for (int i = 0; i < length; ++i) {
        values[i] = SortField.values()[i].toString();
      }
      return values;
    }
  };
  
  public static List<String> getDirections() {
    return Arrays.asList(Direction.toValues());
  }

  public static List<String> getTopicSortBys() {
    List<String> listOrderBy = new ArrayList<String>(Arrays.asList(SortField.toValues()));
    listOrderBy.remove(SortField.ORDER.toString());
    listOrderBy.remove(SortField.TOPICCOUNT.toString());
    return listOrderBy;
  }

  public static List<String> getForumSortBys() {
    List<String> listOrderBy = new ArrayList<String>(Arrays.asList(SortField.toValues()));
    listOrderBy.remove(SortField.POSTCOUNT.toString());
    listOrderBy.remove(SortField.VIEWCOUNT.toString());
    listOrderBy.remove(SortField.ATTACHMENTS.toString());
    return listOrderBy;
  }

}
