package org.itmo.service;

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
}
