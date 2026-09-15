package com.fistpractice.ranking;

public class EloCalculator {

    private final int kFactorProvisional;
    private final int kFactorNormal;
    private final int provisionalMatches;

    public EloCalculator(int kFactorProvisional, int kFactorNormal, int provisionalMatches) {
        this.kFactorProvisional = kFactorProvisional;
        this.kFactorNormal = kFactorNormal;
        this.provisionalMatches = provisionalMatches;
    }

    /**
     * Standard logistic Elo expectation.
     */
    public double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
    }

    /**
     * @param won               did the "A" side win
     * @param ratingA           rating of the player/team being scored
     * @param ratingB           rating of the opponent
     * @param placementMatchesA how many ranked matches A has already played (for provisional K)
     * @return the ELO delta to apply to A (opponent gets the mirrored delta)
     */
    public int calculateDelta(boolean won, int ratingA, int ratingB, int placementMatchesA) {
        double expected = expectedScore(ratingA, ratingB);
        double actual = won ? 1.0 : 0.0;
        int k = placementMatchesA < provisionalMatches ? kFactorProvisional : kFactorNormal;
        return (int) Math.round(k * (actual - expected));
    }
}
