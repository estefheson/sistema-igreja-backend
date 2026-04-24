package com.igreja.system.ministry.repository;

import com.igreja.system.ministry.entity.MinistryMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MinistryMemberRepository extends JpaRepository<MinistryMember, Long> {
    boolean existsByMinistryIdAndMemberId(Long ministryId, Long memberId);

    boolean existsByMinistryIdAndMemberIdAndLeaderTrue(Long ministryId, Long memberId);

    long countByMinistryIdAndLeaderTrue(Long ministryId);

    Optional<MinistryMember> findByMinistryIdAndMemberId(Long ministryId, Long memberId);

    @Query("""
            select mm
            from MinistryMember mm
            join fetch mm.member
            where mm.ministry.id = :ministryId
            order by mm.member.fullName
            """)
    List<MinistryMember> findAllByMinistryId(@Param("ministryId") Long ministryId);

    @Query("""
            select mm
            from MinistryMember mm
            join fetch mm.member
            where mm.ministry.id = :ministryId
              and mm.leader = true
            order by mm.member.fullName
            """)
    List<MinistryMember> findAllLeadersByMinistryId(@Param("ministryId") Long ministryId);

    @Query("""
            select mm.ministry.id
            from MinistryMember mm
            where mm.member.id = :memberId
            order by mm.ministry.id
            """)
    List<Long> findMinistryIdsByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select mm.ministry.id
            from MinistryMember mm
            where mm.member.id = :memberId
              and mm.leader = true
            order by mm.ministry.id
            """)
    List<Long> findLeaderMinistryIdsByMemberId(@Param("memberId") Long memberId);
}
