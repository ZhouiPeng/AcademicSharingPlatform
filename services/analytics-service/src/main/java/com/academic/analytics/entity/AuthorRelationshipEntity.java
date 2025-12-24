package com.academic.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "author_relationship")
public class AuthorRelationshipEntity {

    @Id
    private String userId;

    private String authors;

    public AuthorRelationshipEntity() {
    }

    public AuthorRelationshipEntity(String userId, String authors) {
        this.userId = userId;
        this.authors = authors;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }
}
