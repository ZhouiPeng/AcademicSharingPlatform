package com.academic.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.analytics.entity.AuthorRelationshipEntity;

public interface AuthorRelationshipRepository extends JpaRepository<AuthorRelationshipEntity, String> {

}
