package com.fistpractice.ranking;

public class Division {

    private final String name;
    private final int minElo;
    private final int subRanks;

    public Division(String name, int minElo, int subRanks) {
        this.name = name;
        this.minElo = minElo;
        this.subRanks = Math.max(1, subRanks);
    }

    public String getName() { return name; }
    public int getMinElo() { return minElo; }
    public int getSubRanks() { return subRanks; }

    /**
     * Renders e.g. "Diamond III" for a given elo, assuming elo falls within
     * [minElo, nextDivisionMinElo). subRankSpan is (nextMin - minElo)/subRanks.
     */
    public String render(int elo, int nextDivisionMinElo) {
        if (subRanks <= 1) return name;
        int span = Math.max(1, (nextDivisionMinElo - minElo) / subRanks);
        int sub = Math.min(subRanks, Math.max(1, ((elo - minElo) / span) + 1));
        // Sub-ranks count DOWN from I (highest) as elo increases, mirroring competitive
        // conventions (Diamond V is the lowest sub-rank, Diamond I the highest).
        int displayed = subRanks - sub + 1;
        return name + " " + toRoman(displayed);
    }

    private String toRoman(int n) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        if (n < 1) n = 1;
        if (n > romans.length) n = romans.length;
        return romans[n - 1];
    }
}
