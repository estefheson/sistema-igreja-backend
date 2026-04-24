package com.igreja.system.listmodule.controller;

import com.igreja.system.listmodule.dto.PublicListDetailResponse;
import com.igreja.system.listmodule.dto.PublicListSubmissionRequest;
import com.igreja.system.listmodule.dto.PublicListSubmissionResponse;
import com.igreja.system.listmodule.dto.PublicListSummaryResponse;
import com.igreja.system.listmodule.service.PublicListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/lists")
@RequiredArgsConstructor
public class PublicListController {

    private final PublicListService publicListService;

    @GetMapping
    public List<PublicListSummaryResponse> findAvailableLists() {
        return publicListService.findAvailableLists();
    }

    @GetMapping("/{id}")
    public PublicListDetailResponse findAvailableListById(@PathVariable Long id) {
        return publicListService.findAvailableListById(id);
    }

    @PostMapping("/{id}/submissions")
    public PublicListSubmissionResponse createSubmission(
            @PathVariable Long id,
            @RequestBody PublicListSubmissionRequest request
    ) {
        return publicListService.createSubmission(id, request);
    }
}
