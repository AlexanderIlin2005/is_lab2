package org.itmo.service;

import lombok.RequiredArgsConstructor;
import org.itmo.model.Album;
import org.itmo.repository.AlbumRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumService {
    private final AlbumRepository albumRepository;

    public List<Album> getAll() {
        return albumRepository.findAll();
    }
}
