package com.academic.admin.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.CreatedDate;
import java.time.Instant;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String userId;
    private String title;
    private String content;
    @CreatedDate
    private Instant createdAt;
}
