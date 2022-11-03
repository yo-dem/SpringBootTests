package com.dsbd.docauth.usermanager.entities;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Integer id;
    @NotBlank
    @Column(unique = true)
    String userName;
    @NotBlank String password;
    @Column(unique = true)
    String pin;

    public User() {
    }

    public User(Integer id, String userName, String password, String pin) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.pin = pin;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", userName='" + userName + '\'' + ", password='" + password + '\'' + ", pin='" + pin + '\'' + '}';
    }
}
