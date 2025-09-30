import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public class NativeImageWrapper {
    @CEntryPoint(name = "noop")
    public static void noop(IsolateThread isolate) {
        MyObject.INSTANCE.noop();
    }
}
