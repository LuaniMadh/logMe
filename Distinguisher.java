import java.io.File;
import java.lang.reflect.Method;
public class Distinguisher {
    private enum type {
        FILE,
        CLASS,
        METHOD
    }

    private final type t;
    private final File p;
    private final Class<?> c;
    private final Method m;

    private boolean strict = false;

    public Distinguisher(File p, boolean strict) {
        this.strict = strict;
        this.t = type.FILE;
        this.p = p;
        this.c = null;
        this.m = null;
    }

    public Distinguisher(Class<?> c, boolean strict) {
        this.strict = strict;
        this.t = type.CLASS;
        this.p = null;
        this.c = c;
        this.m = null;
    }

    public Distinguisher(Method m, boolean strict) {
        this.strict = strict;
        this.t = type.METHOD;
        this.p = null;
        this.c = null;
        this.m = m;
    }

    public Distinguisher(Class<?> c) {
        this.t = type.CLASS;
        this.p = null;
        this.c = c;
        this.m = null;
    }

    public Distinguisher(Method m) {
        this.t = type.METHOD;
        this.p = null;
        this.c = null;
        this.m = m;
    }

    type getType() {
        return t;
    }

    boolean appliesTo(StackTraceElement ste) {
        return switch (t) {
            case FILE ->{
                if (strict)
                    yield ste.getFileName().equals(p.getName());
                else
                    yield ste.getFileName().startsWith(p.getName());
            }
            case CLASS ->{
                if (strict)
                    yield ste.getClassName().startsWith(c.getName());
                else
                    yield ste.getClassName().contains(c.getName());
            }
            case METHOD ->{
                if (strict)
                    yield ste.getMethodName().startsWith(m.getName());
                else
                    yield ste.getMethodName().contains(m.getName());
            }
            default -> false;
        };
    }
}