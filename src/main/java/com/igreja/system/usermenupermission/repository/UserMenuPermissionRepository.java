package com.igreja.system.usermenupermission.repository;

import com.igreja.system.usermenupermission.entity.UserMenuPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserMenuPermissionRepository extends JpaRepository<UserMenuPermission, Long> {
    List<UserMenuPermission> findAllByUserIdOrderByMenuKeyAsc(Long userId);

    @Transactional
    void deleteByUserId(Long userId);
}
