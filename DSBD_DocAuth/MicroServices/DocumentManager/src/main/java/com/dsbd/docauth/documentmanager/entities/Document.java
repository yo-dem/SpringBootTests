package com.dsbd.docauth.documentmanager.entities;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Integer id;
    @NotBlank
    @Column(unique = true)
    String documentTitle;
    @NotBlank
    String description;
    @NotBlank
    @Column(unique = true)
    String hashCode;
    @NotBlank
    String timeStamp;
    @NotBlank
    String publisherName;
    String receiverName;
    @NotNull
    Boolean isVerified;
    @NotNull
    Boolean status;

    public Document() {
    }

    public Document(Integer id, String documentTitle, String description, String hashCode, String timeStamp, String publisherName, String receiverName, Boolean isVerified, Boolean status) {
        this.id = id;
        this.documentTitle = documentTitle;
        this.description = description;
        this.hashCode = hashCode;
        this.timeStamp = timeStamp;
        this.publisherName = publisherName;
        this.receiverName = receiverName;
        this.isVerified = isVerified;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Document{" + "id=" + id + ", documentTitle='" + documentTitle + '\'' + ", description='" + description + '\'' + ", hashCode='" + hashCode + '\'' + ", timeStamp='" + timeStamp + '\'' + ", publisherName='" + publisherName + '\'' + ", receiverName='" + receiverName + '\'' + ", isVerified=" + isVerified + ", status==" + status + '}';
    }
}
