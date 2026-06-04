package com.gms.controller;

import com.gms.dto.PageRenderDTO;
import com.gms.dto.PageSummaryDTO;
import com.gms.entity.ProgramRound;
import com.gms.exception.ResourceNotFoundException;
import com.gms.security.SecurityContextProvider;
import com.gms.service.ApplicationService;
import com.gms.service.PageBuilderService;
import com.gms.service.ProgramRoundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs/{programId}/rounds/{roundId}")
public class PageBuilderController {

    private final PageBuilderService pageBuilderService;
    private final ProgramRoundService roundService;
    private final SecurityContextProvider securityContextProvider;
    private final ApplicationService applicationService;

    public PageBuilderController(PageBuilderService pageBuilderService,
                                 ProgramRoundService roundService,
                                 SecurityContextProvider securityContextProvider,
                                 ApplicationService applicationService) {
        this.pageBuilderService = pageBuilderService;
        this.roundService = roundService;
        this.securityContextProvider = securityContextProvider;
        this.applicationService = applicationService;
    }

    /**
     * Returns the ordered page list for the applicant dashboard.
     * Accepts an optional {@code appId} query parameter so the service can
     * compute per-page completion status.
     */
    @GetMapping("/pages")
    public ResponseEntity<List<PageSummaryDTO>> listPages(@PathVariable Long programId,
                                                          @PathVariable Long roundId,
                                                          @RequestParam(required = false) Long appId) {
        ProgramRound round = roundService.getRound(roundId);
        if (!"ACTIVE".equals(round.getStatus())) {
            throw new ResourceNotFoundException("Round not active");
        }
        List<String> roles = securityContextProvider.getCurrentUser().roles();

        // Prefer the appId supplied by the client (avoids an extra DB round-trip).
        // Fall back to resolving from the authenticated user's application when absent.
        Long resolvedAppId = appId;
        if (resolvedAppId == null) {
            try {
                resolvedAppId = applicationService.getMyApplication(roundId).getId();
            } catch (Exception ignored) {
                // No application exists yet — completion defaults to false for all pages.
            }
        }

        List<PageSummaryDTO> pages = pageBuilderService.getPagesForRound(roundId, roles, resolvedAppId);
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<PageRenderDTO> getPage(@PathVariable Long programId,
                                                  @PathVariable Long roundId,
                                                  @PathVariable Long pageId) {
        ProgramRound round = roundService.getRound(roundId);
        if (!"ACTIVE".equals(round.getStatus())) {
            throw new ResourceNotFoundException("Round not active");
        }
        List<String> roles = securityContextProvider.getCurrentUser().roles();
        PageRenderDTO page = pageBuilderService.buildPage(roundId, pageId, roles);
        if (page == null) {
            throw new ResourceNotFoundException("Page not found in this round");
        }
        return ResponseEntity.ok(page);
    }
}
