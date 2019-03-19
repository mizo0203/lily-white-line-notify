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
public class Channel {

  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  @Id
  private long id;

  private String secret;
  private String token;

  public Channel() {
    // Channel must have a no-arg constructor
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  @Nullable
  public String getToken() {
    return token;
  }

  public void setToken(String taskName) {
    token = taskName;
  }
}
