package com.academic.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminMongoInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AdminMongoInitializer.class);

    private final MongoTemplate mongoTemplate;
    private final String messagesCollection = "messages";

    public AdminMongoInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!mongoTemplate.collectionExists(messagesCollection)) {
                mongoTemplate.createCollection(messagesCollection);
                logger.info("Created MongoDB collection '{}'", messagesCollection);
            } else {
                logger.info("MongoDB collection '{}' already exists", messagesCollection);
            }
        } catch (Exception ex) {
            logger.warn("Could not initialize MongoDB collections at startup: {}", ex.getMessage());
            logger.debug("Full exception while initializing Mongo collections", ex);
        }
    }
}
