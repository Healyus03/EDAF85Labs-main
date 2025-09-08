package actor.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import actor.ActorThread;
import util.test.DisplayName;
import util.test.Order;
import util.test.Test;
import util.test.TestSuite;

class ActorThreadTest extends TestSuite {

    @Test
    @Order(1)
    @DisplayName("ExampleBidirectional")
    void testExample1() throws InterruptedException {
        runMainMethod(ExampleBidirectional::main,
                      "ClientThread sending request\n" + 
                              "request received by FibonacciThread\n" + 
                              "received result fib(14) = 377\n" + 
                              "FibonacciThread terminated\n");
    }

    @Test
    @Order(2)
    @DisplayName("ExampleProducerConsumer")
    void testExample2() throws InterruptedException {
        runMainMethod(ExampleProducerConsumer::main,
                      "consumer eagerly awaiting messages...\n" + 
                              "received [ole]\n" + 
                              "received [dole]\n" + 
                              "received [doff]\n" + 
                              "all done\n");
    }

    @Test
    @Order(3)
    @DisplayName("ExampleMessagingWithTimeout")
    void testExample3() throws InterruptedException {
        runMainMethod(ExampleMessagingWithTimeout::main,
                      "consumer eagerly awaiting messages...\n" + 
                              "received [ole]\n" + 
                              "received [dole]\n" + 
                              "received [null]\n" + 
                              "received [doff]\n" + 
                              "all done\n");
    }

    @Test
    @Order(4)
    @DisplayName("ExampleReceiveWithTimeoutKeepsMessagesInOrder")
    void testExample4() throws InterruptedException {
        runMainMethod(ExampleReceiveWithTimeoutKeepsMessagesInOrder::main,
                      "consumer eagerly awaiting messages...\n" +
                              "received [yxi]\n" + 
                              "received [kaxi]\n" + 
                              "received [kolme]\n" + 
                              "received [null]\n" + 
                              "all done\n");
    }

    /**
     * Verify that ActorThread has one single attribute
     * of type BlockingQueue (or a type implementing BlockingQueue),
     * and that the chosen queue type does not have a capacity limit
     */
    @Test
    @Order(5)
    @DisplayName("ActorThread should use an appropriate BlockingQueue")
    void testShouldUseBlockingQueue() throws IllegalAccessException {
        Field[] attributes = ActorThread.class.getDeclaredFields();
        assertEquals(1, attributes.length, "expected one single attribute (BlockingQueue)");

        if (attributes.length == 1) {
            Field queueAttribute = attributes[0];
            Class<?> attributeType = queueAttribute.getType();
            assertTrue(BlockingQueue.class.isAssignableFrom(attributeType), "expected BlockingQueue attribute");

            // inspect the actual attribute value
            ActorThread<?> t = new ActorThread<>() { /* concrete subclass to abstract superclass */ };
            queueAttribute.setAccessible(true);
            Object value = queueAttribute.get(t);
            assertFalse(value instanceof ArrayBlockingQueue, "ArrayBlockingQueue has a limited capacity: can lead to deadlock when used for message queues. Consider LinkedBlockingQueue");
            assertFalse(value instanceof DelayQueue, "DelayQueue introduces additional delays: inappropriate for message queues. Consider LinkedBlockingQueue");
            assertFalse(value instanceof PriorityBlockingQueue, "PriorityBlockingQueue reorders elements: inappropriate for message queues. Consider LinkedBlockingQueue");
            assertFalse(value instanceof SynchronousQueue, "SynchronousQueue has a limited capacity: can lead to deadlock when used for message queues. Consider LinkedBlockingQueue");
        }
    }

    /**
     * Verify that ActorThread methods are not synchronized
     * (they shouldn't be, as BlockingQueues are thread-safe)
     */
    @Test
    @Order(6)
    @DisplayName("ActorThread should not use 'synchronized'")
    void testShouldNotUseSynchronized() {
        Method[] methods = ActorThread.class.getDeclaredMethods();
        assertEquals(3, methods.length, "expected three methods: send, receive, receiveWithTimeout");

        for (Method m : methods) {
            assertFalse(Modifier.isSynchronized(m.getModifiers()), "method " + m.getName() + " is synchronized, but shouldn't be");
        }
    }

