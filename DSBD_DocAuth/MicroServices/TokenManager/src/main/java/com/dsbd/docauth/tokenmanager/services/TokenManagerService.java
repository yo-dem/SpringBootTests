package com.dsbd.docauth.tokenmanager.services;

import com.dsbd.docauth.tokenmanager.entities.Token;
import com.dsbd.docauth.tokenmanager.entities.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@PropertySource("classpath:application.properties")
public class TokenManagerService {
    @Autowired
    TokenRepository tokenRepository;
    @Autowired
    private Environment env;

    /*******************************************************************************************************************
     ADDTOKENSTOUSER (POST)
     Permette di caricare un quantitativo di token stabilito ed
     associarlo a un utente identificato da nome utente e password.
     *******************************************************************************************************************/
    @Transactional
    public String addTokensToUser(Map<String, String> u, Integer amount) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String idUser;
        try {
            // Richiama il servizio usermanager per ottenere l'id dell'utente passato mediante
            // user name e pasword
            idUser = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
            if (idUser == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.\n Utente non in archivio o password errata?");
        }

        int i_IdUser;
        try {
            i_IdUser = Integer.parseInt(idUser);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio.");
        }

        Token t;
        t = tokenRepository.getTokenByUserOwnerId(i_IdUser);
        if (t != null) {
            t.setAmount(t.getAmount() + amount);
            tokenRepository.save(t);
        } else {
            Token newToken = new Token();
            newToken.setUserOwnerId(Integer.parseInt(idUser));
            newToken.setAmount(amount);
            tokenRepository.save(newToken);
        }
        return "Token aggiornati.\n";

    }

    /*******************************************************************************************************************
     GETUSERTOKENS (POST)
     Permette di ottenere il numero di token accreditati a un utente identificato da nome utente e password.
     *******************************************************************************************************************/
    public String getUserTokens(Map<String, String> u) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String idUser;
        try {
            idUser = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
            if (idUser == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o pasword errata.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.\n Utente non in archivio o pasword errata?");
        }

        int i_IdUser;
        try {
            i_IdUser = Integer.parseInt(idUser);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        String tokenAmount;
        try {
            tokenAmount = tokenRepository.getTokenByUserOwnerId(i_IdUser).getAmount().toString();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'Utente non dispone di record token da eliminare.");
        }
        return tokenAmount;
    }

    /*******************************************************************************************************************
     GETALLTOKENS (GET)
     Metodo di debug che permette di ottenere il set di token accreditati a ogi utente.
     *******************************************************************************************************************/
    public Iterable<Token> getAllTokens() {
        return tokenRepository.findAll();
    }

    /*******************************************************************************************************************
     DECREASEUSERTOKENS (POST)
     Decrementa di uno il numero di token accreditati a un utente identificato da nome utente e password.
     *******************************************************************************************************************/
    public String decreaseUserTokens(Map<String, String> u) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String idUser;
        try {
            idUser = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.\n Utente non in archivio o pasword errata?");
        }

        try {
            Integer.parseInt(idUser);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        Token t = tokenRepository.getTokenByUserOwnerId(Integer.parseInt(idUser));
        if (t == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'Utente non dispone di record token da eliminare.");

        if (t.getAmount() > 0)
            t.setAmount(t.getAmount() - 1);
        else
            return "L'utente non dispone di crediti sufficienti";

        tokenRepository.save(t);
        return "Token aggiornati.\nToken rimasti: " + t.getAmount();
    }

    /*******************************************************************************************************************
     INCREASEUSERTOKENS (POST)
     Incrementa di uno il numero di token accreditati a un utente identificato da nome utente e password.
     *******************************************************************************************************************/
    @Transactional
    public String increaseUserTokens(Map<String, String> u) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String idUser;
        try {
            idUser = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.\n Utente non in archivio o pasword errata?");
        }

        try {
            Integer.parseInt(idUser);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        Token t = tokenRepository.getTokenByUserOwnerId(Integer.parseInt(idUser));
        if (t == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'Utente non dispone di record token da eliminare.");

        t.setAmount(t.getAmount() + 1);
        tokenRepository.save(t);
        return "Token aggiornati.\nToken rimasti: " + t.getAmount();
    }

    /*******************************************************************************************************************
     DELETEUSERTOKENS (DELETE)
     Elimina i token di un utente quando questo viene eliminato.
     *******************************************************************************************************************/
    @Transactional
    public String deleteUserTokens(String userOwnerId) {
        try {
            tokenRepository.deleteByUserOwnerId(Integer.parseInt(userOwnerId));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage() + " Impossibile completare l'operazione.");
        }
        return "Token record cancellato con successo.";
    }


    /*******************************************************************************************************************
     DOCUMENTCREATEEVENT (Metodo di servizio.)
     Riceve un messaggio proveniente dal DocumentManager che attesta la creazione di un documento, fornendo l'id
     del documento appena creato e l'id dell'utente su cui è necessario effettuare verifiche che comporteranno
     eventualmente ad una compensazione.
     *******************************************************************************************************************/
    @Transactional
    public String documentCreatedEvent(Map<String, String> u) {
        String idUser = u.get("idUser");
        String documentId = u.get("documentId");

        Token t;
        Integer tokens;
        try {
            // Verifica che l'utente abbia un numero di token sufficienti per completare l'operazione.
            t = tokenRepository.getTokenByUserOwnerId(Integer.parseInt(idUser));
            tokens = t.getAmount();
            if (tokens == 0)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Crediti non sufficienti.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.\n Crediti non sufficienti?");
        }

        t.setAmount(t.getAmount() - 1);
        tokenRepository.save(t);
        return documentId;
    }
}
