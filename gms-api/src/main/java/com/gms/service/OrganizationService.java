package com.gms.service;

import com.gms.entity.Organization;
import com.gms.exception.ValidationException;
import com.gms.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrganizationService {

    private static final Set<String> VALID_TYPES = Set.of(
        "NONPROFIT", "GOVERNMENT", "BUSINESS", "EDUCATIONAL", "OTHER"
    );

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization createOrganization(Organization org) {
        validate(org);
        if (organizationRepository.existsByName(org.getName())) {
            throw new ValidationException("Organization name already exists: " + org.getName());
        }
        return organizationRepository.save(org);
    }

    public Organization updateOrganization(Long id, Organization updates) {
        Organization existing = organizationRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Organization not found: " + id));
        if (!existing.getName().equalsIgnoreCase(updates.getName()) &&
            organizationRepository.existsByName(updates.getName())) {
            throw new ValidationException("Organization name already exists");
        }
        existing.setName(updates.getName());
        existing.setOrganizationType(updates.getOrganizationType());
        existing.setDescription(updates.getDescription());
        validate(existing);
        return organizationRepository.save(existing);
    }

    public void deactivateOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Organization not found: " + id));
        org.setActive(false);
        organizationRepository.save(org);
    }

    @Transactional(readOnly = true)
    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    private void validate(Organization org) {
        if (org.getName() == null || org.getName().isBlank()) {
            throw new ValidationException("Organization name is required");
        }
        if (org.getName().length() > 255) {
            throw new ValidationException("Organization name must be at most 255 characters");
        }
        if (org.getOrganizationType() == null || !VALID_TYPES.contains(org.getOrganizationType())) {
            throw new ValidationException("organizationType must be one of: " + VALID_TYPES);
        }
    }
}
