package org.entando.entando.plugins.jpredis.aps.system.notify;

import com.agiletec.aps.system.EntThreadLocal;
import com.agiletec.aps.system.services.lang.events.LangsChangedEvent;
import io.lettuce.core.internal.LettuceFactories;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(RedisTestExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath*:spring/testpropertyPlaceholder.xml",
        "classpath*:spring/baseSystemConfig.xml",
        "classpath*:spring/aps/**/**.xml",
        "classpath*:spring/plugins/**/aps/**/**.xml",
        "classpath*:spring/web/**.xml"
})
@WebAppConfiguration(value = "")
class RedisNotifyManagerIntegrationTest {

    @BeforeAll
    static void setUp() {
        EntThreadLocal.clear();
        TestEntandoJndiUtils.setupJndi();
    }

    @Autowired
    private RedisNotifyManager redisNotifyManager;

    @Test
    void test() throws Exception {
        testNotifyEvent();
        testNotifyCustomEvent();
    }


    void testNotifyEvent() throws Exception {
        DefaultRedisPubSubListener listener = this.createListener();
        Assertions.assertNotNull(this.redisNotifyManager);
        this.redisNotifyManager.addListener("testchannel1", listener);
        LangsChangedEvent event = new LangsChangedEvent();
        event.setChannel("testchannel1");
        redisNotifyManager.notify(event);
        synchronized (this) {
            wait(1000);
        }
        Assertions.assertEquals(1, listener.getCounts().size());
        Assertions.assertEquals(0, listener.getMessages().size());
    }

    void testNotifyCustomEvent() throws Exception {
        DefaultRedisPubSubListener listener = this.createListener();
        Assertions.assertEquals(0, listener.getMessages().size());
        Assertions.assertEquals(0, listener.getCounts().size());

        Assertions.assertNotNull(this.redisNotifyManager);
        redisNotifyManager.addListener("testchannel2", listener);
        Map<String, String> properties = new HashMap<>();
        properties.put("aaa", "111");
        properties.put("bbb", "222");
        properties.put("ccc", "333");
        TestEvent event = new TestEvent("testchannel2", properties);

        redisNotifyManager.notify(event);
        synchronized (this) {
            wait(2000);
        }
        Assertions.assertEquals(1, listener.getCounts().size());
        Assertions.assertEquals(1, listener.getMessages().size());
        String received = listener.getMessages().take();
        Assertions.assertNotNull(received);
        Map<String, String> extractedProperties = TestEvent.getProperties(received);
        Assertions.assertEquals(3, extractedProperties.size());
        Assertions.assertEquals("111", extractedProperties.get("aaa"));
        Assertions.assertEquals("222", extractedProperties.get("bbb"));
        Assertions.assertEquals("333", extractedProperties.get("ccc"));
    }

    private DefaultRedisPubSubListener createListener() {
        BlockingQueue<String> messages = LettuceFactories.newBlockingQueue();
        BlockingQueue<String> channels = LettuceFactories.newBlockingQueue();
        BlockingQueue<Long> counts = LettuceFactories.newBlockingQueue();
        return new DefaultRedisPubSubListener(messages, channels, counts);
    }

}
