package com.igreja.system.scheduleassignment.repository;

import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.scheduleassignment.entity.ScheduleAssignment;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment, Long> {
    boolean existsByScheduleNeedIdAndMemberId(Long scheduleNeedId, Long memberId);

    long countByScheduleNeedId(Long scheduleNeedId);

    List<ScheduleAssignment> findAllByScheduleNeedId(Long scheduleNeedId);

    Optional<ScheduleAssignment> findByScheduleNeedIdAndMemberId(Long scheduleNeedId, Long memberId);

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where scheduleNeed.id = :scheduleNeedId
            order by sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findAllDetailedByScheduleNeedId(
            @Param("scheduleNeedId") Long scheduleNeedId
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where scheduleNeed.id in :scheduleNeedIds
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findAllDetailedByScheduleNeedIdIn(
            @Param("scheduleNeedIds") List<Long> scheduleNeedIds
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where scheduleNeed.id = :scheduleNeedId
              and member.id = :memberId
            """)
    Optional<ScheduleAssignment> findDetailedByScheduleNeedIdAndMemberId(
            @Param("scheduleNeedId") Long scheduleNeedId,
            @Param("memberId") Long memberId
    );

    @Modifying
    @Query("""
            delete
            from ScheduleAssignment sa
            where sa.scheduleNeed.reservation.id = :reservationId
            """)
    void deleteByReservationId(@Param("reservationId") Long reservationId);

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where member.id = :memberId
              and reservation.status = :reservationStatus
              and (:status is null or scheduleNeed.status = :status)
              and (:date is null or scheduleNeed.date = :date)
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findDetailedByMemberIdAndFilters(
            @Param("memberId") Long memberId,
            @Param("status") ScheduleNeedStatus status,
            @Param("date") LocalDate date,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where member.id = :memberId
              and reservation.status = :reservationStatus
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findDetailedByMemberId(
            @Param("memberId") Long memberId,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where member.id = :memberId
              and reservation.status = :reservationStatus
              and scheduleNeed.date >= :startDate
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findDetailedByMemberIdAndDateGreaterThanEqual(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where member.id = :memberId
              and reservation.status = :reservationStatus
              and scheduleNeed.date <= :endDate
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findDetailedByMemberIdAndDateLessThanEqual(
            @Param("memberId") Long memberId,
            @Param("endDate") LocalDate endDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sa
            from ScheduleAssignment sa
            join fetch sa.scheduleNeed scheduleNeed
            join fetch scheduleNeed.ministry ministry
            join fetch scheduleNeed.reservation reservation
            join fetch reservation.room room
            join fetch sa.member member
            left join fetch sa.assignedByUser assignedByUser
            left join fetch assignedByUser.member assignedByMember
            where member.id = :memberId
              and reservation.status = :reservationStatus
              and scheduleNeed.date between :startDate and :endDate
            order by scheduleNeed.date asc, scheduleNeed.startTime asc, sa.assignedAt asc, sa.id asc
            """)
    List<ScheduleAssignment> findDetailedByMemberIdAndDateBetween(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );
}
