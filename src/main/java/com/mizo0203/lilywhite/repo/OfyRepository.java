package com.mizo0203.lilywhite.repo;

import com.googlecode.objectify.ObjectifyService;
import com.mizo0203.lilywhite.repo.objectify.entity.Channel;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;
import com.mizo0203.lilywhite.repo.objectify.entity.Reminder;

/* package */ public class OfyRepository {

  /* package */ void deleteLineTalkRoomConfig(String key) {
    ObjectifyService.ofy().delete().type(LineTalkRoomConfig.class).id(key).now();
  }

  public LineTalkRoomConfig loadLineTalkRoomConfig(String source_id) {
    LineTalkRoomConfig config =
        ObjectifyService.ofy().load().type(LineTalkRoomConfig.class).id(source_id).now();
    if (config == null) {
      config = new LineTalkRoomConfig(source_id);
    }
    return config;
  }

  public void saveLineTalkRoomConfig(LineTalkRoomConfig entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }

  public Reminder factoryReminder() {
    return loadReminder(ObjectifyService.ofy().save().entity(new Reminder()).now().getId());
  }

  public Reminder loadReminder(long id) {
    return ObjectifyService.ofy().load().type(Reminder.class).id(id).now();
  }

  public void saveReminder(Reminder entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }

  /* package */ KeyEntity loadKeyEntity(String key) {
    return ObjectifyService.ofy().load().type(KeyEntity.class).id(key).now();
  }

  /* package */ void saveKeyEntity(KeyEntity entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }

  /* package */ void deleteKeyEntity(String key) {
    ObjectifyService.ofy().delete().type(KeyEntity.class).id(key).now();
  }

  /* package */ Channel loadChannel(long id) {
    return ObjectifyService.ofy().load().type(Channel.class).id(id).now();
  }

  /* package */ void saveChannel(Channel entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }
}
