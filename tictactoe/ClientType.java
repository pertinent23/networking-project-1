public enum ClientType {
    X("X"),
    O("O");

    private String data;

    private ClientType(String symbol) {
        this.data = symbol;
    }

    @Override
    public String toString() {
        return this.data.toUpperCase();
    }

    /**
     * Create a ClientType from a string
     * if the string is not valid return null
     * @param str
     * @return
    */
    public static ClientType fromString(String str) {
        if (str.equalsIgnoreCase("X")) {
            return ClientType.X;
        } else if (str.equalsIgnoreCase("O")) {
            return ClientType.O;
        } else {
            return null;
        }
    }
}
