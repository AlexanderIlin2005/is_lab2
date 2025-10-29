package org.itmo.controller;

import jakarta.validation.Valid;
import org.itmo.dto.MusicBandCreateDto;
import org.itmo.dto.MusicBandResponseDto;
import org.itmo.model.MusicGenre;
import org.itmo.service.MusicBandService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// --- NECESSARY IMPORTS ---
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map; // For response bodies
// --- END IMPORTS ---

import java.util.List;

@RestController
@RequestMapping("/api/music-bands")
public class MusicBandController {
    private final MusicBandService musicBandService;

    public MusicBandController(MusicBandService musicBandService) {
        this.musicBandService = musicBandService;
    }

    @GetMapping(produces = "application/json")
    public Page<MusicBandResponseDto> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(required = false) String order,
                                           @RequestParam(required = false) String nameEquals) {
        Sort sortSpec = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            Sort.Direction dir = (order != null && order.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortSpec = Sort.by(dir, sort);
        }
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return musicBandService.list(nameEquals, pageable);
    }

    @GetMapping(value="/{id}", produces = "application/json")
    public MusicBandResponseDto get(@PathVariable Long id) {
        return musicBandService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MusicBandResponseDto create(@Valid @RequestBody MusicBandCreateDto musicBand) {
        return musicBandService.create(musicBand);
    }

    @PatchMapping("/{id}")
    public MusicBandResponseDto update(@PathVariable Long id,
                                       @Valid @RequestBody MusicBandCreateDto patch) {
        return musicBandService.update(id, patch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        musicBandService.delete(id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/by-studio")
    public Map<String, Object> deleteOneByStudio(@RequestParam String studioName) {
        boolean deleted = musicBandService.deleteOneByStudioName(studioName);
        if (deleted) return Map.of("deleted", 1);
        return Map.of("deleted", 0);
    }

    @GetMapping(value="/average-album-count", produces = "application/json")
    public Map<String, Object> getAverageAlbumCount() {
        Double average = musicBandService.getAverageAlbumCount();
        return Map.of("average", average != null ? average : 0);
    }

    @GetMapping(value="/count-by-studio", produces = "application/json")
    public Map<String, Object> countByStudioNameGreaterThan(@RequestParam String studioName) {
        long count = musicBandService.countByStudioNameGreaterThan(studioName);
        return Map.of("count", count);
    }

    @GetMapping(value="/by-genre/{genre}", produces = "application/json")
    public List<MusicBandResponseDto> findByGenre(@PathVariable MusicGenre genre) {
        return musicBandService.findByGenre(genre);
    }

    @PatchMapping("/{id}/remove-participant")
    public Map<String, Object> removeParticipant(@PathVariable Long id) {
        long newCount = musicBandService.removeParticipant(id);
        return Map.of("numberOfParticipants", newCount);
    }

    // --- NEW IMPORT ENDPOINT ---
    @PostMapping("/import")
    public ResponseEntity<?> importBandsFromXml(@RequestParam("file") MultipartFile file) {
        // Basic file checks
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select an XML file to upload."));
        }
        // Check content type (optional but recommended)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/xml") && !contentType.equals("text/xml"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Please upload an XML file. Mime type was: " + contentType));
        }

        try {
            // Call the transactional service method
            List<MusicBandResponseDto> importedBands = musicBandService.importFromXml(file);
            // Return success response
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully imported " + importedBands.size() + " music bands.",
                    "importedCount", importedBands.size()
            ));
        } catch (IllegalArgumentException e) { // Catch validation errors specifically
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Import failed due to validation: " + e.getMessage()));
        } catch (RuntimeException e) { // Catch other runtime errors (like parsing, DB issues)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Import failed: " + e.getMessage()));
        } catch (Exception e) { // Catch unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred during import: " + e.getMessage()));
        }
    }
    // --- END OF IMPORT ENDPOINT ---
}
