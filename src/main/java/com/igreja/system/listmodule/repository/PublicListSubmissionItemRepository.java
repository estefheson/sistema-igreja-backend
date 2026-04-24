package com.igreja.system.listmodule.repository;

import com.igreja.system.listmodule.entity.PublicListSubmissionItem;
import com.igreja.system.listmodule.repository.projection.ListItemParticipantProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicListSubmissionItemRepository extends JpaRepository<PublicListSubmissionItem, Long> {

    @Query("""
            select new com.igreja.system.listmodule.repository.projection.ListItemParticipantProjection(
                listItem.id,
                submission.fullName,
                submission.phone,
                submissionItem.quantity,
                submission.createdAt
            )
            from PublicListSubmissionItem submissionItem
            join submissionItem.submission submission
            join submissionItem.listItem listItem
            where listItem.list.id = :listId
            order by listItem.id asc, submission.createdAt asc, submissionItem.id asc
            """)
    List<ListItemParticipantProjection> findParticipantsByListId(@Param("listId") Long listId);

    @Query("""
            select new com.igreja.system.listmodule.repository.projection.ListItemParticipantProjection(
                listItem.id,
                submission.fullName,
                submission.phone,
                submissionItem.quantity,
                submission.createdAt
            )
            from PublicListSubmissionItem submissionItem
            join submissionItem.submission submission
            join submissionItem.listItem listItem
            where listItem.id = :itemId
            order by submission.createdAt asc, submissionItem.id asc
            """)
    List<ListItemParticipantProjection> findParticipantsByItemId(@Param("itemId") Long itemId);
}
