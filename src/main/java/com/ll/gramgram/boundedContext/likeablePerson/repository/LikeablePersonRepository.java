package com.ll.gramgram.boundedContext.likeablePerson.repository;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeablePersonRepository extends JpaRepository<LikeablePerson, Long> {
    List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId);
//    Optional<LikeablePerson> findByFromInstaMemberUsernameAndToInstaMemberUsername(String fromUsername, String toUsername);

    Optional<LikeablePerson> findByFromInstaMemberIdAndToInstaMemberId(Long fromId, Long toId);

    Long countByFromInstaMemberId(Long fromInstaMemberId);
}
