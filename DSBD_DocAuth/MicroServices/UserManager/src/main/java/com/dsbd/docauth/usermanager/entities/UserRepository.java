package com.dsbd.docauth.usermanager.entities;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {
    User getUserByUserName(String userName);
    User getUserByUserNameAndPassword(String userName, String password);
    User getUserByUserNameAndPin(String userName, String pin);

}
