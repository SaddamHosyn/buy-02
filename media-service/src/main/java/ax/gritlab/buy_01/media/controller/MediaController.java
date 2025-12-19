package ax.gritlab.buy_01.media.controller;

import ax.gritlab.buy_01.media.model.Media;
import ax.gritlab.buy_01.media.model.User;
import ax.gritlab.buy_01.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/images")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<List<Media>> getAllUserMedia(Authentication authentication) {
        List<Media> mediaList = mediaService.findByUserId(((User) authentication.getPrincipal()).getId());
        return ResponseEntity.ok(mediaList);
    }

    @PostMapping("/images")
    @PreAuthorize("hasAnyAuthority('SELLER', 'CLIENT')")
    public ResponseEntity<Media> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Media savedMedia = mediaService.save(file, (User) authentication.getPrincipal());
        return ResponseEntity.ok(savedMedia);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> serveImage(@PathVariable String id) {
        try {
            MediaService.MediaResource mediaResource = mediaService.getResourceById(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaResource.getContentType())
                    .body(mediaResource.getResource());
        } catch (RuntimeException e) {
            // Return 404 for missing images instead of throwing exception
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/images/{id}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<Void> deleteImage(@PathVariable String id, Authentication authentication) {
        mediaService.delete(id, (User) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/images/{id}/product/{productId}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<Media> associateWithProduct(
            @PathVariable String id,
            @PathVariable String productId,
            Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        Media updatedMedia = mediaService.associateWithProduct(id, productId, userId);
        return ResponseEntity.ok(updatedMedia);
    }
}
