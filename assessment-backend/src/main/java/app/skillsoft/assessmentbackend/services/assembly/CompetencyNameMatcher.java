package app.skillsoft.assessmentbackend.services.assembly;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for fuzzy matching of competency names.
 *
 * O*NET benchmark competency names often differ slightly from internal competency names
 * (e.g., O*NET "Critical Thinking" vs internal "Critical Thinking Skills").
 * This matcher uses token-based Jaccard similarity and containment checks
 * to find the best match among candidate names.
 */
public final class CompetencyNameMatcher {

    /**
     * Default similarity threshold for fuzzy matching.
     * A Jaccard similarity of 0.6 means at least 60% of tokens overlap.
     */
    public static final double DEFAULT_THRESHOLD = 0.6;

    private CompetencyNameMatcher() {
        // Utility class - prevent instantiation
    }

    /**
     * Find the best matching name from a collection of candidate names using fuzzy matching.
     *
     * Matching strategy (in order of priority):
     * 1. Containment check: if one normalized name fully contains the other, it's a match
     * 2. Jaccard similarity: token-based overlap score must exceed the threshold
     *
     * @param onetName       The O*NET competency name to match
     * @param candidateNames The collection of internal competency names to search
     * @param threshold      Minimum Jaccard similarity score (0.0-1.0) to consider a match
     * @return The best matching candidate name, or empty if no match exceeds the threshold
     */
    public static Optional<String> findBestMatch(String onetName, Collection<String> candidateNames, double threshold) {
        if (onetName == null || onetName.isBlank() || candidateNames == null || candidateNames.isEmpty()) {
            return Optional.empty();
        }

        String normalizedOnet = normalize(onetName);
        Set<String> onetTokens = tokenize(normalizedOnet);

        if (onetTokens.isEmpty()) {
            return Optional.empty();
        }

        String bestMatch = null;
        double bestScore = 0.0;

        for (String candidate : candidateNames) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            String normalizedCandidate = normalize(candidate);

            // Priority 1: Containment check (one name fully contains the other)
            // Weight by length ratio to prevent trivially short names from perfect-matching
            if (normalizedOnet.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedOnet)) {
                String shorter = normalizedOnet.length() <= normalizedCandidate.length()
                        ? normalizedOnet : normalizedCandidate;
                String longer = normalizedOnet.length() > normalizedCandidate.length()
                        ? normalizedOnet : normalizedCandidate;
                double lengthRatio = (double) shorter.length() / longer.length();

                double containmentScore;
                // Require minimum 8 chars AND good length ratio for high containment score
                if (shorter.length() >= 8) {
                    containmentScore = Math.max(lengthRatio, 0.7); // Minimum 0.7 for genuine containment
                } else {
                    containmentScore = lengthRatio; // Short strings get proportional score
                }

                if (containmentScore > bestScore) {
                    bestScore = containmentScore;
                    bestMatch = candidate;
                }
                continue;
            }

            // Priority 2: Jaccard similarity on tokens
            Set<String> candidateTokens = tokenize(normalizedCandidate);
            if (candidateTokens.isEmpty()) {
                continue;
            }

            double similarity = jaccardSimilarity(onetTokens, candidateTokens);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = candidate;
            }
        }

        if (bestScore >= threshold && bestMatch != null) {
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }

    /**
     * Find the best matching name using the default threshold.
     *
     * @param onetName       The O*NET competency name to match
     * @param candidateNames The collection of internal competency names to search
     * @return The best matching candidate name, or empty if no match found
     */
    public static Optional<String> findBestMatch(String onetName, Collection<String> candidateNames) {
        return findBestMatch(onetName, candidateNames, DEFAULT_THRESHOLD);
    }

    /**
     * Compute Jaccard similarity between two token sets.
     * Jaccard = |intersection| / |union|
     *
     * @param tokensA First token set
     * @param tokensB Second token set
     * @return Similarity score between 0.0 (no overlap) and 1.0 (identical)
     */
    static double jaccardSimilarity(Set<String> tokensA, Set<String> tokensB) {
        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 1.0;
        }
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return (double) intersection.size() / union.size();
    }

    /**
     * Normalize a competency name for matching.
     * Converts to lowercase and strips all punctuation characters.
     *
     * @param name The name to normalize
     * @return Normalized name
     */
    static String normalize(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();
    }

    /**
     * Tokenize a normalized name into a set of words.
     * Splits on whitespace and filters out empty tokens.
     *
     * @param normalizedName The normalized name to tokenize
     * @return Set of tokens
     */
    static Set<String> tokenize(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(normalizedName.split("\\s+")));
    }
}