    /**
     * Verify that ActorThread method send() is not declared
     * 'throws InterruptedException'
     */
    @Test
    @Order(7)
    @DisplayName("send() should not be declared 'throws'")
    void testSendShouldNotBeDeclaredThrows() {
        for (Method m : ActorThread.class.getDeclaredMethods()) {
            if ("send".equals(m.getName())) {
                Type[] exceptionsThrown = m.getExceptionTypes();
                if (exceptionsThrown.length != 0) {
                    String[] exceptions = Stream.of(exceptionsThrown)
                            .map(t -> {
                                String n = t.getTypeName();
                                int dot = n.lastIndexOf('.');
                                return (dot >= 0) ? n.substring(dot + 1) : n;
                            }).toArray(String[]::new);
                    String typeString = String.join(", ", exceptions);

                    System.err.println("** ERROR:");
                    System.err.println("**");
                    System.err.println("** Method 'send()' was declared 'throws " + typeString + "'.");
                    System.err.println("** This method should not throw any exceptions.\n");
                    System.err.println("** HINT:");
                    System.err.println("**");
                    System.err.println("** Don't use the BlockingQueue method put(). Use add() or offer() instead.");
                    System.err.println("** Then remove 'throws " + typeString + "' from method 'send'.");
                }
                assertEquals(0, exceptionsThrown.length, "method 'send' should not throw any exceptions");
                return;
            }
        }
        fail("no method 'send' found in ActorThread");
    }

    /**
     * Verify that ActorThread methods receive() and receiveWithTimeout()
     * are declared 'throws InterruptedException'
     */
    @Test
    @Order(8)
    @DisplayName("receive() should be declared 'throws InterruptedException'")
    void testReceiveShouldBeDeclaredThrows() {
        int k = 0;
        for (Method m : ActorThread.class.getDeclaredMethods()) {
            String name = m.getName();
            if ("receive".equals(name) || "receiveWithTimeout".equals(name)) {
                Type[] exceptionsThrown = m.getExceptionTypes();
                Set<String> typeNames = Stream.of(exceptionsThrown).map(Type::getTypeName).collect(Collectors.toSet());
                boolean throwsInterruptedException = typeNames.contains(InterruptedException.class.getTypeName());
                assertTrue(throwsInterruptedException, "method '" + name + "' should be declared 'throws InterruptedException'");
                k++;
            }
        }

        assertEquals(2, k, "missing method 'receive' and/or 'receiveWithTimeout'");
    }

    @Test
    @Order(9)
    @DisplayName("receive() should block")
    void testReceiveShouldBlock() throws InterruptedException {
        AtomicBoolean interruptionHandledCorrectly = new AtomicBoolean(false);
        ActorThread<?> blocker = new ActorThread<>() {
            @Override
            public void run() {
                try {
                    receive();
                } catch (InterruptedException e) {
                    // interruption expected: check
                    // InterruptedException handled correctly
                    interruptionHandledCorrectly.set(true);
                } catch (Throwable unexpected) {
                    fail("unexpected exception", unexpected);
                }
            }
        };
        blocker.start();
        assertThreadState(blocker, Thread.State.WAITING);
        blocker.interrupt();
        assertThreadState(blocker, Thread.State.TERMINATED);

        assertTrue(interruptionHandledCorrectly.get(), "receive should not catch InterruptedException");
    }

    @Test
    @Order(10)
    @DisplayName("receiveWithTimeout() should block")
    void testReceiveWithTimeoutShouldBlock() throws InterruptedException {
        AtomicBoolean interruptionHandledCorrectly = new AtomicBoolean(false);
        ActorThread<?> blocker = new ActorThread<>() {
            @Override
            public void run() {
                try {
                    receiveWithTimeout(60 * 60 * 1000); // one hour
                } catch (InterruptedException e) {
                    // interruption expected: check
                    // InterruptedException handled correctly
                    interruptionHandledCorrectly.set(true);
                } catch (Throwable unexpected) {
                    fail("unexpected exception", unexpected);
                }
            }
        };
        blocker.start();
        assertThreadState(blocker, Thread.State.TIMED_WAITING);
        blocker.interrupt();
        assertThreadState(blocker, Thread.State.TERMINATED);

        assertTrue(interruptionHandledCorrectly.get(), "receiveWithTimeout must not catch InterruptedException");
    }

