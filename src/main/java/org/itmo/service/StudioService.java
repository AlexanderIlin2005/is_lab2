package org.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.itmo.model.Studio;
import org.itmo.repository.StudioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudioService {
    private final StudioRepository studioRepository;

    public List<Studio> getAll() {
        return studioRepository.findAll();
    }

    public Studio getById(Long id) {
        return studioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Studio not found: " + id));
    }

    public Studio create(Studio studio) {
        return studioRepository.save(studio);
    }

    public Studio update(Long id, Studio studio) {
        Studio existing = getById(id);
        existing.setName(studio.getName());
        return studioRepository.save(existing);
    }

    public void delete(Long id) {
        Studio studio = getById(id);
        studioRepository.delete(studio);
    }
}