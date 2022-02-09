package org.shrtr.core.controllers;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.LinkMetric;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.services.LinkService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
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

    @GetMapping("/{id}/metrics")
    public List<LinkMetricsDto> getLinkMetrics(@PathVariable("id") UUID id, @AuthenticationPrincipal User user,
                                               @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Optional<Link> link = linkService.getLink(user, id);
        if (link.isPresent()) {
            return linkService.findLinkMetrics(link.get(), from, to)
                    .stream()
                    .map(LinkMetricsDto::fromLinkMetric)
                    .collect(Collectors.toList());
        }
        throw new NotFoundException();
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

    @Builder
    @Data
    public static class LinkMetricsDto {
        private LocalDate date;
        private long count;

        static LinkMetricsDto fromLinkMetric(LinkMetric linkMetric) {
            return LinkMetricsDto.builder()
                    .count(linkMetric.getCount())
                    .date(linkMetric.getDate())
                    .build();
        }
    }

}
