package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findBySender_IdOrReceiver_Id(Integer senderId, Integer receiverId);
    List<Message> findByReceiver_IdAndIsReadFalse(Integer receiverId);
}
