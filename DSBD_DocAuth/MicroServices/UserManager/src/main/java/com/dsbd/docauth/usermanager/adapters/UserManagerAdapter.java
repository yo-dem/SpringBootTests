package com.dsbd.docauth.usermanager.adapters;

import com.dsbd.docauth.usermanager.entities.User;
import com.dsbd.docauth.usermanager.services.UserManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping(path = "usersapi/")
public class UserManagerAdapter {
    @Autowired
    UserManagerService userManagerService;

    @PostMapping(path = "insertUser/")
    public @ResponseBody
    String insertUser(@RequestBody Map<String, String> u) {
        String resultUser;
        String resultToken;
        try {
            resultUser = userManagerService.insertUser(u.get("userName"), u.get("password"));
            resultToken = userManagerService.newUserTokenGift(u.get("userName"), u.get("password"));
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
        return resultUser + "\n" + resultToken;
    }

    @GetMapping(path = "getAllUsers/")
    public @ResponseBody
    Iterable<User> getAllUsers() {
        return userManagerService.getAllUsers();
    }

    @PostMapping(path = "updateUserPin/")
    public @ResponseBody
    String updateUserPin(@RequestBody Map<String, String> u) {
        try {
            return userManagerService.updateUserPin(u.get("userName"), u.get("password"));
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PutMapping(path = "updateUserPassword/{userName}/{pin}")
    public @ResponseBody
    String updateUserPassword(@PathVariable String userName, @PathVariable String pin, @RequestBody String newPassword) {
        try {
            return userManagerService.updateUserPassword(userName, pin, newPassword);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @DeleteMapping(path = "deleteUser/{userName}/{pin}")
    public @ResponseBody
    String deleteUser(@PathVariable String userName, @PathVariable String pin) {
        try {
            return userManagerService.deleteUser(userName, pin);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PostMapping(path = "getUserId/")
    public @ResponseBody
    String getUserId(@RequestBody Map<String, String> credentials) {
        try {
            return userManagerService.getUserId(credentials.get("userName"), credentials.get("password"));
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

}