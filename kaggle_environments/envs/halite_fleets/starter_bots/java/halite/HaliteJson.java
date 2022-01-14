package halite;

/**
 * Don't want to import a full blown json library
 * Our json is simple enough to use this
 */
public class HaliteJson {

    public static int getIntFromJson(String raw, String key) {
        return (int)Integer.parseInt(getNumberPartFromJson(raw, key));
    }

    private static String getNumberPartFromJson(String raw, String key) {
        int keyIdx = raw.indexOf(key);
        String rest = raw.substring(keyIdx + key.length() + 3);
        int end = rest.indexOf(",") > 0 ? rest.indexOf(",") : rest.indexOf("}");
        return rest.substring(0, end);
    }

    public static int getPlayerIdxFromJson(String raw) {
        String key = "'player': ";
        int keyIdx = raw.indexOf(key);
        String rest = raw.substring(keyIdx + key.length());
        int end = rest.indexOf(",") > 0 ? rest.indexOf(",") : rest.indexOf("}");
        return (int)Integer.parseInt(rest.substring(0, end));
    }

    public static String getStrFromJson(String raw, String key) {
        int keyIdx = raw.indexOf(key);
        String rest = raw.substring(keyIdx + key.length() + 4);
        int end = rest.indexOf(",") > 0 ? rest.indexOf(",") : rest.indexOf("}");
        return rest.substring(0, end - 1);
    }

    public static float getFloatFromJson(String raw, String key) {
        String val = getNumberPartFromJson(raw, key);
        return (float)Float.parseFloat(val);
    }

    private static String getStrArrStrFromJson(String raw, String key) {
        int keyIdx = raw.indexOf(key);
        String rest = raw.substring(keyIdx + key.length() + 4);
        int end = rest.indexOf("],") > 0 ? rest.indexOf("],") : rest.indexOf("]}");
        return rest.substring(0, end);
    }

    public static String[] getPlayerPartsFromJson(String raw) {
        String key = "players";
        int keyIdx = raw.indexOf(key);
        String rest = raw.substring(keyIdx + key.length() + 5);
        int end = rest.indexOf("]],") > 0 ? rest.indexOf("]],") : rest.indexOf("]]}");
        return rest.substring(0, end).split("], \\[");
    }

    public static String[] getStrArrFromJson(String raw, String key) {
        String arrStr = getStrArrStrFromJson(raw, key);
        return arrStr.split(", ");
    }

    public static int[] getIntArrFromJson(String raw, String key) {
        String[] arrStrParts = getStrArrStrFromJson(raw, key).split(", ");
        int[] intArr = new int[arrStrParts.length];
        for (int i = 0; i < arrStrParts[i].length(); i++) {
            intArr[i] = Integer.parseInt(arrStrParts[i]);
        }
        return intArr;
    }

    public static float[] getFloatArrFromJson(String raw, String key) {
        String[] arrStrParts = getStrArrStrFromJson(raw, key).split(", ");
        float[] floatArr = new float[arrStrParts.length];
        for (int i = 0; i < arrStrParts[i].length(); i++) {
            floatArr[i] = Float.parseFloat(arrStrParts[i]);
        }
        return floatArr;
    }
    
    public static boolean containsKey(String raw, String key) {
        return raw.indexOf(key) > -1;
    }
}
