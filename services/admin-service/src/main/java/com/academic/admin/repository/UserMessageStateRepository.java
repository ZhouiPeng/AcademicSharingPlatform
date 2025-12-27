package com.academic.admin.repository;

import com.academic.admin.entity.UserMessageState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserMessageStateRepository extends JpaRepository<UserMessageState, String> {
	List<UserMessageState> findByUserIdOrderByUpdatedAtDesc(String userId);
	List<UserMessageState> findByMessageId(String messageId);
	UserMessageState findByUserIdAndMessageId(String userId, String messageId);
}
