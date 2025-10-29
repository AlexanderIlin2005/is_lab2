package org.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.itmo.dto.*;
import org.itmo.mapper.MusicBandMapper;
import org.itmo.model.*;
import org.itmo.repository.MusicBandRepository;
import org.itmo.repository.AlbumRepository;
import org.itmo.repository.CoordinatesRepository;
import org.itmo.repository.StudioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.SerializationFeature; // <-- NEW
import com.fasterxml.jackson.dataformat.xml.XmlMapper;     // <-- NEW
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // <-- NEW (для ZonedDateTime)
import jakarta.validation.ConstraintViolation; // <-- NEW
import jakarta.validation.Validator;           // <-- NEW
import org.itmo.dto.MusicBandsXmlDto;         // <-- NEW
import org.springframework.web.multipart.MultipartFile; // <-- NEW
import org.springframework.beans.factory.annotation.Autowired; // <-- NEW
import java.io.InputStream;                     // <-- NEW
import java.util.ArrayList;                     // <-- NEW
import java.util.Set;                           // <-- NEW


@Service
@Transactional
public class MusicBandService {
    private final MusicBandRepository musicBandRepository;
    private final AlbumRepository albumRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final StudioRepository studioRepository;
    private final MusicBandMapper musicBandMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private Validator validator;

    public MusicBandService(MusicBandRepository musicBandRepository,
                            AlbumRepository albumRepository,
                            CoordinatesRepository coordinatesRepository,
                            StudioRepository studioRepository,
                            MusicBandMapper musicBandMapper,
                            SimpMessagingTemplate messagingTemplate
    ) {
        this.musicBandRepository = musicBandRepository;
        this.albumRepository = albumRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.studioRepository = studioRepository;
        //this.eventsPublisher = eventsPublisher;
        this.musicBandMapper = musicBandMapper;
        this.messagingTemplate = messagingTemplate;

    }


    private void notifyClients(String type) {

        messagingTemplate.convertAndSend("/topic/bands/updates", type);
    }

    public Page<MusicBandResponseDto> list(String nameEquals, Pageable pageable) {
        if (nameEquals != null && !nameEquals.isEmpty()) {
            return musicBandRepository.findByName(nameEquals, pageable)
                    .map(musicBandMapper::toResponseDto);
        }
        return musicBandRepository.findAll(pageable).map(musicBandMapper::toResponseDto);
    }

    public MusicBandResponseDto get(@NotNull Long id) {
        MusicBand musicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MusicBand not found: " + id));
        return musicBandMapper.toResponseDto(musicBand);
    }

    public MusicBandResponseDto create(@Valid MusicBandCreateDto dto) {
        MusicBand musicBand = musicBandMapper.toEntity(dto);


        Long coordsId = dto.getCoordinates() != null ? dto.getCoordinates().getId() : null;
        if (coordsId != null) {
            Coordinates persistentCoords = coordinatesRepository.findById(coordsId)
                    .orElseThrow(() -> new EntityNotFoundException("Coordinates not found: " + coordsId));
            musicBand.setCoordinates(persistentCoords);
        } else if (dto.getCoordinates() != null) {
            Coordinates newCoords = musicBandMapper.toEntity(dto.getCoordinates());
            newCoords = coordinatesRepository.save(newCoords);
            musicBand.setCoordinates(newCoords);
        } else {
            throw new IllegalArgumentException("coordinates are required");
        }


        Long albumId = dto.getBestAlbum() != null ? dto.getBestAlbum().getId() : null;
        if (albumId != null) {
            Album persistentAlbum = albumRepository.findById(albumId)
                    .orElseThrow(() -> new EntityNotFoundException("Album not found: " + albumId));
            musicBand.setBestAlbum(persistentAlbum);
        } else if (dto.getBestAlbum() != null) {
            Album newAlbum = musicBandMapper.toEntity(dto.getBestAlbum());
            newAlbum = albumRepository.save(newAlbum);
            musicBand.setBestAlbum(newAlbum);
        }


        Long studioId = dto.getStudio() != null ? dto.getStudio().getId() : null;
        if (studioId != null) {
            Studio persistentStudio = studioRepository.findById(studioId)
                    .orElseThrow(() -> new EntityNotFoundException("Studio not found: " + studioId));
            musicBand.setStudio(persistentStudio);
        }


        musicBand = musicBandRepository.save(musicBand);
        notifyClients("BAND_UPDATED"); // <-- Добавить вызов
        return musicBandMapper.toResponseDto(musicBand);
    }

