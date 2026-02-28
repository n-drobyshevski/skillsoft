package app.skillsoft.assessmentbackend.services.external;

import app.skillsoft.assessmentbackend.domain.dto.ResolvedOnetCompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.assembly.CompetencyNameMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves O*NET benchmark competency names to internal Competency entities.
 *
 * Extracted from JobFitAssembler so that both the assembler and the library
 * restriction endpoint use identical resolution logic, ensuring the canvas
 * and assembler always agree on which competencies match an O*NET profile.
 *
 * Resolution strategy (two-phase):
 * 1. Fast path: case-insensitive name lookup via findByNameInIgnoreCase
 * 2. Fallback: scan all active competencies and match via standardCodes
 *    (onetRef.title, escoRef.title) + CompetencyNameMatcher fuzzy matching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnetCompetencyResolver {

    private final OnetService onetService;
    private final CompetencyRepository competencyRepository;

    /**
     * Resolve O*NET benchmark competency names to internal competencies.
     *
     * @param socCode Standard Occupational Classification code (e.g., "15-1252.00")
     * @return List of resolved competencies with their benchmark scores, or empty if profile not found
     * @throws IllegalArgumentException if socCode is null or blank
     */
    public List<ResolvedOnetCompetencyDto> resolveCompetencies(String socCode) {
        if (socCode == null || socCode.isBlank()) {
            throw new IllegalArgumentException("SOC code must not be null or blank");
        }

        var onetProfile = onetService.getProfile(socCode);
        if (onetProfile.isEmpty()) {
            log.warn("No O*NET profile found for SOC code: {}", socCode);
            return List.of();
        }

        var benchmarks = onetProfile.get().benchmarks();
        log.debug("Resolving {} benchmark competencies for SOC code {}", benchmarks.size(), socCode);

        Set<String> benchmarkNames = benchmarks.keySet().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        // Phase 1: fast path — direct name match
        List<Competency> candidates = competencyRepository.findByNameInIgnoreCase(benchmarkNames);

        // Phase 2: if name match found fewer competencies than benchmarks, broaden search
        if (candidates.size() < benchmarkNames.size()) {
            log.info("Name-based lookup matched only {}/{} benchmarks. Falling back to full active scan.",
                candidates.size(), benchmarkNames.size());
            candidates = competencyRepository.findByIsActiveTrue();
        }

        Map<String, List<Competency>> lookupMaps = buildCompetencyLookupMaps(candidates);

        // Resolve each benchmark to an internal competency
        List<ResolvedOnetCompetencyDto> resolved = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();

        for (var entry : benchmarks.entrySet()) {
            String benchmarkName = entry.getKey();
            double benchmarkScore = entry.getValue();

            Competency matched = findCompetencyByName(benchmarkName, lookupMaps);
            if (matched != null && seenIds.add(matched.getId())) {
                resolved.add(new ResolvedOnetCompetencyDto(
                    matched.getId(),
                    matched.getName(),
                    matched.getCategory().name(),
                    benchmarkName,
                    benchmarkScore
                ));
            }
        }

        log.info("Resolved {}/{} O*NET benchmarks to internal competencies for SOC code {}",
            resolved.size(), benchmarks.size(), socCode);

        return resolved;
    }

    /**
     * Build lookup maps from competency name/O*NET code/title/ESCO title to competency list.
     * Indexes each competency by ALL known identifiers so cross-language/cross-taxonomy
     * matching works.
     */
    public Map<String, List<Competency>> buildCompetencyLookupMaps(List<Competency> competencies) {
        Map<String, List<Competency>> lookup = new HashMap<>();
        for (var c : competencies) {
            lookup.computeIfAbsent(c.getName().toLowerCase(), k -> new ArrayList<>()).add(c);

            var standardCodes = c.getStandardCodes();
            if (standardCodes == null) continue;

            if (standardCodes.hasOnetMapping()) {
                var onetRef = standardCodes.onetRef();
                if (onetRef.code() != null && !onetRef.code().isBlank()) {
                    lookup.computeIfAbsent(onetRef.code().toLowerCase(), k -> new ArrayList<>()).add(c);
                }
                if (onetRef.title() != null && !onetRef.title().isBlank()) {
                    lookup.computeIfAbsent(onetRef.title().toLowerCase(), k -> new ArrayList<>()).add(c);
                }
            }

            if (standardCodes.hasEscoMapping()) {
                var escoRef = standardCodes.escoRef();
                if (escoRef.title() != null && !escoRef.title().isBlank()) {
                    lookup.computeIfAbsent(escoRef.title().toLowerCase(), k -> new ArrayList<>()).add(c);
                }
            }
        }
        return lookup;
    }

    /**
     * Find a competency by benchmark name using exact match, then fuzzy fallback.
     */
    private Competency findCompetencyByName(String benchmarkName, Map<String, List<Competency>> lookupMaps) {
        List<Competency> exact = lookupMaps.getOrDefault(benchmarkName.toLowerCase(), List.of());
        if (!exact.isEmpty()) {
            return exact.getFirst();
        }

        // Fuzzy fallback
        Optional<String> fuzzyMatch = CompetencyNameMatcher.findBestMatch(
            benchmarkName, lookupMaps.keySet());

        if (fuzzyMatch.isPresent()) {
            log.info("Fuzzy match: O*NET '{}' → internal key '{}'", benchmarkName, fuzzyMatch.get());
            List<Competency> fuzzy = lookupMaps.getOrDefault(fuzzyMatch.get(), List.of());
            if (!fuzzy.isEmpty()) {
                return fuzzy.getFirst();
            }
        }

        log.debug("No match found for O*NET benchmark: {}", benchmarkName);
        return null;
    }
}
