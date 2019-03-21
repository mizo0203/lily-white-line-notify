package com.mizo0203.lilywhite.repo.objectify.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.mizo0203.lilywhite.repo.objectify.OfyHelper;

/**
 * The @Entity tells Objectify about our entity. We also register it in {@link OfyHelper} Our
 * primary key @Id is set automatically by the Google Datastore for us.
 *
 * <p>We add a @Parent to tell the object about its ancestor. We are doing this to support many
 * guestbooks. Objectify, unlike the AppEngine library requires that you specify the fields you want
 * to index using @Index. Only indexing the fields you need can lead to substantial gains in
 * performance -- though if not indexing your data from the start will require indexing it later.
 *
 * <p>NOTE - all the properties are PUBLIC so that can keep the code simple.
 */
@Entity
public class LineTalkRoomConfig {

  @Id private String sourceId;
  private String nickname;
  private Long editingReminderId;

  private LineTalkRoomConfig() {
    // LineTalkRoomConfig must have a no-arg constructor
  }

  /** A convenience constructor */
  public LineTalkRoomConfig(String sourceId) {
    this();
    this.sourceId = sourceId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getSourceId() {
    return sourceId;
  }

  public Long getEditingReminderId() {
    return editingReminderId;
  }

  public void setEditingReminderId(Long editingReminderId) {
    this.editingReminderId = editingReminderId;
  }
}