    public MusicBandResponseDto update(@NotNull Long id, @Valid MusicBandCreateDto patch) {
        MusicBand existing = musicBandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MusicBand not found: " + id));


        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getGenre() != null) existing.setGenre(patch.getGenre());
        if (patch.getNumberOfParticipants() > 0) existing.setNumberOfParticipants(patch.getNumberOfParticipants());
        if (patch.getSingleCount() != null) existing.setSingleCount(patch.getSingleCount());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getAlbumCount() > 0) existing.setAlbumCount(patch.getAlbumCount());
        if (patch.getEstablishmentDate() != null) existing.setEstablishmentDate(patch.getEstablishmentDate());


        if (patch.getCoordinates() != null) {
            Long coordsId = patch.getCoordinates().getId();
            if (coordsId != null) {
                Coordinates persistentCoords = coordinatesRepository.findById(coordsId)
                        .orElseThrow(() -> new EntityNotFoundException("Coordinates not found: " + coordsId));
                existing.setCoordinates(persistentCoords);
            } else {
                Coordinates newCoords = musicBandMapper.toEntity(patch.getCoordinates());
                newCoords = coordinatesRepository.save(newCoords);
                existing.setCoordinates(newCoords);
            }
        }


        if (patch.getBestAlbum() != null) {
            Long albumId = patch.getBestAlbum().getId();
            if (albumId != null) {
                Album persistentAlbum = albumRepository.findById(albumId)
                        .orElseThrow(() -> new EntityNotFoundException("Album not found: " + albumId));
                existing.setBestAlbum(persistentAlbum);
            } else {
                Album newAlbum = musicBandMapper.toEntity(patch.getBestAlbum());
                newAlbum = albumRepository.save(newAlbum);
                existing.setBestAlbum(newAlbum);
            }
        }


        if (patch.getStudio() != null) {
            Long studioId = patch.getStudio().getId();

            if (studioId != null) {

                Studio persistentStudio = studioRepository.findById(studioId)
                        .orElseThrow(() -> new EntityNotFoundException("Studio not found: " + studioId));
                existing.setStudio(persistentStudio);
            } else {

                existing.setStudio(null);
            }
        }


        existing = musicBandRepository.save(existing);
        notifyClients("BAND_UPDATED");
        return musicBandMapper.toResponseDto(existing);
    }

    public void delete(@NotNull Long id) {
        MusicBand musicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MusicBand not found: " + id));
        musicBandRepository.delete(musicBand);

        notifyClients("BAND_UPDATED");
    }

    // Специальные операции
    public boolean deleteOneByStudioName(String studioName) {
        Optional<MusicBand> musicBand = musicBandRepository.findFirstByStudioName(studioName);
        if (musicBand.isPresent()) {
            musicBandRepository.delete(musicBand.get());

            return true;
        }
        return false;
    }

    public Double getAverageAlbumCount() {
        return musicBandRepository.findAverageAlbumCount();
    }

    public long countByStudioNameGreaterThan(String studioName) {
        return musicBandRepository.countByStudioNameGreaterThan(studioName);
    }

    public List<MusicBandResponseDto> findByGenre(MusicGenre genre) {
        List<MusicBand> musicBands = musicBandRepository.findByGenre(genre);
        return musicBands.stream()
                .map(musicBandMapper::toResponseDto)
                .toList();
    }

    public long removeParticipant(Long id) {
        MusicBand musicBand = musicBandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MusicBand not found: " + id));

        if (musicBand.getNumberOfParticipants() > 1) {
            musicBand.setNumberOfParticipants(musicBand.getNumberOfParticipants() - 1);
            musicBandRepository.save(musicBand);

            notifyClients("BAND_UPDATED");
            return musicBand.getNumberOfParticipants();
        }
        return musicBand.getNumberOfParticipants();
    }

    // --- NEW IMPORT METHOD ---
    @Transactional
    public List<MusicBandResponseDto> importFromXml(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Import file cannot be empty.");
        }

        // Конфигурируем XmlMapper
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<MusicBandResponseDto> createdBands = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            // Десериализация XML в DTO
            MusicBandsXmlDto importData = xmlMapper.readValue(inputStream, MusicBandsXmlDto.class);

            if (importData == null || importData.getMusicBands() == null || importData.getMusicBands().isEmpty()) {
                throw new IllegalArgumentException("XML file does not contain any valid 'musicBand' elements or is malformed.");
            }

            for (MusicBandCreateDto dto : importData.getMusicBands()) {
                // 1. Валидация DTO с помощью внедрённого this.validator
                Set<ConstraintViolation<MusicBandCreateDto>> violations = this.validator.validate(dto);
                if (!violations.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Validation failed for band '")
                            .append(dto.getName() != null ? dto.getName() : "[Unknown Name]")
                            .append("': ");
                    violations.forEach(v -> errorMsg.append(v.getPropertyPath()).append(" ").append(v.getMessage()).append("; "));
                    throw new IllegalArgumentException(errorMsg.toString());
                }

                // 2. Используем существующий метод create, который обрабатывает все связи
                createdBands.add(this.create(dto));
            }
        } catch (Exception e) {
            System.err.println("XML Import failed: " + e.getMessage());
            // Перебрасываем RuntimeException для отката транзакции
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }

        if (!createdBands.isEmpty()) {
            notifyClients("BANDS_IMPORTED");
        }
        return createdBands;
    }
}
