package com.mizo0203.lilywhite.repo;

import com.googlecode.objectify.ObjectifyService;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;

/* package */ class OfyRepository {

  @SuppressWarnings("EmptyMethod")
  public void destroy() {
    // NOP
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
}
