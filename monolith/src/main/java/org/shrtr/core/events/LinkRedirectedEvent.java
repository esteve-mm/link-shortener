package org.shrtr.core.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class LinkRedirectedEvent {

    private LocalDateTime timestamp;
    private UUID id;
    private String original;
    private String shortened;
    private String ownerUsername;
    private Long latency;
}
