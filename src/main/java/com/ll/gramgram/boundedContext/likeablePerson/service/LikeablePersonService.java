package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Value("${likeable.max}")
    private int LIKEABLE_MAX_VALUE;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if (!member.hasConnectedInstaMember()) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        InstaMember fromInstaMember = member.getInstaMember();

        if (fromInstaMember.getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();
        LikeablePerson findLikeablePerson = getLikeablePersonByInstaMember(fromInstaMember, toInstaMember)
            .orElse(null);

        if (findLikeablePerson != null) {
            if (attractiveTypeCode == findLikeablePerson.getAttractiveTypeCode()) {
                return RsData.of("F-3", "같은 타입의 호감이 이미 존재합니다.");
            }

            findLikeablePerson.updateAttractiveTypeCode(attractiveTypeCode);
            return RsData.of("S-2", "기존의 호감 타입을 입력하신 호감타입으로 수정되었습니다.", findLikeablePerson);
        }

        if (fromInstaMember.getFromLikeablePeople().size() >= LIKEABLE_MAX_VALUE) {
            return RsData.of("F-4","호감 상대는 %s명 까지만 등록 가능합니다.".formatted(LIKEABLE_MAX_VALUE));
        }

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        // 너가 좋아하는 호감표시 생겼어.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        // 너를 좋아하는 호감표시 생겼어.
        toInstaMember.addToLikeablePerson(likeablePerson);

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    private Optional<LikeablePerson> getLikeablePersonByInstaMember(InstaMember fromMember, InstaMember toInstaMember) {
        return likeablePersonRepository
                .findByFromInstaMemberIdAndToInstaMemberId(fromMember.getId(), toInstaMember.getId());
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }

    @Transactional
    public RsData delete(LikeablePerson likeablePerson) {
        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    public RsData canActorDelete(Member actor, LikeablePerson likeablePerson) {
        if (likeablePerson == null) {
            return RsData.of("F-1", "이미 삭제되었습니다.");
        }

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId) {
            return RsData.of("F-2", "권한이 없습니다.");
        }

        return RsData.of("S-1", "삭제가능합니다.");
    }
}
