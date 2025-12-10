package com.academic.achievement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FolderIdDto {
    @JsonProperty("folderId")
    private String folderId;

    public FolderIdDto() {}

    public FolderIdDto(String folderId) {
        this.folderId = folderId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
