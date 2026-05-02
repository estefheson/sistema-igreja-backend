package com.igreja.system.ministry.controller;

import com.igreja.system.ministry.dto.MinistryActiveUpdateRequest;
import com.igreja.system.ministry.dto.MinistryCreateRequest;
import com.igreja.system.ministry.dto.MinistryMemberLeaderUpdateRequest;
import com.igreja.system.ministry.dto.MinistryMemberResponse;
import com.igreja.system.ministry.dto.MinistryResponse;
import com.igreja.system.ministry.dto.MinistryUpdateRequest;
import com.igreja.system.ministry.service.MinistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ministries")
@RequiredArgsConstructor
public class MinistryController {

    private final MinistryService ministryService;

    @PostMapping
    public MinistryResponse create(@RequestBody MinistryCreateRequest request) {
        return ministryService.create(request);
    }

    @GetMapping
    public List<MinistryResponse> findAll() {
        return ministryService.findAll();
    }

    @GetMapping("/active")
    public List<MinistryResponse> findAllActive() {
        return ministryService.findAllActive();
    }

    @GetMapping("/{id}")
    public MinistryResponse findById(@PathVariable Long id) {
        return ministryService.findById(id);
    }

    @GetMapping("/{id}/members")
    public List<MinistryMemberResponse> findMembersByMinistryId(@PathVariable Long id) {
        return ministryService.findMembersByMinistryId(id);
    }

    @GetMapping("/{id}/leaders")
    public List<MinistryMemberResponse> findLeadersByMinistryId(@PathVariable Long id) {
        return ministryService.findLeadersByMinistryId(id);
    }

    @PutMapping("/{id}")
    public MinistryResponse update(@PathVariable Long id, @RequestBody MinistryUpdateRequest request) {
        return ministryService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public MinistryResponse updateActive(@PathVariable Long id, @RequestBody MinistryActiveUpdateRequest request) {
        return ministryService.updateActive(id, request);
    }

    @PostMapping("/{id}/members/{memberId}")
    public MinistryResponse addMember(@PathVariable Long id, @PathVariable Long memberId) {
        return ministryService.addMember(id, memberId);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public MinistryResponse removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        return ministryService.removeMember(id, memberId);
    }

    @PatchMapping("/{ministryId}/members/{memberId}/leader")
    public MinistryMemberResponse updateLeader(
            @PathVariable Long ministryId,
            @PathVariable Long memberId,
            @RequestBody MinistryMemberLeaderUpdateRequest request
    ) {
        return ministryService.updateLeader(ministryId, memberId, request);
    }
}
