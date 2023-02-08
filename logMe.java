package logme;
import static logme.colors.*;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class logMe {
    final static private int headerLength = "[ debug ] ".length();
    final static private String intendationString = " ".repeat(headerLength);

    private static PrintStream out = System.out;

    public static void setOut(PrintStream out) {
        logMe.out = out;
    }

    private static Map<Class<?>, header> autoHeader = new HashMap<>();

    public static enum LOG_LEVEL {
        MAIN("[ main  ] ", PURPLE),
        DEBUG("[ debug ] ", GREEN),
        INFO("[ info  ] ", CYAN),
        WARN("[ warn  ] ", YELLOW),
        ERROR("[ error ] ", RED),
        API("[ API   ] ", BLUE);

        private final header h;

        private LOG_LEVEL(String header, String... format) {
            h = new header(header, format);
        }

        public header header() {
            return h;
        }
    }

    private static header LOGGER = new header("[LOGGER ] ", WHITE);

    private static int lastLineLength = 0; // only set and important when \r is used
    private static String lastString = "";
    private static int lineRepetitions = 0;
    static private int intendation = 0;

    public static synchronized void log(String str, int intendation, String intendationString, header h,
            boolean newLine) {
        String wStr = str + "";
        // consoleWidth wrapping to allow proper intendation
        wStr = adaptForScreenWidth(wStr);
        // format intendation
        wStr = wStr.replace("\n", "\n " + " ".repeat(headerLength) + intendationString.repeat(intendation));

        // check for repetition
        if (lastString.equals(wStr) && newLine) {
            lineRepetitions++;
            out.print("\r");
        } else if (!newLine) {
            if (lineRepetitions != 0)
                out.print("\r");
            else
                out.println();
            lineRepetitions++;
        } else {
            out.println();
            lineRepetitions = 0;
        }

        // print
        out.print(intendationString.repeat(intendation));// intendaton
        h.print();// header
        out.print(wStr);// str, intendation was previously added(see: format intendation)

        // clean if last log was \r
        if (lastLineLength > wStr.length()) {
            out.print(" ".repeat(lastLineLength - wStr.length()));
            lastLineLength = 0;
        }

        // make future cleaning possible
        if (!newLine)
            lastLineLength = wStr.length();

        // repetition-label
        if (lineRepetitions > 0 && newLine) {
            out.print(intendationString + "x" + (lineRepetitions + 1));
        }

        lastString = str;
    }

    public static void log(String str) {
        header header = autoHeader();
        log(str, intendation, intendationString, header, true);
    }

    public static void log(Object obj) {
        log(obj.toString());
    }

    public static void debug(String str) {
        log(str, intendation, intendationString, LOG_LEVEL.DEBUG.header(), true);
    }

    public static void error(String str) {
        log(str, intendation, intendationString, LOG_LEVEL.ERROR.header(), true);
    }

    public static void error(Object obj) {
        error(obj.toString());
    }

    public static void log(String str, LOG_LEVEL level) {
        log(str, intendation, intendationString, level.header(), true);
    }

    public static void sameLineLog(String str) {
        log(str, intendation, intendationString, LOG_LEVEL.INFO.header(), false);
    }

    public static void logWithMethod(String str) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        String callerClass = ste.getClassName();
        String callerMethod = ste.getMethodName();
        String output = String.format("[%s, %s]: %s%n", callerClass, callerMethod, str);
        log(output, LOG_LEVEL.DEBUG);
    }

    public static void increaseIndentation() {
        intendation++;
    }

    public static void decreaseIndentation() {
        intendation--;
    }

    public static void setIntendation(int intendation) {
        logMe.intendation = intendation;
    }

    private static header autoHeader() {
        StackTraceElement[] steA = Thread.currentThread().getStackTrace();
        if (steA.length < 5)
            return LOGGER;
        var ste = steA[4];
        header res = autoHeader.get(ste.getClass());
        if(res == null)
            return LOG_LEVEL.INFO.header();
        return res;
    }

    private static int consoleWidth() {
        try {
            String[] signals = new String[] {
                    "\u001b[s", // save cursor position
                    "\u001b[5000;5000H", // move to col 5000 row 5000
                    "\u001b[6n", // request cursor position
                    "\u001b[u", // restore cursor position
            };
            for (String s : signals) {
                out.print(s);
            }
            if (System.in.available() == 0)
                return -1;

            int read = -1;
            StringBuilder sb = new StringBuilder();
            byte[] buff = new byte[1];
            while ((read = System.in.read(buff, 0, 1)) != -1) {
                sb.append((char) buff[0]);
                // System.err.printf("Read %s chars, buf size=%s%n", read, sb.length());
                if ('R' == buff[0]) {
                    break;
                }
            }
            String size = sb.toString();
            int rows = Integer.parseInt(size.substring(size.indexOf("\u001b[") + 2, size.indexOf(';')));
            int cols = Integer.parseInt(size.substring(size.indexOf(';') + 1, size.indexOf('R')));
            System.err.printf("rows = %s, cols = %s%n", rows, cols);
            return cols;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static String adaptForScreenWidth(String str) {
        String wStr = str + "";
        int width = consoleWidth();
        if (width != -1) {
            int newLineIndex = -1;
            int nextIndex = 0;
            newLineLoop: while ((newLineIndex = nextIndex) != -1) {
                nextIndex = wStr.indexOf("\n", newLineIndex + 1);
                int j = newLineIndex;
                // out.println(wStr + ", " + newLineIndex + ", " + nextIndex);
                while (j + 1 < ((nextIndex == -1) ? wStr.length() : nextIndex)) {
                    j++;
                    if (wStr.charAt(j) == '\n') {
                        continue newLineLoop;
                    }
                    if (j - newLineIndex > width && j + 1 != wStr.length()) {
                        wStr = wStr.substring(0, j) + "\n" + wStr.substring(j);
                        newLineIndex = j + 1;
                        j++;
                    }
                }
            }
        }
        return wStr;
    }

    private static class header {
        private String header;
        private String[] format;

        public header(String header, String... format) {
            String h = header + "";
            this.format = format;

            // too long header
            if (h.length() > headerLength) {
                h.substring(0, headerLength);
                // too short --> if possible add [ ]
            } else if (h.length() + 2 > headerLength) {
                if (!h.startsWith("["))
                    h = "[" + header;
                if (!h.strip().endsWith("]"))
                    h = header.strip() + "]";
                // too short --> add spaces
            }

            h = h + " ".repeat(headerLength - h.length());

            this.header = h;
        }

        public String getHeader() {
            return header;
        }

        public String[] getFormat() {
            return format;
        }

        public void print() {
            out.print(colorize(header, format));
        }
    }

    public static String colorize(String str, String[] format) {
        String res = "";
        for (String f : format) {
            res += f;
        }
        return res + str + RESET;
    }

    public static void demo() {
        log("test");
    }

    public static void main(String[] args) {
        demo();
    }
}
