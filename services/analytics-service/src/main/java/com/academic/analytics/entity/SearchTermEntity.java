package com.academic.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_terms")
public class SearchTermEntity {

    @Id
    @Column(name = "term", length = 512)
    private String term;

    @Column(name = "count")
    private Long count;

    @Column(name = "last_seen")
    private Long lastSeen;

    public SearchTermEntity() {
    }

    public SearchTermEntity(String term, Long count, Long lastSeen) {
        this.term = term;
        this.count = count;
        this.lastSeen = lastSeen;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
