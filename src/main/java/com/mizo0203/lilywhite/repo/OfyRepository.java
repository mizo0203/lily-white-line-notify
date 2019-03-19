package com.mizo0203.lilywhite.repo;

import com.googlecode.objectify.ObjectifyService;
import com.mizo0203.lilywhite.repo.objectify.entity.Channel;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;
import com.mizo0203.lilywhite.repo.objectify.entity.LineTalkRoomConfig;

/* package */ public class OfyRepository {

  public void deleteLineTalkRoomConfig(String key) {
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

  public KeyEntity loadKeyEntity(String key) {
    return ObjectifyService.ofy().load().type(KeyEntity.class).id(key).now();
  }

  public void saveKeyEntity(KeyEntity entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }

  public void deleteKeyEntity(String key) {
    ObjectifyService.ofy().delete().type(KeyEntity.class).id(key).now();
  }

  public Channel loadChannel(long id) {
    return ObjectifyService.ofy().load().type(Channel.class).id(id).now();
  }

  public void saveChannel(Channel entity) {
    ObjectifyService.ofy().save().entity(entity).now();
  }
}
