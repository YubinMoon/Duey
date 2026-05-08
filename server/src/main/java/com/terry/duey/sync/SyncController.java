package com.terry.duey.sync;

import com.terry.duey.auth.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync/v1")
public class SyncController {
    private final SyncRepository syncRepository;

    public SyncController(SyncRepository syncRepository) {
        this.syncRepository = syncRepository;
    }

    @GetMapping("/bootstrap")
    public SyncPayload bootstrap(@AuthenticationPrincipal UserPrincipal user) {
        return syncRepository.bootstrap(user.userId());
    }

    @PostMapping("/push")
    public SyncPayload push(
            @AuthenticationPrincipal UserPrincipal user, @RequestBody SyncPayload request) {
        return syncRepository.push(user.userId(), request);
    }
}
