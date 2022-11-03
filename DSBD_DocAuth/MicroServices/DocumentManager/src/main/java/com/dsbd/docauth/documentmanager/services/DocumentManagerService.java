package com.dsbd.docauth.documentmanager.services;

import com.dsbd.docauth.documentmanager.entities.Document;
import com.dsbd.docauth.documentmanager.entities.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.transaction.Transactional;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DocumentManagerService {
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    Environment env;

    /*******************************************************************************************************************
     CRONJOB (Metodo di servizio)
     Questo metodo si attiva periodicamente. Raccoglie dal database tutti i documenti non ancora verificati
     (ovvero quelli che hanno il campo isVerified=false) e per ciascuno di essi verifica che il timestamp non sia
     più lontano rispetto alla data attuale di un valore che per debug è impostato a 3 minuti.
     In caso che il documento sia "scaduto" prima che il legittimo destinatario lo verificasse, questo viene
     eliminato dagli archivi, e contestualmente il token speso dal publisher per l'upload viene restituito.
     *******************************************************************************************************************/
    @Scheduled(fixedRate = 1000 * 10)
    @Transactional
    public void chronJob() throws ParseException {
        Iterable<Document> docs = documentRepository.getDocumentsByIsVerified(false);
        if (docs != null) {
            for (Document d : docs) {
                String s_t0 = d.getTimeStamp();

                SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss");
                String s_t1 = date.format(new Date());

                Date d_t0 = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(s_t0);
                Date d_t1 = new SimpleDateFormat("dd/MM/yyyy-hh:mm:ss").parse(s_t1);

                long delta_s = (d_t1.getTime() - d_t0.getTime()) / 1000;

                if (delta_s >= 60 * 3) {
                    documentRepository.delete(d);

                    //Incrementa token mittente
                    RestTemplate restTemplate = new RestTemplate();

                    String url = env.getProperty("tokenmanager.path");
                    url += "/tokensapi/increaseUserTokens/";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    // Imposta i parametri dell'utente in una hashmap
                    Map<String, String> u = new HashMap<>();
                    u.put("userName", d.getPublisherName());

                    HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

                    try {
                        restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile accreditare i token.");
                    }
                }
            }
        }
    }

    /*******************************************************************************************************************
     TOHEXSTRING (Metodo di servizio)
     Converte un array di byte in una stringa alfanumerica.
     *******************************************************************************************************************/
    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v / 16];
            hexChars[j * 2 + 1] = hexArray[v % 16];
        }
        return new String(hexChars);
    }

    /*******************************************************************************************************************
     CALCULATEHASH (Metodo di servizio)
     Riceve un descrittore del documento caricato e ne calcola l'hash secondo l'algoritmo MD5.
     *******************************************************************************************************************/
    String calculateHash(MultipartFile file) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(file.getBytes());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nel salvataggio del documento.");
        }
        return toHexString(hash);
    }

    /*******************************************************************************************************************
     TOUSERMANAGER_GETUSERID (Metodo di servizio)
     Effettua una richiesta al servizio UserManager per ottenere l'id di un utente a partire dalle sue
     credenziali (nome utente e password)
     *******************************************************************************************************************/
    private String toUserManager_GetUserId(String userName, String password) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> u = new HashMap<>();
        u.put("userName", userName);
        u.put("password", password);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String idUser;
        try {
            idUser = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
            if (idUser == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\n Utente non in archivio o password errata?");
        }
        return idUser;
    }

    /*******************************************************************************************************************
     DOCUMENTCREATEDEVENT (Metodo di servizio)
     Genera una chiamata al metodo documentCreatedEvent esposto dal servizio TokenManager, in conformità col pattern
     SAGA, inviando l'id del documento appena creato dal DocumentManager e l'id dell'utente sul quale è necessario
     effettuare verifiche che possono eventualmente portare alla compensazione tramite eliminazione
     del documento salvato.
     *******************************************************************************************************************/
    private String documentCreatedEvent(String idUser, Integer documentId) {
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("tokenmanager.path");
        url += "/tokensapi/documentCreatedEvent/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> u = new HashMap<>();
        u.put("idUser", idUser);
        u.put("documentId", documentId.toString());

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        try {
            return restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossibile procedere.");
        }
    }

    /*******************************************************************************************************************
     UPLOADDOCUMENT (POST)
     Estrae i metadati del documento caricato e li inserisce nel database.
     *******************************************************************************************************************/
    public String uploadDocument(MultipartFile file, String userName,
                                 String password, String documentTitle,
                                 String description, String receiverName) {
        // Calcola l'hash del documento caricato
        String hashCode = calculateHash(file);

        // Genera un timestamp da associare al documento
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss");
        String timeStamp = date.format(new Date());

        // Verifica la presenza dell'utente in archivio
        String idUser = toUserManager_GetUserId(userName, password);

        try {
            Integer.parseInt(idUser);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        Document doc = new Document();
        doc.setDocumentTitle(documentTitle);
        doc.setDescription(description);
        doc.setHashCode(hashCode);
        doc.setTimeStamp(timeStamp);
        doc.setPublisherName(userName);
        doc.setReceiverName(receiverName);
        doc.setVerified(false);
        doc.setStatus(false);
        try {
            documentRepository.save(doc);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Il documento è già in archivio.");
        }

        Integer documentId = documentRepository.getDocumentByDocumentTitle(documentTitle).getId();

        String result = documentCreatedEvent(idUser, documentId);

        if (result.equals(documentId.toString())) {
            doc.setStatus(true);
            documentRepository.save(doc);
        } else {
            documentRepository.deleteById(documentId);
            return "Il documento non sarà conservato in archivio. Token insufficienti\n" + doc;
        }
        return doc.toString();
    }

    /*******************************************************************************************************************
     VERIFYDOCUMENT (POST)
     Permette a un utente di verificare che la propria copia di un dato documento sia o no originale, in base
     all'hash calcolato dopo l'upload e alla verifica che questo sia stato depositato in archivio da un publisher.
     *******************************************************************************************************************/
    @Transactional
    public String verifyDocument(MultipartFile file, String userName, String password) {
        String hashCode = calculateHash(file);

        // Verifica la presenza dell'utente in archivio
        // richiamando il metodo getUserId attivo sul servizio UserManager
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Imposta i parametri dell'utente in una hashmap
        Map<String, String> u = new HashMap<>();
        u.put("userName", userName);
        u.put("password", password);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String userId;
        try {
            userId = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\n Utente non in archivio o password errata?");
        }

        try {
            Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        Document doc = documentRepository.getDocumentByHashCode(hashCode);
        if (doc == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento non presente in archivio.");

        // Il documento è stato verificato
        if (userName.equals(doc.getReceiverName())) {
            doc.setVerified(true);
            documentRepository.save(doc);
        }
        return "Documento verificato!\n" + doc;
    }

    /*******************************************************************************************************************
     GETALLDOCUMENTS (GET)
     Restituisce i documenti archiviati.
     *******************************************************************************************************************/
    public Iterable<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /*******************************************************************************************************************
     GETDOCUMENTBYTITLE (GET)
     Restituisce un documento archiviato a partire dal titolo (univoco).
     *******************************************************************************************************************/
    public String getDocumentByTitle(String documentTitle) {
        Document doc;
        doc = documentRepository.getDocumentByDocumentTitle(documentTitle);
        if (doc == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento non in archivio.");
        return doc.toString();
    }

    /*******************************************************************************************************************
     UPDATEDOCUMENTTITLE (PUT)
     Modifica il titolo del documento.
     *******************************************************************************************************************/
    public String updateDocumentTitle(String documentTitle, String userName, String password, String newDocumentTitle) {
        Document d;
        d = documentRepository.getDocumentByDocumentTitle(documentTitle);
        if (d == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento non in archivio.");
        if(!d.getPublisherName().equals(userName))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utente non abilitato alle modifiche.");

        // Verifica la presenza dell'utente in archivio
        // richiamando il metodo getUserId attivo sul servizio UserManager
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Imposta i parametri dell'utente in una hashmap
        Map<String, String> u = new HashMap<>();
        u.put("userName", userName);
        u.put("password", password);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String userId;
        try {
            userId = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\n Utente non in archivio o password errata?");
        }

        try {
            Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        d.setDocumentTitle(newDocumentTitle);
        documentRepository.save(d);
        return "Documento aggiornato.";
    }

    /*******************************************************************************************************************
     UPDATEDOCUMENTDESCRIPTION (PUT)
     Modifica la descrizione del documento.
     *******************************************************************************************************************/
    public String updateDocumentDescription(String documentTitle, String userName, String password, String newDocumentDescription) {
        Document d;
        d = documentRepository.getDocumentByDocumentTitle(documentTitle);
        if (d == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento non in archivio.");
        if(!d.getPublisherName().equals(userName))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utente non abilitato alle modifiche.");

        // Verifica la presenza dell'utente in archivio
        // richiamando il metodo getUserId attivo sul servizio UserManager
        RestTemplate restTemplate = new RestTemplate();

        String url = env.getProperty("usermanager.path");
        url += "/usersapi/getUserId/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Imposta i parametri dell'utente in una hashmap
        Map<String, String> u = new HashMap<>();
        u.put("userName", userName);
        u.put("password", password);

        HttpEntity<?> httpEntity = new HttpEntity<Object>(u, headers);

        String userId;
        try {
            userId = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class).getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\n Utente non in archivio o password errata?");
        }

        try {
            Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non in archivio o password errata.");
        }

        d.setDescription(newDocumentDescription);
        documentRepository.save(d);
        return "Documento aggiornato.";
    }

    /*******************************************************************************************************************
     DELETEDOCUMENTBYTITLE (DELETE)
     Elimina un documento archiviato a partire dal titolo (univoco).
     *******************************************************************************************************************/
    @Transactional
    public String deleteDocumentByTitle(String documentTitle) {
        Document doc;
        doc = documentRepository.getDocumentByDocumentTitle(documentTitle);
        if (doc == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento non in archivio, controllare i dati inseriti.");
        try {
            documentRepository.deleteDocumentByDocumentTitle(documentTitle);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\nDocumento non in archivio? Controllare i dati inseriti.");
        }
        return "Documento eliminato con successo.";
    }

    /*******************************************************************************************************************
     DELETEDOCUMENTBYPUBLISHERNAME (DELETE)
     Elimina un documento archiviato a partire dal nome del publisher (univoco).
     *******************************************************************************************************************/
    @Transactional
    public String deleteDocumentsByPublisherName(String publisherName) {
        Iterable<Document> docs;
        docs = documentRepository.getDocumentsByPublisherName(publisherName);
        if (docs.spliterator().getExactSizeIfKnown() == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documenti non in archivio. Controllare i dati inseriti.");
        try {
            documentRepository.deleteDocumentsByPublisherName(publisherName);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore generico del sistema.\nDocumenti non in archivio? Controllare i dati inseriti.");
        }
        return "Documenti eliminati con successo.";
    }

}
