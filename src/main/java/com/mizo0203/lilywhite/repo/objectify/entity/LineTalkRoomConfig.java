package com.mizo0203.lilywhite.repo.objectify.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.mizo0203.lilywhite.repo.objectify.OfyHelper;

import javax.annotation.Nullable;

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
  private String reminderMessage;
  private String reminderEnqueuedTaskName;
  private boolean cancellationConfirm;

  public LineTalkRoomConfig() {
    // LineTalkRoomConfig must have a no-arg constructor
    reminderMessage = null;
    reminderEnqueuedTaskName = null;
    cancellationConfirm = false;
  }

  /** A convenience constructor */
  public LineTalkRoomConfig(String sourceId) {
    this();
    this.sourceId = sourceId;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getReminderMessage() {
    return reminderMessage;
  }

  public void setReminderMessage(String reminderMessage) {
    this.reminderMessage = reminderMessage;
  }

  public boolean isReminderEnqueued() {
    return reminderEnqueuedTaskName != null;
  }

  @Nullable
  public String getReminderEnqueuedTaskName() {
    return reminderEnqueuedTaskName;
  }

  public void setReminderEnqueuedTaskName(String taskName) {
    reminderEnqueuedTaskName = taskName;
  }

  public boolean isCancellationConfirm() {
    return cancellationConfirm;
  }

  public void setCancellationConfirm(boolean cancellationConfirm) {
    this.cancellationConfirm = cancellationConfirm;
  }
}
