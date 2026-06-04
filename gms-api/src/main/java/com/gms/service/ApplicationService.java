package com.gms.service;

import com.gms.entity.Application;
import com.gms.entity.Organization;
import com.gms.entity.ProgramRound;
import com.gms.entity.StatusHistory;
import com.gms.exception.ResourceNotFoundException;
import com.gms.exception.ValidationException;
import com.gms.repository.ApplicationRepository;
import com.gms.repository.OrganizationRepository;
import com.gms.repository.ProgramRoundRepository;
import com.gms.repository.StatusHistoryRepository;
import com.gms.security.SecurityContextProvider;
import com.gms.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProgramRoundRepository roundRepository;
    private final OrganizationRepository organizationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final SecurityContextProvider securityContextProvider;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ProgramRoundRepository roundRepository,
                              OrganizationRepository organizationRepository,
                              StatusHistoryRepository statusHistoryRepository,
                              SecurityContextProvider securityContextProvider) {
        this.applicationRepository = applicationRepository;
        this.roundRepository = roundRepository;
        this.organizationRepository = organizationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.securityContextProvider = securityContextProvider;
    }

    public Application createApplication(Long roundId) {
        UserPrincipal user = securityContextProvider.getCurrentUser();
        if (user.organizationId() == null) {
            throw new ValidationException("User has no organization assigned");
        }

        ProgramRound round = roundRepository.findById(roundId)
            .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
        if (!"ACTIVE".equals(round.getStatus())) {
            throw new ValidationException("Round is not active");
        }

        Organization org = organizationRepository.findById(user.organizationId())
            .orElseThrow(() -> new ValidationException("Organization not found"));

        // Check eligibility
        if (round.getEligibleOrganizationTypes() != null && !round.getEligibleOrganizationTypes().isBlank()) {
            Set<String> eligible = Arrays.stream(round.getEligibleOrganizationTypes().split(","))
                .map(String::trim).collect(Collectors.toSet());
            if (!eligible.contains(org.getOrganizationType())) {
                throw new ValidationException("Organization type " + org.getOrganizationType()
                    + " is not eligible for this round");
            }
        }

        // Check for existing application
        Optional<Application> existing = applicationRepository.findByProgramRoundIdAndUserId(roundId, user.userId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Application app = new Application();
        app.setProgramRound(round);
        app.setOrganization(org);
        app.setUserId(user.userId());
        app.setStatus("DRAFT");
        Application saved = applicationRepository.save(app);
        recordStatusChange(saved.getId(), null, "DRAFT");
        return saved;
    }

    /**
     * CR-01: Uses JOIN FETCH to eagerly load programRound in a single query,
     * preventing LazyInitializationException when getProgramRound().getId() is
     * called in ApplicationDataController.resolveRoundPageId() after this method
     * returns and the transaction closes.
     */
    @Transactional(readOnly = true)
    public Application getApplication(Long appId) {
        return applicationRepository.findByIdWithRound(appId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + appId));
    }

    public Application getMyApplication(Long roundId) {
        UserPrincipal user = securityContextProvider.getCurrentUser();
        return applicationRepository.findByProgramRoundIdAndUserId(roundId, user.userId())
            .orElseThrow(() -> new ResourceNotFoundException("No application found for this round"));
    }

    public Application updateStatus(Long appId, String newStatus) {
        UserPrincipal user = securityContextProvider.getCurrentUser();
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getUserId().equals(user.userId()) && !user.isAdmin()) {
            throw new ValidationException("Access denied");
        }

        String currentStatus = app.getStatus();
        validateStatusTransition(currentStatus, newStatus, user.isAdmin());

        String oldStatus = app.getStatus();
        app.setStatus(newStatus);
        applicationRepository.save(app);
        recordStatusChange(appId, oldStatus, newStatus);
        return app;
    }

    public Application reopenApplication(Long appId) {
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!"SUBMITTED".equals(app.getStatus())) {
            throw new ValidationException("Only SUBMITTED applications can be reopened");
        }
        app.setStatus("DRAFT");
        applicationRepository.save(app);
        recordStatusChange(appId, "SUBMITTED", "DRAFT");
        return app;
    }

    @Transactional(readOnly = true)
    public Page<Application> listApplicationsForRound(Long roundId, String status,
                                                       Boolean eligibilityWarning, Pageable pageable) {
        return applicationRepository.findByRoundFiltered(roundId, status, eligibilityWarning, pageable);
    }

    public void verifyOwnership(Long appId) {
        UserPrincipal user = securityContextProvider.getCurrentUser();
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!app.getUserId().equals(user.userId()) && !user.isAdmin()) {
            throw new ValidationException("Access denied");
        }
    }

    private void validateStatusTransition(String current, String target, boolean isAdmin) {
        switch (target) {
            case "SUBMITTED" -> {
                if (!"DRAFT".equals(current)) throw new ValidationException("Can only submit from DRAFT");
            }
            case "WITHDRAWN" -> {
                if (!"DRAFT".equals(current) && !"SUBMITTED".equals(current))
                    throw new ValidationException("Can only withdraw from DRAFT or SUBMITTED");
            }
            case "DRAFT" -> {
                if (!isAdmin || !"SUBMITTED".equals(current))
                    throw new ValidationException("Only admin can reopen a SUBMITTED application");
            }
            default -> throw new ValidationException("Invalid status: " + target);
        }
    }

    private void recordStatusChange(Long appId, String from, String to) {
        StatusHistory history = new StatusHistory();
        history.setEntityType("APPLICATION");
        history.setEntityId(appId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(securityContextProvider.getCurrentUser().userId());
        statusHistoryRepository.save(history);
    }
}
