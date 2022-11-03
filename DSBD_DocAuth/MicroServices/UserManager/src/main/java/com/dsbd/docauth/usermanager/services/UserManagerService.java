package com.dsbd.docauth.usermanager.services;

import com.dsbd.docauth.usermanager.entities.User;
import com.dsbd.docauth.usermanager.entities.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@PropertySource("classpath:application.properties")
public class UserManagerService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    private Environment env;

    /*******************************************************************************************************************
     GENERATEPIN (Metodo di servizio)
     Genera un pin di 4 cifre, casuale e unico per ogni utente.
     Il pin permette di interagire col sistema senza inviare la password.
     Un nuovo pin può essere generato su richiesta dell'utente.
     *******************************************************************************************************************/
    private String generatePin(String userName) {
        Random random = new Random(LocalTime.now().toNanoOfDay());
        String pin = String.format("%04d", random.nextInt(10000));
        while (userRepository.getUserByUserNameAndPin(userName, pin) != null)
            pin = String.format("%04d", random.nextInt(10000));
        return pin;
    }

    /*******************************************************************************************************************
     NEWUSERTOKENGIFT (Metodo di servizio)
     Richiama il metodo addTokenToUser esposto dal servizio TokenManager.
     Permette di caricare i 5 token omaggio alla creazione dell'identità digitale.
     La gestione delle eccezioni è demandata al servizio chiamato.
     *******************************************************************************************************************/
    public String newUserTokenGift(String userName, String password) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("tokenmanager.path");
        url += "/tokensapi/addTokensToUser/5";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> args = new HashMap<>();
        args.put("userName", userName);
        args.put("password", password);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(args, headers);

        return restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
    }

    /*******************************************************************************************************************
     INSERTUSER (POST)
     Registra una nuova identità digitale.
     Dopo aver salvato nome utente e password, viene caricato un bonus di 5 token sul conto dell'utente.
     *******************************************************************************************************************/
    public String insertUser(String userName, String password) {
        String pin = generatePin(userName);

        User u = new User();
        u.setUserName(userName);
        u.setPassword(password);
        u.setPin(pin);

        if (userRepository.getUserByUserName(userName) != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utente è già presente in archivio.");
        try {
            userRepository.save(u);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico di sistema.");
        }

        return "Utente registrato con successo.\n"
                + "Conservare il pin: "
                + pin;
    }

    /*******************************************************************************************************************
     GETALLUSERS (GET)
     Restituisce una lista degli utenti registrati (solo per debug).
     *******************************************************************************************************************/
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    /*******************************************************************************************************************
     UPDATEUSERPIN (POST)
     Rigenera il pin di un utente.
     *******************************************************************************************************************/
    public String updateUserPin(String userName, String password) {
        User u = userRepository.getUserByUserNameAndPassword(userName, password);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        String pin = generatePin(userName);
        u.setPin(pin);
        userRepository.save(u);
        return "Conservare il nuovo pin: " + pin;
    }

    /*******************************************************************************************************************
     UPDATEUSERPASSWORD (PUT)
     Modifica la password dell'utente.
     *******************************************************************************************************************/
    public String updateUserPassword(String userName, String pin, String newPassword) {
        User u = userRepository.getUserByUserNameAndPin(userName, pin);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio.");
        u.setPassword(newPassword);
        userRepository.save(u);
        return "Password aggiornata.";
    }

    /*******************************************************************************************************************
     DELETEUSER (DELETE)
     Elimina l'utente.
     *******************************************************************************************************************/
    public String deleteUser(String userName, String pin) {
        User u = userRepository.getUserByUserNameAndPin(userName, pin);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin errato o utente non più in archivio.");

        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("tokenmanager.path");
        url += "/tokensapi/deleteUserTokens/" + u.getId();

        // Elimina i token associati all'utente
        restTemplate.exchange(url, HttpMethod.DELETE, null, String.class).getBody();

        url = env.getProperty("documentmanager.path");
        url += "/documentsapi/deleteDocumentsByPublisherName/" + u.getUserName();

        // Elimina i documenti associati all'utente
        restTemplate.exchange(url, HttpMethod.DELETE, null, String.class).getBody();

        // Elimina l'utente
        userRepository.delete(u);

        return "Utente cancellato con successo.";
    }

    /*******************************************************************************************************************
     GETUSERID (POST)
     Ottiene l'id dell'utente a partire dal nome utente e dalla password.
     *******************************************************************************************************************/
    @Transactional
    public String getUserId(String userName, String password) {
        User u = userRepository.getUserByUserNameAndPassword(userName, password);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio.");
        return u.getId().toString();
    }

}
