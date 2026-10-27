package com.bookloop;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Provides a simple public status response for the backend base URL. */
@RestController
public class BackendStatusController {

    /** Confirms that the API process is reachable and points clients to its API prefix. */
    @GetMapping("/")
    public Map<String, String> status() {
        return Map.of("service", "BookLoop API", "status", "running", "apiBasePath", "/api");
    }
}
