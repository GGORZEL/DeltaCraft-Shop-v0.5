package pl.deltacraft.shop;

import java.nio.file.*;
import java.io.*;
import java.util.Properties;

/** Treasury and tax calculations, all monetary amounts are integer cents. */
public final class Treasury {
    public static final long LIMIT = 1_000_000_000_000_000L;
    private final Path file;
    private long cents;

    public Treasury(Path file) {
        this.file = file;
        Properties data = new Properties();
        if (Files.exists(file)) try (var in = Files.newBufferedReader(file)) {
            data.load(in);
            cents = Math.max(0L, Math.min(LIMIT, Long.parseLong(data.getProperty("cents", "0"))));
        } catch (Exception ex) { System.err.println("[DeltaCraft] Treasury read: " + ex); }
    }
    public long balance() { return cents; }
    /** Round to nearest cent, without floating-point money. */
    public static long percent(long amount, long rate) {
        return amount / 100L * rate + (amount % 100L * rate + 50L) / 100L;
    }
    public static long purchaseTax(long price) { return percent(price, 2); }
    /** Sale base is 35% of the original purchase price. */
    public static long saleTax(long baseSale) { return percent(baseSale, 2); }
    /** Extra 15% of the 65% that is not paid to the seller. */
    public static long saleReserve(long fullPrice) {
        long baseSale = fullPrice * 35L / 100L;
        return percent(fullPrice - baseSale, 15);
    }
    public boolean canDeposit(long amount) { return amount >= 0 && cents <= LIMIT - amount; }
    public void deposit(long amount) {
        if (!canDeposit(amount)) throw new IllegalArgumentException("Treasury limit exceeded");
        cents += amount;
        save();
    }
    public void withdrawTo(PlayerAccount account) {
        if (cents == 0) return;
        if (account.cents > LIMIT - cents) throw new IllegalArgumentException("Player account limit exceeded");
        account.cents += cents;
        cents = 0;
        save();
    }
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Properties data = new Properties();
            data.setProperty("cents", Long.toString(cents));
            Path tmp = Files.createTempFile(file.getParent(), "treasury-", ".tmp");
            try (var writer = Files.newBufferedWriter(tmp)) { data.store(writer, "DeltaCraft Treasury"); }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) { System.err.println("[DeltaCraft] Treasury save: " + ex); }
    }
}
