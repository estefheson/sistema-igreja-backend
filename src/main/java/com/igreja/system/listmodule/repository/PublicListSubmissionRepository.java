package com.igreja.system.listmodule.repository;

import com.igreja.system.listmodule.entity.PublicListSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicListSubmissionRepository extends JpaRepository<PublicListSubmission, Long> {
    long countByListId(Long listId);
}
