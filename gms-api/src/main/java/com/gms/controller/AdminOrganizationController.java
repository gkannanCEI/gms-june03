package com.gms.controller;

import com.gms.entity.Organization;
import com.gms.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/organizations")
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    public AdminOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<Organization> create(@RequestBody Organization org) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.createOrganization(org));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> update(@PathVariable Long id, @RequestBody Organization org) {
        return ResponseEntity.ok(organizationService.updateOrganization(id, org));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        organizationService.deactivateOrganization(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Organization>> list() {
        return ResponseEntity.ok(organizationService.listOrganizations());
    }
}
