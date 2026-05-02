package com.igreja.system.reservation.repository;

import com.igreja.system.reservation.entity.Reservation;
import com.igreja.system.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where r.id = :id
            """)
    java.util.Optional<Reservation> findByIdWithRelations(
            @Param("id") Long id
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllWithRelations();

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where r.status = :status
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByStatusWithRelations(
            @Param("status") ReservationStatus status
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where room.id = :roomId
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByRoomIdWithRelations(
            @Param("roomId") Long roomId
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where r.reservationDate >= :startDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByReservationDateGreaterThanEqualWithRelations(
            @Param("startDate") LocalDate startDate
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where r.reservationDate <= :endDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByReservationDateLessThanEqualWithRelations(
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where r.reservationDate between :startDate and :endDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByReservationDateBetweenWithRelations(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where room.id = :roomId
              and r.reservationDate >= :startDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByRoomIdAndReservationDateGreaterThanEqualWithRelations(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where room.id = :roomId
              and r.reservationDate <= :endDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByRoomIdAndReservationDateLessThanEqualWithRelations(
            @Param("roomId") Long roomId,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct r
            from Reservation r
            join fetch r.room room
            left join fetch r.requestedBy requestedBy
            left join fetch r.usingMinistry usingMinistry
            left join fetch r.scheduleDemandMinistries scheduleDemandMinistries
            where room.id = :roomId
              and r.reservationDate between :startDate and :endDate
            order by r.reservationDate asc, r.startTime asc, r.id asc
            """)
    List<Reservation> findAllByRoomIdAndReservationDateBetweenWithRelations(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select count(r) > 0
            from Reservation r
            where r.room.id = :roomId
              and r.reservationDate = :reservationDate
              and r.status <> :cancelledStatus
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsConflictingReservation(
            @Param("roomId") Long roomId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ReservationStatus cancelledStatus
    );
}
