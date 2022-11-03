package com.dsbd.docauth.tokenmanager.entities;

import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, Integer> {
    Token getTokenByUserOwnerId(Integer userOwnerId);
    void deleteByUserOwnerId(Integer userOwnerId);
}
