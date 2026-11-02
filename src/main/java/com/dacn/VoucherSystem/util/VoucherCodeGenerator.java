package com.dacn.VoucherSystem.util;

import com.dacn.VoucherSystem.exception.InvalidBatchConfigException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

@Component
public class VoucherCodeGenerator {

    public static final Set<String> SUPPORTED_CHARSETS = Set.of("ALPHABETIC", "ALPHANUMERIC");

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ROUNDS = 20;

    public Set<String> generate(String prefix, int length, String charset, int count,
                                Function<Collection<String>, Set<String>> findExisting) {

        String alphabet = resolveAlphabet(charset);

        // The number of combinations must be at least twice the number of codes to be generated;
        // otherwise, repeated codes will occur constantly.
        double capacity = Math.pow(alphabet.length(), length);
        if (capacity < count * 2.0) {
            throw new InvalidBatchConfigException(
                    "codeConfig.length is too short to generate " + count + " unique codes");
        }

        Set<String> result = new LinkedHashSet<>(count * 2);
        int round = 0;

        while (result.size() < count) {
            if (++round > MAX_ROUNDS) {
                throw new IllegalStateException("Could not generate enough unique codes");
            }
            int need = count - result.size();

            Set<String> candidates = new HashSet<>();
            while (candidates.size() < need) {
                candidates.add(prefix + randomPart(alphabet, length));
            }
            candidates.removeAll(result);                         // Matches the code generated during this instance
            candidates.removeAll(findExisting.apply(candidates)); // Matches a code already present in the DB

            result.addAll(candidates);
        }
        return result;
    }

    private String randomPart(String alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private String resolveAlphabet(String charset) {
        return switch (charset) {
            case "ALPHABETIC" -> "ABCDEFGHJKLMNPQRSTUVWXYZ";
            case "ALPHANUMERIC" -> "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            default -> throw new InvalidBatchConfigException("codeConfig.charset is not supported");
        };
    }
}