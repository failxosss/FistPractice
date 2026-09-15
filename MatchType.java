package com.fistpractice.match;

/**
 * All four flow through the same MatchEngine (spec section 24) - this enum
 * only changes bookkeeping (rating pool, history labelling), never combat logic.
 */
public enum MatchType {
    DUEL_1V1,
    PARTY_VS_PARTY,
    TEAM_VS_TEAM,
    TOURNAMENT
}
