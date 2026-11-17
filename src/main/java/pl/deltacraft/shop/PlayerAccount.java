package pl.deltacraft.shop;

public final class PlayerAccount {
    // Money is stored as cents to avoid floating-point transaction errors.
    public long cents = 0L;
    public int level = 0;
    public long xp = 0L;

    public void addXp(long amount) {
        if (amount <= 0) return;
        xp = Math.min(1_000_000_000_000L, xp + amount);
        while (level < 500 && xp >= MultiMine.requiredXp(level)) {
            xp -= MultiMine.requiredXp(level);
            level++;
        }
    }

    public void earnBaseDollars(long dollars) {
        if (dollars <= 0) return;
        double value = dollars * 100d * MultiMine.earningsMultiplier(level);
        long centsToAdd = (long) Math.min(value, 1_000_000_000_000d);
        cents = Math.min(1_000_000_000_000_000L, cents + centsToAdd);
    }

    public String money() { return String.format(java.util.Locale.ROOT, "$%.2f", cents / 100.0); }
    
   
    public String shortMoney() {
        double dollars = cents / 100.0;
    
        if (dollars >= 1_000_000_000) {
            return shortNumber(dollars / 1_000_000_000) + " mld";
        }
        if (dollars >= 1_000_000) {
            return shortNumber(dollars / 1_000_000) + " mln";
        }
        if (dollars >= 1_000) {
            return shortNumber(dollars / 1_000) + " tys.";
        }
    
        return String.format(
            java.util.Locale.ROOT, "$%.2f", dollars
        );
    }
    
    private static String shortNumber(double value) {
        String result = String.format(
            java.util.Locale.ROOT, "%.1f", value
        );
    
        if (result.endsWith(".0")) {
            result = result.substring(0, result.length() - 2);
        }
    
        return "$" + result.replace('.', ',');
    }


}
