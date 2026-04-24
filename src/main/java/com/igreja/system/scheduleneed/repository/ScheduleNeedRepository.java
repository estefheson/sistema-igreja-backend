package com.igreja.system.scheduleneed.repository;

import com.igreja.system.reservation.entity.ReservationStatus;
import com.igreja.system.scheduleneed.entity.ScheduleNeed;
import com.igreja.system.scheduleneed.entity.ScheduleNeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleNeedRepository extends JpaRepository<ScheduleNeed, Long> {
    boolean existsByReservationIdAndMinistryId(Long reservationId, Long ministryId);

    @Modifying
    @Query("""
            delete
            from ScheduleNeed sn
            where sn.reservation.id = :reservationId
            """)
    void deleteByReservationId(@Param("reservationId") Long reservationId);

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and (:reservationId is null or reservation.id = :reservationId)
              and (:status is null or sn.status = :status)
              and (:date is null or sn.date = :date)
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByFilters(
            @Param("reservationId") Long reservationId,
            @Param("status") ScheduleNeedStatus status,
            @Param("date") LocalDate date,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and ministry.id in :ministryIds
              and (:reservationId is null or reservation.id = :reservationId)
              and (:status is null or sn.status = :status)
              and (:date is null or sn.date = :date)
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByFiltersAndMinistryIds(
            @Param("ministryIds") List<Long> ministryIds,
            @Param("reservationId") Long reservationId,
            @Param("status") ScheduleNeedStatus status,
            @Param("date") LocalDate date,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and ministry.id in :ministryIds
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByMinistryIds(
            @Param("ministryIds") List<Long> ministryIds,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and ministry.id in :ministryIds
              and sn.date >= :startDate
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByMinistryIdsAndDateGreaterThanEqual(
            @Param("ministryIds") List<Long> ministryIds,
            @Param("startDate") LocalDate startDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and ministry.id in :ministryIds
              and sn.date <= :endDate
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByMinistryIdsAndDateLessThanEqual(
            @Param("ministryIds") List<Long> ministryIds,
            @Param("endDate") LocalDate endDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where reservation.status = :reservationStatus
              and ministry.id in :ministryIds
              and sn.date between :startDate and :endDate
            order by sn.date asc, sn.startTime asc, sn.id asc
            """)
    List<ScheduleNeed> findApprovedDetailedByMinistryIdsAndDateBetween(
            @Param("ministryIds") List<Long> ministryIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select distinct sn
            from ScheduleNeed sn
            join fetch sn.reservation reservation
            join fetch reservation.room room
            join fetch sn.ministry ministry
            where sn.id = :id
              and reservation.status = :reservationStatus
            """)
    Optional<ScheduleNeed> findApprovedDetailedById(
            @Param("id") Long id,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );

    @Query("""
            select sn
            from ScheduleNeed sn
            where sn.id = :id
              and sn.reservation.status = :reservationStatus
            """)
    Optional<ScheduleNeed> findApprovedById(
            @Param("id") Long id,
            @Param("reservationStatus") ReservationStatus reservationStatus
    );
}
