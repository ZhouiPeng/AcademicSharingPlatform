package com.academic.achievement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.achievement.entity.FolderEntity;

public interface FolderRepository extends JpaRepository<FolderEntity, String> {

}
