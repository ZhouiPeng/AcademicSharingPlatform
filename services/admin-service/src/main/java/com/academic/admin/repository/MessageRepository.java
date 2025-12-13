package com.academic.admin.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.academic.admin.entity.Message;

public interface MessageRepository extends MongoRepository<Message, String> {
}
