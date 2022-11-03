package com.dsbd.docauth.tokenmanager.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Integer id;
    @NotNull
    @Column(unique = true)
    Integer userOwnerId;
    @NotNull
    Integer amount;

    public Token() {
    }

    public Token(Integer id, Integer userOwnerId, Integer amount) {
        this.id = id;
        this.userOwnerId = userOwnerId;
        this.amount = amount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserOwnerId() {
        return userOwnerId;
    }

    public void setUserOwnerId(Integer userOwnerId) {
        this.userOwnerId = userOwnerId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Token{" +
                "id=" + id +
                ", userOwnerId=" + userOwnerId +
                ", amount=" + amount +
                '}';
    }
}
