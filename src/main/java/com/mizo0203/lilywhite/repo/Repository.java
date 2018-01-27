package com.mizo0203.lilywhite.repo;

import com.mizo0203.lilywhite.domain.Define;
import com.mizo0203.lilywhite.repo.objectify.entity.KeyEntity;

import java.io.IOException;
import java.util.logging.Logger;

public class Repository {

  private static final Logger LOG = Logger.getLogger(Repository.class.getName());
  private final OfyRepository mOfyRepository;
  private final LineRepository mLineRepository;

  public Repository() {
    mOfyRepository = new OfyRepository();
    mLineRepository = new LineRepository();
  }

  public void destroy() {
    mOfyRepository.destroy();
    mLineRepository.destroy();
  }

  public void authorizeOauth(AuthorizeOauthCallback callback) throws IOException {
    String client_id = getClientId();
    String redirect_uri_str = Define.REDIRECT_URI_STR;
    mLineRepository.authorizeOauth(client_id, redirect_uri_str, callback);
  }

  private String getClientId() {
    KeyEntity keyEntity = mOfyRepository.loadKeyEntity("ClientId");
    if (keyEntity == null) {
      keyEntity = new KeyEntity();
      keyEntity.key = "ClientId";
      keyEntity.value = "";
      mOfyRepository.saveKeyEntity(keyEntity);
    }
    if (keyEntity.value.isEmpty()) {
      LOG.severe("ClientId isEmpty");
    }
    return keyEntity.value;
  }
}
