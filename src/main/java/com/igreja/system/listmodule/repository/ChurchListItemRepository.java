package com.igreja.system.listmodule.repository;

import com.igreja.system.listmodule.entity.ChurchListItem;
import com.igreja.system.listmodule.repository.projection.ListItemReservedQuantityProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChurchListItemRepository extends JpaRepository<ChurchListItem, Long> {

    @Query("""
            select item
            from ChurchListItem item
            join fetch item.list list
            where item.id = :id
            """)
    java.util.Optional<ChurchListItem> findByIdWithList(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from ChurchListItem item
            where item.list.id = :listId
              and item.id in :itemIds
            order by item.id asc
            """)
    List<ChurchListItem> findAllByListIdAndIdInForUpdate(
            @Param("listId") Long listId,
            @Param("itemIds") List<Long> itemIds
    );

    @Query("""
            select new com.igreja.system.listmodule.repository.projection.ListItemReservedQuantityProjection(
                item.id,
                coalesce(sum(submissionItem.quantity), 0)
            )
            from ChurchListItem item
            left join item.submissionItems submissionItem
            where item.list.id = :listId
            group by item.id
            """)
    List<ListItemReservedQuantityProjection> findReservedQuantitiesByListId(@Param("listId") Long listId);

    @Query("""
            select new com.igreja.system.listmodule.repository.projection.ListItemReservedQuantityProjection(
                item.id,
                coalesce(sum(submissionItem.quantity), 0)
            )
            from ChurchListItem item
            left join item.submissionItems submissionItem
            where item.list.id = :listId
              and item.id in :itemIds
            group by item.id
            """)
    List<ListItemReservedQuantityProjection> findReservedQuantitiesByListIdAndItemIds(
            @Param("listId") Long listId,
            @Param("itemIds") List<Long> itemIds
    );
}
