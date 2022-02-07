package org.shrtr.core.controllers;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.services.LinkService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
public class LinksController {

    private final LinkService linkService;

    @PostMapping
    public LinkDto createLink(@RequestBody CreateLinkDto body, @AuthenticationPrincipal User user) {
        Link link = linkService.create(body.getOriginal(), user);
        return LinkDto.fromLink(link);
    }

    @GetMapping
    public List<LinkDto> getLinks(@AuthenticationPrincipal User user) {
        return linkService.getAllLinks(user)
                .stream()
                .map(LinkDto::fromLink)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public LinkDto getLink(@PathVariable("id") UUID id, @AuthenticationPrincipal User user) {
        return linkService.getLink(user, id)
                .map(LinkDto::fromLink)
                .orElseThrow(NotFoundException::new);
    }

    @DeleteMapping("/{id}")
    public LinkDto deleteLink(@PathVariable("id") UUID id, @AuthenticationPrincipal User user) {
        return linkService.deleteLink(user, id)
                .map(LinkDto::fromLink)
                .orElseThrow(NotFoundException::new);
    }

    @Data
    public static class CreateLinkDto {
        String original;
    }

    @Builder
    @Data
    public static class LinkDto {
        private UUID id;
        private String original;
        private String shortened;

        static LinkDto fromLink(Link link) {
            return LinkDto.builder()
                    .id(link.getId())
                    .original(link.getOriginal())
                    .shortened(link.getShortened())
                    .build();
        }
    }

}
