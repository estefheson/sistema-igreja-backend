package com.igreja.system.member.controller;

import com.igreja.system.member.dto.MemberActiveUpdateRequest;
import com.igreja.system.member.dto.MemberCreateRequest;
import com.igreja.system.member.dto.MemberResponse;
import com.igreja.system.member.dto.MemberUpdateRequest;
import com.igreja.system.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @PostMapping
    public MemberResponse create(@RequestBody MemberCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<MemberResponse> findAll(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String cpf
    ) {
        return service.findAll(fullName, cpf);
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @RequestBody MemberUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public MemberResponse updateActive(@PathVariable Long id, @RequestBody MemberActiveUpdateRequest request) {
        return service.updateActive(id, request);
    }
}
