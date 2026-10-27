package com.faust0z.B2BSystem.access.web;

import com.faust0z.B2BSystem.access.application.ScanService;
import com.faust0z.B2BSystem.access.web.ScanRequest;
import com.faust0z.B2BSystem.access.web.ScanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/access/scan")
public class ScanController {

    private final ScanService scanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ScanResponse processScan(@RequestBody ScanRequest request) {
        return scanService.processScan(request);
    }
}
