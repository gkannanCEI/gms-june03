package com.gms.controller;

import com.gms.service.ProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/programs")
public class ApplicantProgramController {

    private final ProgramService programService;

    public ApplicantProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> listActivePrograms() {
        List<Map<String, Object>> programs = programService.listActivePrograms();
        return ResponseEntity.ok(programs);
    }
}
