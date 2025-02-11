package io.recruit_assist.recrugen.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;

    @Lob
    private byte[] fileData; // Store resume as byte array

    @Lob
    private String extractedData; // Store extracted categories JSON

    public Resume() {
    }

    public Resume(Long id, String fileName, String fileType, byte[] fileData, String extractedData) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileData = fileData;
        this.extractedData = extractedData;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(String extractedData) {
        this.extractedData = extractedData;
    }
}
