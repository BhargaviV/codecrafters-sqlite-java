package pagereader;

public enum PageType {

    TABLE_LEAF,
    TABLE_INTERNAL,
    INDEX_LEAF,
    INDEX_INTERNAL;

    public static PageType getPageType(int value) {
        switch(value) {
            case 0x05 -> {
                return TABLE_INTERNAL;
            }
            case 0x0D -> {
                return TABLE_LEAF;
            }
        }
        System.err.println("getPageType" + value);
        return null;
    }
}
