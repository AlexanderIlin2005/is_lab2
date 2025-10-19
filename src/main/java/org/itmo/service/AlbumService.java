package org.itmo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Для Real-Time

import jakarta.persistence.EntityNotFoundException;
import org.itmo.model.Album;
import org.itmo.repository.AlbumRepository; // Убедитесь, что этот репозиторий существует

import java.util.List;

@Service
@Transactional
public class AlbumService {
    private final AlbumRepository albumRepository;
    private final SimpMessagingTemplate messagingTemplate; // Для Real-Time

    public AlbumService(AlbumRepository albumRepository, SimpMessagingTemplate messagingTemplate) {
        this.albumRepository = albumRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private void notifyClients() {
        // Уведомляем клиентов о любом изменении, так как альбомы могут быть bestAlbum
        messagingTemplate.convertAndSend("/topic/bands/updates", "ALBUM_UPDATED");
    }

    public List<Album> getAll() {
        return albumRepository.findAll();
    }

    public Album getById(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Album not found: " + id));
    }

    public Album create(Album album) {
        Album createdAlbum = albumRepository.save(album);
        notifyClients();
        return createdAlbum;
    }

    public Album update(Long id, Album patch) {
        Album existing = getById(id);

        // Обновляем только разрешенные поля (name, tracks, length)
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getTracks() > 0) existing.setTracks(patch.getTracks());
        if (patch.getLength() > 0) existing.setLength(patch.getLength());

        if (patch.getSales() != null) {
            // Проверка на null обязательна, так как Double может быть null
            existing.setSales(patch.getSales());
        }

        Album updatedAlbum = albumRepository.save(existing);
        notifyClients();
        return updatedAlbum;
    }

    public void delete(Long id) {
        // NOTE: Если Album связан с MusicBand (bestAlbum), может потребоваться
        // сначала обнулить ссылку в MusicBand, чтобы избежать ошибки FK.
        albumRepository.deleteById(id);
        notifyClients();
    }
}