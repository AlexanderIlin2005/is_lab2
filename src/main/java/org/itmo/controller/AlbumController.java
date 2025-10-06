package org.itmo.controller;

import lombok.RequiredArgsConstructor;
import org.itmo.model.Album;
import org.itmo.service.AlbumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;

    @GetMapping
    public List<Album> getAll() {
        return albumService.getAll();
    }
}
