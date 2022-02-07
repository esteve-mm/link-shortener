package org.shrtr.core.controllers;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shrtr.core.domain.entities.Link;
import org.shrtr.core.domain.entities.User;
import org.shrtr.core.services.LinkService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;

    @GetMapping("/{shortened}")
    public RedirectView getLinks(@PathVariable("shortened") String shortened) {
        log.info("Asked for redirect {}", shortened);
        return linkService.findForRedirect(shortened)
                .map(link -> {
                    RedirectView redirectView = new RedirectView();
                    redirectView.setUrl(link.getOriginal());
                    log.info("Found redirect from {} to {}", shortened, link.getOriginal());
                    return redirectView;
                })
                .orElseThrow(() -> {
                    log.info("Not found redirect from {}", shortened);
                    return new NotFoundException();
                });
    }

}
