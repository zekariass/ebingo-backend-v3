package com.ebingo.backend.game.service.autoplay;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/autoplay")
@RequiredArgsConstructor
public class AutoPlayAdminController {

    private final GlobalAutoPlayManager manager;

    @PostMapping("/enable")
    public void enable(@RequestParam(defaultValue = "Admin triggered") String reason) {
        manager.enable(reason).subscribe();
    }

    @PostMapping("/disable")
    public void disable(@RequestParam(defaultValue = "Admin triggered") String reason) {
        manager.disable(reason).subscribe();
    }

    @GetMapping
    public boolean status() {
        return manager.isEnabled();
    }
}
