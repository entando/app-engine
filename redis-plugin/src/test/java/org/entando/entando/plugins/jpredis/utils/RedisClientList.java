package org.entando.entando.plugins.jpredis.utils;

import io.lettuce.core.KillArgs;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;

/**
 * Reads and kills the connections listed by {@code CLIENT LIST} of the server behind the given commands.
 */
public final class RedisClientList {

    private static final Pattern ID = Pattern.compile("^id=(\\d+)");
    private static final Pattern FLAGS = Pattern.compile(" flags=(\\S*)");

    private RedisClientList() {
    }

    public static List<Long> ids(RedisCommands<String, String> commands) {
        return lines(commands).map(RedisClientList::id).toList();
    }

    public static Set<Long> trackingIds(RedisCommands<String, String> commands) {
        return lines(commands).filter(line -> flags(line).contains("t")).map(RedisClientList::id)
                .collect(Collectors.toSet());
    }

    /**
     * Runs the action and returns the id of the only connection with client tracking enabled that it opened.
     */
    public static long openedTrackingConnection(RedisCommands<String, String> commands, Runnable action) {
        Set<Long> before = trackingIds(commands);
        action.run();
        Set<Long> opened = new HashSet<>(trackingIds(commands));
        opened.removeAll(before);
        Assertions.assertEquals(1, opened.size(), () -> "tracking connections opened, in\n" + commands.clientList());
        return opened.iterator().next();
    }

    /**
     * Kills the given connection and waits until it has been replaced by a new connection.
     */
    public static void killAndAwaitReconnect(RedisCommands<String, String> commands, long id, long timeoutMillis)
            throws InterruptedException {
        int clients = ids(commands).size();

        Assertions.assertEquals(1L, commands.clientKill(KillArgs.Builder.id(id)));

        long deadline = System.currentTimeMillis() + timeoutMillis;
        List<Long> current = ids(commands);
        while (current.contains(id) || current.size() < clients) {
            if (System.currentTimeMillis() > deadline) {
                Assertions.fail("Timed out waiting for the reconnection of client " + id);
            }
            Thread.sleep(50);
            current = ids(commands);
        }
    }

    private static Stream<String> lines(RedisCommands<String, String> commands) {
        return commands.clientList().lines().filter(line -> !line.isBlank());
    }

    private static long id(String line) {
        Matcher matcher = ID.matcher(line);
        Assertions.assertTrue(matcher.find(), line);
        return Long.parseLong(matcher.group(1));
    }

    private static String flags(String line) {
        Matcher matcher = FLAGS.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
