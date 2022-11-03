package com.dsbd.docauth.documentmanager.adapters;

import com.dsbd.docauth.documentmanager.entities.Document;
import com.dsbd.docauth.documentmanager.services.DocumentManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping(path = "documentsapi/")
public class DocumentManagerAdapter {
    @Autowired
    DocumentManagerService documentManagerService;

    public void saveDurationMetrics(String filePath, Double metric)
            throws IOException {
        // Da nanosecondi a secondi
        metric=metric/1000000000;
        SimpleDateFormat date = new SimpleDateFormat("DD/MM/YYYY HH:mm:ss");
        String timeStamp = date.format(new Date());

        FileWriter fw = new FileWriter(filePath, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(timeStamp + "," + metric);
        bw.newLine();
        bw.close();
    }

    static long startRequest = 0;
    static long endRequest = 0;
    static long startError = 0;
    static long endError = 0;

    static Integer errorCount = 0;

    public void saveErrorRateCountMetrics(String filePath)
            throws IOException {
        errorCount++;
        Double elapsed = 0d;

        if (errorCount == 1) {
            startError = System.currentTimeMillis();
        }
        if (errorCount == 5) {
            endError = System.currentTimeMillis();
            elapsed = Double.parseDouble((endError - startError)+"") / 1000;

            SimpleDateFormat date = new SimpleDateFormat("DD/MM/YYYY HH:mm:ss");
            String timeStamp = date.format(new Date());

            FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(timeStamp + "," + errorCount / elapsed);
            bw.newLine();
            bw.close();

            errorCount = 0;
        }
    }

    static Integer requestCount = 0;

    public void saveRequestRateMetrics(String filePath)
            throws IOException {
        requestCount++;
        Double elapsed = 0d;

        if (requestCount == 1) {
            startRequest = System.currentTimeMillis();
        }
        if (requestCount == 5) {
            endRequest = System.currentTimeMillis();
            elapsed = Double.parseDouble((endRequest - startRequest)+"") / 1000;

            SimpleDateFormat date = new SimpleDateFormat("DD/MM/YYYY HH:mm:ss");
            String timeStamp = date.format(new Date());

            FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(timeStamp + "," + requestCount / elapsed);
            bw.newLine();
            bw.close();

            requestCount = 0;
        }
    }

    @PostMapping(path = "uploadDocument/")
    public @ResponseBody
    String uploadDocument(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName,
                          @RequestParam("password") String password, @RequestParam("documentTitle") String documentTitle,
                          @RequestParam("description") String description, @RequestParam("receiverName") String receiverName) {
        try {
            long startCall = System.nanoTime();
            String retValue = documentManagerService.uploadDocument(file, userName, password, documentTitle, description, receiverName);
            long endCall = System.nanoTime();

            Double elapsedTimeCall = Double.parseDouble((endCall - startCall)+"");
            try {
                saveDurationMetrics("/app/uploadsDurationMetrics.txt", elapsedTimeCall);
                saveRequestRateMetrics("/app/uploadsRequestRateMetrics.txt");
            } catch (IOException ex1) {
                return ex1.getMessage();
            }
            return retValue;
        } catch (ResponseStatusException e) {
            try {
                saveErrorRateCountMetrics("/app/uploadsErrorCountMetrics.txt");
            } catch (IOException ex2) {
                ex2.getMessage();
            }
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PostMapping(path = "verifyDocument/")
    public @ResponseBody
    String verifyDocument(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName,
                          @RequestParam("password") String password) {
        try {
            return documentManagerService.verifyDocument(file, userName, password);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @GetMapping(path = "getAllDocuments/")
    public @ResponseBody
    Iterable<Document> getAllDocuments() {
        return documentManagerService.getAllDocuments();
    }

    @GetMapping(path = "getDocumentByTitle/{documentTitle}")
    public @ResponseBody
    String getDocumentByTitle(@PathVariable String documentTitle) {
        try {
            return documentManagerService.getDocumentByTitle(documentTitle);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PutMapping(path = "updateDocumentTitle/{documentTitle}")
    public @ResponseBody
    String updateDocumentTitle(@PathVariable String documentTitle, @RequestParam("userName") String userName, @RequestParam("password") String password, @RequestParam("newDocumentTitle") String newDocumentTitle) {
        try {
            return documentManagerService.updateDocumentTitle(documentTitle, userName, password, newDocumentTitle);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @PutMapping(path = "updateDocumentDescription/{documentTitle}")
    public @ResponseBody
    String updateDocumentDescription(@PathVariable String documentTitle, @RequestParam("userName") String userName, @RequestParam("password") String password, @RequestParam("newDocumentDescription") String newDocumentDescription) {
        try {
            return documentManagerService.updateDocumentDescription(documentTitle, userName, password, newDocumentDescription);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @DeleteMapping(path = "deleteDocumentByTitle/{documentTitle}")
    public @ResponseBody
    String deleteDocumentByTitle(@PathVariable String documentTitle) {
        try {
            return documentManagerService.deleteDocumentByTitle(documentTitle);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }

    @DeleteMapping(path = "deleteDocumentsByPublisherName/{publisherName}")
    public @ResponseBody
    String deleteDocumentsByPublisherName(@PathVariable String publisherName) {
        try {
            return documentManagerService.deleteDocumentsByPublisherName(publisherName);
        } catch (ResponseStatusException e) {
            return e.getStatus() + "\n" + e.getReason();
        }
    }
}
