package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.ChatboxSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatboxSessionRepository extends JpaRepository<ChatboxSession, Integer> {
    
    /**
     * Tìm session theo user ID
     */
    List<ChatboxSession> findByUserIdOrderByUpdatedAtDesc(Integer userId);
    
    /**
     * Tìm session mới nhất của user
     */
    Optional<ChatboxSession> findFirstByUserIdOrderByUpdatedAtDesc(Integer userId);
    
    /**
     * Tìm session theo user và trong khoảng thời gian
     */
    @Query("SELECT cs FROM ChatboxSession cs WHERE cs.user.id = :userId AND cs.createdAt >= :startDate")
    List<ChatboxSession> findByUserIdAndCreatedAtAfter(@Param("userId") Integer userId, @Param("startDate") java.time.LocalDateTime startDate);
    
    /**
     * Đếm số session của user
     */
    long countByUserId(Integer userId);
    
    /**
     * Xóa session cũ (giữ lại N session gần nhất)
     */
    @Query(value = "DELETE FROM chatbox_sessions WHERE user_id = :userId AND id NOT IN " +
           "(SELECT id FROM (SELECT id FROM chatbox_sessions WHERE user_id = :userId ORDER BY updated_at DESC LIMIT :keepCount) AS temp)", 
           nativeQuery = true)
    void deleteOldSessions(@Param("userId") Integer userId, @Param("keepCount") int keepCount);
}
