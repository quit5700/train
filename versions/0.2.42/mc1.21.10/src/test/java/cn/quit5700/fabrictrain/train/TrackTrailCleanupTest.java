package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TrackTrailCleanupTest {
    @Test
    void repeatedlyTrimsBeyondActualTrainHistoryLimit() throws Exception {
        runProbe("long");
    }

    @Test
    void discardsMultipleOldPointsAfterLargeMovement() throws Exception {
        runProbe("jump");
    }

    private void runProbe(String mode) throws Exception {
        String separator = System.getProperty("path.separator");
        String classpath = Path.of(Probe.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + separator + Path.of(TrackTrail.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, Probe.class.getName(), mode).redirectErrorStream(true).start();
        try {
            // A separate process can be killed even when the regression is an uninterruptible loop.
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "轨迹清理超时，可能存在死循环");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(3, TimeUnit.SECONDS), "诊断子进程未结束");
            }
        }
    }

    public static class Probe {
        public static void main(String[] args) {
            if ("long".equals(args[0])) {
                TrackTrail trail = new TrackTrail(140.0D);
                for (int x = 0; x <= 2000; x++) trail.record(x, 64, 0);
                check(trail.pointBehind(140) != null, "上限以内的跟随目标丢失");
                check(Math.abs(trail.pointBehind(140).x() - 1860) < 0.0001D, "上限位置错误");
                check(Math.abs(trail.pointBehind(2.5).x() - 1997.5) < 0.0001D, "车厢插值错误");
                check(trail.pointBehind(140.01D) == null, "过期轨迹未删除");
                for (int x = 1999; x >= 0; x--) trail.record(x, 64, 0);
                check(Math.abs(trail.pointBehind(2.5).x() - 2.5) < 0.0001D, "反向路径错误");
                check(trail.pointBehind(140.01D) == null, "反向运动过期轨迹未删除");
            } else {
                TrackTrail trail = new TrackTrail(2);
                trail.record(0, 64, 0);
                trail.record(1, 64, 0);
                trail.record(2, 64, 0);
                check(trail.pointBehind(2) != null, "恰好到达上限时提前删除");
                trail.record(10, 64, 0);
                check(trail.pointBehind(0).x() == 10, "最新轨迹点丢失");
                check(trail.pointBehind(0.1D) == null, "大跨度旧点未删除");
                trail.record(11, 64, 0);
                trail.record(12, 64, 0);
                check(trail.pointBehind(2).x() == 10, "清理后的新跟随轨迹错误");
                TrackTrail zero = new TrackTrail(0);
                zero.record(0, 0, 0);
                zero.record(1, 0, 0);
                check(zero.pointBehind(0).x() == 1 && zero.pointBehind(0.1D) == null,
                        "零长度上限未保留且仅保留最新点");
            }
        }

        private static void check(boolean result, String message) {
            if (!result) throw new AssertionError(message);
        }
    }
}
