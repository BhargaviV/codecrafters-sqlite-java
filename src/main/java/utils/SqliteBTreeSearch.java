package utils;

import java.util.List;

public class SqliteBTreeSearch {

    // A helper method to handle the strict SQLite collation hierarchy
    public static int compareSqliteValues(Object cellValue, Object searchKey) {
        if (cellValue instanceof String strKey) {
            if (strKey.startsWith("'") && strKey.endsWith("'")) {
                searchKey = strKey.substring(1, strKey.length() - 1).trim();
            }
        }
        if (cellValue == searchKey) return 0;

        // 1. Handle SQLite NULL / empty string placeholders (always lowest)
        boolean cellIsNull = (cellValue == null || "".equals(cellValue));
        boolean keyIsNull = (searchKey == null || "".equals(searchKey));

        if (cellIsNull && !keyIsNull) return -1;
        if (!cellIsNull && keyIsNull) return 1;
        if (cellIsNull && keyIsNull) return 0;

        // 2. Handle numeric types vs strings (numbers are always lowest)
        boolean cellIsNum = cellValue instanceof Number;
        boolean keyIsNum = searchKey instanceof Number;

        if (cellIsNum && !keyIsNum) return -1;
        if (!cellIsNum && keyIsNum) return 1;

        if (cellIsNum && keyIsNum) {
            return Long.compare(((Number) cellValue).longValue(), ((Number) searchKey).longValue());
        } else {
            // 3. String alphabetical comparison
            return cellValue.toString().trim().compareTo(searchKey.toString().trim());
        }
    }


    public static int findLessThanOrEqualTo(List<Object> keys, Object searchKey) {
        int left = 0;
        int right = keys.size() - 1;
        int ans = -1; // Default to -1 if every key on the page is greater than the target

        if (searchKey instanceof String) {
            String strKey = (String) searchKey;
            if (strKey.startsWith("'") && strKey.endsWith("'")) {
                searchKey = strKey.substring(1, strKey.length() - 1).trim();
            }
        }

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Object midKey = keys.get(mid);

            int cmp = compareSqliteValues(midKey, searchKey);
//            System.err.println("----------------------------------------");
//            System.err.println("Mid Key Class: " + (midKey == null ? "null" : midKey.getClass().getName()));
//            System.err.println("Mid Key Value: [" + midKey + "]");
//            System.err.println("Target Value : [" + searchKey + "]");
//            System.err.println("Cmp Result   : " + cmp);

            if (cmp <= 0) {
                // midKey is LESS THAN or EQUAL to searchKey (e.g., "australia" vs "micronesia")
                // This is a valid candidate! Record it and look for an even closer value to the right.
                ans = mid;
                left = mid + 1;
            } else {
                // midKey is GREATER THAN searchKey (e.g., "united states" vs "micronesia")
                // This key is too big. We must search left.
                right = mid - 1;
            }
        }
        return ans;
    }
}
