package com.igreja.system.user.controller;

import com.igreja.system.user.dto.UserCreateRequest;
import com.igreja.system.user.dto.UserResponse;
import com.igreja.system.user.dto.UserActiveUpdateRequest;
import com.igreja.system.user.dto.UserPasswordUpdateRequest;
import com.igreja.system.user.dto.UserUpdateRequest;
import com.igreja.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponse create(@RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public UserResponse updateActive(@PathVariable Long id, @RequestBody UserActiveUpdateRequest request) {
        return userService.updateActive(id, request);
    }

    @PatchMapping("/{id}/password")
    public UserResponse updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateRequest request) {
        return userService.updatePassword(id, request);
    }
}
