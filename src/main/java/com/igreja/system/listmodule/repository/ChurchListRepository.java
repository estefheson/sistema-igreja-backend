package com.igreja.system.listmodule.repository;

import com.igreja.system.listmodule.entity.ChurchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChurchListRepository extends JpaRepository<ChurchList, Long> {

    @Query("""
            select distinct cl
            from ChurchList cl
            left join fetch cl.items item
            order by cl.startsAt desc, cl.id desc
            """)
    List<ChurchList> findAllWithItems();

    @Query("""
            select distinct cl
            from ChurchList cl
            left join fetch cl.items item
            where cl.id = :id
            """)
    Optional<ChurchList> findByIdWithItems(@Param("id") Long id);

    @Query("""
            select cl
            from ChurchList cl
            where cl.active = true
              and cl.startsAt <= :referenceDateTime
              and cl.endsAt >= :referenceDateTime
            order by cl.startsAt asc, cl.id asc
            """)
    List<ChurchList> findAllPubliclyAvailable(@Param("referenceDateTime") LocalDateTime referenceDateTime);

    @Query("""
            select distinct cl
            from ChurchList cl
            left join fetch cl.items item
            where cl.id = :id
              and cl.active = true
              and cl.startsAt <= :referenceDateTime
              and cl.endsAt >= :referenceDateTime
            """)
    Optional<ChurchList> findPublicDetailedById(
            @Param("id") Long id,
            @Param("referenceDateTime") LocalDateTime referenceDateTime
    );
}
