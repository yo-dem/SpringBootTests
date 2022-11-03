package com.dsbd.docauth.tokenmanager.adapters;

import com.dsbd.docauth.tokenmanager.entities.Token;
import com.dsbd.docauth.tokenmanager.services.TokenManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
@RequestMapping(path = "tokensapi/")
public class TokenManagerAdapter {
    @Autowired
    TokenManagerService tokenManagerService;

    @PostMapping(path = "addTokensToUser/{amount}")
    public @ResponseBody
    String addTokensToUser(@RequestBody Map<String, String> u, @PathVariable Integer amount) {
        try {
            return tokenManagerService.addTokensToUser(u, amount);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PostMapping(path = "getUserTokens/")
    public @ResponseBody
    String getUserTokens(@RequestBody Map<String, String> u) {
        try {
            return tokenManagerService.getUserTokens(u);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @GetMapping(path = "getAllTokens/")
    public @ResponseBody
    Iterable<Token> getAllTokens() {
        return tokenManagerService.getAllTokens();
    }

    @PostMapping(path = "decreaseUserTokens/")
    public @ResponseBody
    String decreaseUserTokens(@RequestBody Map<String, String> u) {
        try {
            return tokenManagerService.decreaseUserTokens(u);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PostMapping(path = "increaseUserTokens/")
    public @ResponseBody
    String increaseUserTokens(@RequestBody Map<String, String> u) {
        try {
            return tokenManagerService.increaseUserTokens(u);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @DeleteMapping(path = "deleteUserTokens/{userOwnerId}")
    public @ResponseBody
    String deleteUserTokens(@PathVariable String userOwnerId) {
        try {
            return tokenManagerService.deleteUserTokens(userOwnerId);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PostMapping(path = "documentCreatedEvent/")
    public @ResponseBody
    String documentCreatedEvent(@RequestBody Map<String, String> u) {
        try {
            return tokenManagerService.documentCreatedEvent(u);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }
}