    @Test
    @Order(11)
    @DisplayName("receiveWithTimeout() should time out when no message arrives")
    void testDelaysInReceiveWithTimeout() throws InterruptedException {
        List<Long> measurements = new ArrayList<>();
        ActorThread<?> delayed = new ActorThread<>() {
            @Override
            public void run() {
                try {
                    long t0 = System.currentTimeMillis();
                    receiveWithTimeout(300);
                    long t1 = System.currentTimeMillis();
                    receiveWithTimeout(100);
                    long t2 = System.currentTimeMillis();
                    receiveWithTimeout(200);
                    long t3 = System.currentTimeMillis();
                    measurements.add(t1 - t0);
                    measurements.add(t2 - t1);
                    measurements.add(t3 - t2);
                } catch (Throwable unexpected) {
                    fail("unexpected exception", unexpected);
                }
            }
        };
        delayed.start();
        delayed.join();

        assertEquals(3, measurements.size(), "timeout value lost??");

        // allow 50ms additional delay: huge overkill
        assertTrue(measurements.get(0) >= 300, "receiveWithTimeout(300) timed out after " + measurements.get(0) + " ms");
        assertTrue(measurements.get(0) < 350, "receiveWithTimeout(300) timed out after " + measurements.get(0) + " ms");
        assertTrue(measurements.get(1) >= 100, "receiveWithTimeout(100) timed out after " + measurements.get(1) + " ms");
        assertTrue(measurements.get(1) < 150, "receiveWithTimeout(100) timed out after " + measurements.get(1) + " ms");
        assertTrue(measurements.get(2) >= 200, "receiveWithTimeout(200) timed out after " + measurements.get(2) + " ms");
        assertTrue(measurements.get(2) < 250, "receiveWithTimeout(200) timed out after " + measurements.get(2) + " ms");
    }

    @Test
    @Order(12)
    @DisplayName("receiveWithTimeout() can receive a message while waiting")
    void testReceiveWithTimeoutWithLateMessage() throws InterruptedException {
        AtomicReference<String> slot = new AtomicReference<>();
        ActorThread<String> delayed = new ActorThread<>() {
            @Override
            public void run() {
                try {
                    String s = receiveWithTimeout(1000);
                    slot.set(s);
                } catch (Throwable unexpected) {
                    fail("unexpected exception", unexpected);
                }
            }
        };
        delayed.start();
        assertThreadState(delayed, Thread.State.TIMED_WAITING);
        long t0 = System.currentTimeMillis();
        delayed.send("hello");
        delayed.join();
        long dt = System.currentTimeMillis() - t0;

        assertEquals("hello", slot.get(), "message lost");

        // allow 50ms additional delay: huge overkill
        assertTrue(dt < 50, "receiveWithTimeout(1000) took " + dt + " ms to receive message");
    }

    @Test
    @Order(13)
    @DisplayName("attribute should be final")
    void testAttributeShouldBeFinal() {
        Field[] attributes = ActorThread.class.getDeclaredFields();

        // previous tests check we have exactly one appropriate attribute
        if (attributes.length >= 1) {
            Field queueAttribute = attributes[0];
            int mod = queueAttribute.getModifiers();
            assertTrue(Modifier.isFinal(mod), "reference attribute should be final");
        }
    }

    @Test
    @Order(14)
    @DisplayName("attribute should be private")
    void testAttributeShouldBePrivate() {
        Field[] attributes = ActorThread.class.getDeclaredFields();

        // previous tests check we have exactly one appropriate attribute
        if (attributes.length >= 1) {
            Field queueAttribute = attributes[0];
            int mod = queueAttribute.getModifiers();
            assertTrue(Modifier.isPrivate(mod), "queue attribute should be private");
        }
    }
    
    public static void main(String[] args) {
        new ActorThreadTest().run();
    }
}
