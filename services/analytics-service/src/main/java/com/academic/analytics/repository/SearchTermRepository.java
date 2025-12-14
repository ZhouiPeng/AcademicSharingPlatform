package com.academic.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.analytics.entity.SearchTermEntity;

public interface SearchTermRepository extends JpaRepository<SearchTermEntity, String> {
}
