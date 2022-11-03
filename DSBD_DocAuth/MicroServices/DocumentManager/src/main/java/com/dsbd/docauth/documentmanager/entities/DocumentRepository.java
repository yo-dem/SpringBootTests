package com.dsbd.docauth.documentmanager.entities;

import org.springframework.data.repository.CrudRepository;

public interface DocumentRepository extends CrudRepository<Document, Integer> {
    Document getDocumentByDocumentTitle(String documentTitle);
    void deleteDocumentByDocumentTitle(String documentTitle);
    void deleteDocumentsByPublisherName(String publisherName);
    Document getDocumentByHashCode(String hashCode);
    Iterable<Document> getDocumentsByPublisherName(String publisherName);
    Iterable<Document> getDocumentsByIsVerified(Boolean isVerified);
}
