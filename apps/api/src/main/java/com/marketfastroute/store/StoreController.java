package com.marketfastroute.store;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreResponse> findAll() {
        return storeService.findAll();
    }

    @GetMapping("/{id}")
    public StoreResponse findById(@PathVariable UUID id) {
        return storeService.findById(id);
    }
}
