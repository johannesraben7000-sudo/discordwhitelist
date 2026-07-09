package com.example.discordwhitelist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Small helper for validating Minecraft usernames against Mojang's API.
 * This confirms the name belongs to a real account and gives us the
 * correctly-cased username plus UUID (Mojang usernames are case-insensitive
 * but the whitelist looks nicer with correct casing, and looking players up
 * by UUID avoids a second blocking network call later).
 */
public class MojangAPI {

    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    public record LookupResult(boolean validFormat, boolean exists, String correctedName, UUID uuid) {}

    public static LookupResult lookup(String rawInput) {
        String input = rawInput.trim();

        if (!VALID_NAME.matcher(input).matches()) {
            return new LookupResult(false, false, null, null);
        }

        try {
            URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + input);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();

            if (status == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    String correctedName = obj.get("name").getAsString();
                    UUID uuid = parseMojangUuid(obj.get("id").getAsString());
                    return new LookupResult(true, true, correctedName, uuid);
                }
            } else if (status == 404) {
                return new LookupResult(true, false, null, null);
            } else {
                // Rate limited or Mojang API hiccup - treat as unknown, caller should
                // fall back gracefully rather than wrongly rejecting a real account.
                return new LookupResult(true, false, null, null);
            }
        } catch (IOException e) {
            return new LookupResult(true, false, null, null);
        }
    }

    /** Mojang returns UUIDs without dashes (e.g. "069a79f4..."); UUID.fromString needs dashes. */
    private static UUID parseMojangUuid(String undashed) {
        String dashed = undashed.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(dashed);
    }

    public static Optional<String> quickFormatCheck(String rawInput) {
        String input = rawInput.trim();
        if (VALID_NAME.matcher(input).matches()) {
            return Optional.of(input);
        }
        return Optional.empty();
    }
}
