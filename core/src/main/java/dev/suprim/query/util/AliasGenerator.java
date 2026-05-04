package dev.suprim.query.util;

import java.util.UUID;

public class AliasGenerator {
    private static final int UUID_LENGTH = 36;
    private static final int UUID_NUM_CHARS = 12;

    public static String getAlias(String sqlIdentifier) {
        int length = 4;
        if (sqlIdentifier.length() > length) {
            return sqlIdentifier.substring(0, length) + "_" + generateUUID();
        } else {
            return sqlIdentifier + "_" + generateUUID();
        }
    }

    private static String generateUUID() {
        int startIndex = UUID_LENGTH - UUID_NUM_CHARS;
        return UUID.randomUUID().toString().substring(startIndex, UUID_LENGTH);
    }
}
