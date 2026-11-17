package pl.deltacraft.shop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Plain properties persistence, one account per UUID; atomic replace on save. */
public final class Accounts {
    private final Map<UUID, PlayerAccount> accounts = new HashMap<>();
    private final Path directory;

    public Accounts(Path directory) { this.directory = directory; }

    public PlayerAccount get(UUID id) { return accounts.computeIfAbsent(id, this::load); }

    private PlayerAccount load(UUID id) {
        PlayerAccount account = new PlayerAccount();
        Path path = directory.resolve(id + ".properties");
        if (!Files.exists(path)) return account;
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(reader);
            account.cents = Math.max(0, Long.parseLong(p.getProperty("cents", "0")));
            account.level = Math.max(0, Math.min(500, Integer.parseInt(p.getProperty("level", "0"))));
            account.xp = Math.max(0, Long.parseLong(p.getProperty("xp", "0")));
        } catch (Exception ex) {
            System.err.println("[DeltaCraft Shop] Could not read account " + id + ": " + ex);
        }
        return account;
    }

    public void saveAll() {
        try { Files.createDirectories(directory); }
        catch (IOException e) { System.err.println("[DeltaCraft Shop] Cannot create account directory: " + e); return; }
        for (var entry : accounts.entrySet()) {
            Properties p = new Properties();
            p.setProperty("cents", Long.toString(entry.getValue().cents));
            p.setProperty("level", Integer.toString(entry.getValue().level));
            p.setProperty("xp", Long.toString(entry.getValue().xp));
            Path dest = directory.resolve(entry.getKey() + ".properties");
            try {
                Path temp = Files.createTempFile(directory, "account-", ".tmp");
                try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { p.store(writer, "DeltaCraft Shop"); }
                try { Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ex) { Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING); }
            } catch (IOException e) { System.err.println("[DeltaCraft Shop] Cannot save " + entry.getKey() + ": " + e); }
        }
    }
}
