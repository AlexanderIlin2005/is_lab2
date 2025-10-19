package org.itmo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.itmo.service.AlbumService;
import org.itmo.model.Album;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {
    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping
    public List<Album> getAllAlbums() {
        return albumService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Album> getAlbumById(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Album> createAlbum(@Valid @RequestBody Album album) {
        Album createdAlbum = albumService.create(album);
        return new ResponseEntity<>(createdAlbum, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Album> updateAlbum(@PathVariable Long id, @Valid @RequestBody Album patch) {
        Album updatedAlbum = albumService.update(id, patch);
        return ResponseEntity.ok(updatedAlbum);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        albumService.delete(id);
        return ResponseEntity.noContent().build();
    }
}