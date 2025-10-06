package org.itmo.controller;

import lombok.RequiredArgsConstructor;
import org.itmo.model.Studio;
import org.itmo.service.StudioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/studios")
@RequiredArgsConstructor
public class StudioController {
    private final StudioService studioService;

    @GetMapping
    public List<Studio> getAll() {
        return studioService.getAll();
    }
}
