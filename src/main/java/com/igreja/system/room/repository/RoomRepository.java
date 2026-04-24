package com.igreja.system.room.repository;

import com.igreja.system.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            select distinct r
            from Room r
            left join fetch r.photos photos
            left join fetch r.reservationRules reservationRules
            order by r.id asc
            """)
    List<Room> findAllWithDetails();

    @Query("""
            select distinct r
            from Room r
            left join fetch r.photos photos
            left join fetch r.reservationRules reservationRules
            where r.id = :id
            """)
    Optional<Room> findByIdWithDetails(@Param("id") Long id);
}
