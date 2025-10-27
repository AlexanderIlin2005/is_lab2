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

// --- NECESSARY IMPORTS ---
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // For ZonedDateTime parsing
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator; // Jakarta Bean Validation
import org.itmo.dto.MusicBandsXmlDto; // Your new DTO
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
// --- END IMPORTS ---

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class MusicBandService {
    private final MusicBandRepository musicBandRepository;
    private final AlbumRepository albumRepository;
    private final CoordinatesRepository coordinatesRepository;
    private final StudioRepository studioRepository;
    //private final MusicBandEventsPublisher eventsPublisher;
    private final MusicBandMapper musicBandMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final Validator validator; // Inject Validator

    public MusicBandService(MusicBandRepository musicBandRepository,
                            AlbumRepository albumRepository,
                            CoordinatesRepository coordinatesRepository,
                            StudioRepository studioRepository,
                            MusicBandMapper musicBandMapper,
                            SimpMessagingTemplate messagingTemplate,
                            Validator validator) {
        this.musicBandRepository = musicBandRepository;
        this.albumRepository = albumRepository;
        this.coordinatesRepository = coordinatesRepository;
        this.studioRepository = studioRepository;
        //this.eventsPublisher = eventsPublisher;
        this.musicBandMapper = musicBandMapper;
        this.messagingTemplate = messagingTemplate;
        this.validator = validator; // Initialize validator
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
    @Transactional // Ensures atomicity: either all import or none
    public List<MusicBandResponseDto> importFromXml(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Import file cannot be empty.");
        }

        // Configure XmlMapper for ZonedDateTime
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO strings

        List<MusicBandResponseDto> createdBands = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            // Deserialize XML into the wrapper DTO
            MusicBandsXmlDto importData = xmlMapper.readValue(inputStream, MusicBandsXmlDto.class);

            if (importData == null || importData.getMusicBands() == null || importData.getMusicBands().isEmpty()) {
                throw new IllegalArgumentException("XML file does not contain any valid 'musicBand' elements or is malformed.");
            }

            // Process each band within the transaction
            for (MusicBandCreateDto dto : importData.getMusicBands()) {
                // 1. Standard Bean Validation (checks @NotNull, @Min, etc.)
                Set<ConstraintViolation<MusicBandCreateDto>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Validation failed for band '")
                            .append(dto.getName() != null ? dto.getName() : "[Unknown Name]")
                            .append("': ");
                    violations.forEach(v -> errorMsg.append(v.getPropertyPath()).append(" ").append(v.getMessage()).append("; "));
                    // Throw exception to trigger transaction rollback
                    throw new IllegalArgumentException(errorMsg.toString());
                }

                // 2. Add custom business logic validation here if needed
                // (e.g., uniqueness constraints not covered by DB/JPA)

                // 3. Use the existing 'create' method which handles entity linking
                // If 'create' fails for any reason, the transaction rolls back
                createdBands.add(this.create(dto));
            }
        } catch (Exception e) {
            // Log the specific error
            System.err.println("XML Import failed: " + e.getMessage());
            // Rethrow to ensure transaction rollback and inform the controller
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }

        // If transaction commits successfully, notify clients
        if (!createdBands.isEmpty()) {
            notifyClients("BANDS_IMPORTED"); // Use a specific message type if needed
        }
        return createdBands;
    }
    // --- END OF IMPORT METHOD ---
}
