import com.diogonunes.jcolor.AnsiFormat;
import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.*;

public class logMe {

    //Every log called from the main class will have the [ main ] header.
    public static String mainClass = "mainClassName";
    //Every log called from the api class will have the [ API ] header.
    public static String apiClass = "apiClassName";

    public static enum LOG_LEVEL {
        MAIN("[ main  ] ", new AnsiFormat(MAGENTA_TEXT(), BACK_COLOR(234))),
        DEBUG("[ debug ] ", new AnsiFormat(GREEN_TEXT(), BACK_COLOR(234))),
        INFO("[ info  ] ", new AnsiFormat(CYAN_TEXT(), BACK_COLOR(234))),
        WARN("[ warn  ] ", new AnsiFormat(YELLOW_TEXT(), BACK_COLOR(234))),
        ERROR("[ error ] ", new AnsiFormat(RED_TEXT(), BACK_COLOR(234))),
        API("[ API   ] ", new AnsiFormat(BLUE_TEXT(), BACK_COLOR(234)));

        private final header h;

        private LOG_LEVEL(String header, AnsiFormat format) {
            h = new header(header, format);
        }

        public header header() {
            return h;
        }
    }

    final static private int headerLength = "[ debug ] ".length();
    final static private String intendationString = " ".repeat(headerLength);

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
            System.out.print("\r");
        } else if (!newLine) {
            if (lineRepetitions != 0)
                System.out.print("\r");
            else
                System.out.println();
            lineRepetitions++;
        } else {
            System.out.println();
            lineRepetitions = 0;
        }

        // print
        System.out.print(intendationString.repeat(intendation));// intendaton
        h.print();// header
        System.out.print(wStr);// str, intendation was previously added(see: format intendation)

        // clean if last log was \r
        if (lastLineLength > wStr.length()) {
            System.out.print(" ".repeat(lastLineLength - wStr.length()));
            lastLineLength = 0;
        }

        // make future cleaning possible
        if (!newLine)
            lastLineLength = wStr.length();

        // repetition-label
        if (lineRepetitions > 0 && newLine) {
            System.out.print(intendationString + "x" + (lineRepetitions + 1));
        }

        lastString = str;
    }

    public static void log(String str) {
        header header = LOG_LEVEL.INFO.header();
        if (calledBy(mainClass))
            header = LOG_LEVEL.MAIN.header();
        if (calledBy(apiClass))
            header = LOG_LEVEL.API.header();
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

    private static boolean calledBy(String className) {
        StackTraceElement[] steA = Thread.currentThread().getStackTrace();
        if (steA.length < 5)
            return true;
        var ste = steA[4];
        String callerClass = ste.getClassName();
        return callerClass.startsWith(className);
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
                System.out.print(s);
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
                // System.out.println(wStr + ", " + newLineIndex + ", " + nextIndex);
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
        private AnsiFormat format;

        public header(String header, AnsiFormat format) {
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

        public AnsiFormat getFormat() {
            return format;
        }

        public void print() {
            System.out.print(colorize(header, format));
        }

        public void println() {
            System.out.println(colorize(header, format));
        }
    }
}
