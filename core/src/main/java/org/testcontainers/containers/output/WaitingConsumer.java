package org.testcontainers.containers.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * A consumer for container output that buffers lines in a {@link java.util.concurrent.BlockingDeque} and enables tests
 * to wait for a matching condition.
 */
public class WaitingConsumer extends BaseConsumer<WaitingConsumer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingConsumer.class);

    private LinkedBlockingDeque<OutputFrame> frames = new LinkedBlockingDeque<>();

    @Override
    public void accept(OutputFrame frame) {
        frames.add(frame);
    }

    /**
     * Get access to the underlying frame buffer. Modifying the buffer contents is likely to cause problems if the
     * waitUntil() methods are also being used, as they feed on the same data.
     *
     * @return the collection of frames
     */
    public LinkedBlockingDeque<OutputFrame> getFrames() {
        return frames;
    }

    /**
     * Wait until any frame (usually, line) of output matches the provided predicate.
     * <p>
     * Note that lines will often have a trailing newline character, and this is not stripped off before the
     * predicate is tested.
     *
     * @param predicate a predicate to test against each frame
     */
    public void waitUntil(Predicate<OutputFrame> predicate) throws TimeoutException {
        // ~2.9 million centuries ought to be enough for anyone
        waitUntil(predicate, Long.MAX_VALUE, 1);
    }

    /**
     * Wait until any frame (usually, line) of output matches the provided predicate.
     * <p>
     * Note that lines will often have a trailing newline character, and this is not stripped off before the
     * predicate is tested.
     *
     * @param predicate a predicate to test against each frame
     * @param limit     maximum time to wait
     * @param limitUnit maximum time to wait (units)
     */
    public void waitUntil(Predicate<OutputFrame> predicate, int limit, TimeUnit limitUnit) throws TimeoutException {
        waitUntil(predicate, limit, limitUnit, 1);
    }

    /**
     * Wait until any frame (usually, line) of output matches the provided predicate.
     * <p>
     * Note that lines will often have a trailing newline character, and this is not stripped off before the
     * predicate is tested.
     *
     * @param predicate a predicate to test against each frame
     * @param limit     maximum time to wait
     * @param limitUnit maximum time to wait (units)
     * @param times     number of times the predicate has to match
     */
    public void waitUntil(Predicate<OutputFrame> predicate, long limit, TimeUnit limitUnit, int times) throws TimeoutException {
        long expiry = limitUnit.toMillis(limit) + System.currentTimeMillis();

        waitUntil(predicate, expiry, times);
    }

    private void waitUntil(Predicate<OutputFrame> predicate, long expiry, int times) throws TimeoutException {

        int numberOfMatches = 0;
        while (System.currentTimeMillis() < expiry) {
            try {
                OutputFrame frame = frames.pollLast(100, TimeUnit.MILLISECONDS);

                if (frame != null) {
                    final String trimmedFrameText = frame.getUtf8String().replaceFirst("\n$", "");
                    LOGGER.debug("{}: {}", frame.getType(), trimmedFrameText);

                    if (predicate.test(frame)) {
                        numberOfMatches++;

                        if (numberOfMatches == times) {
                            return;
                        }
                    }
                }

                if (frames.isEmpty()) {
                    // sleep for a moment to avoid excessive CPU spinning
                    Thread.sleep(10L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // did not return before expiry was reached
        throw new TimeoutException();
    }

    /**
     * Wait until Docker closes the stream of output.
     */
    public void waitUntilEnd() {
        try {
            waitUntilEnd(Long.MAX_VALUE);
        } catch (TimeoutException e) {
            // timeout condition can never occur in a realistic timeframe
            throw new IllegalStateException(e);
        }
    }

    /**
     * Wait until Docker closes the stream of output.
     *
     * @param limit     maximum time to wait
     * @param limitUnit maximum time to wait (units)
     */
    public void waitUntilEnd(long limit, TimeUnit limitUnit) throws TimeoutException {
        long expiry = limitUnit.toMillis(limit) + System.currentTimeMillis();

        waitUntilEnd(expiry);
    }

    private void waitUntilEnd(Long expiry) throws TimeoutException {
        while (System.currentTimeMillis() < expiry) {
            try {
                OutputFrame frame = frames.pollLast(100, TimeUnit.MILLISECONDS);

                if (frame == OutputFrame.END) {
                    return;
                }

                if (frames.isEmpty()) {
                    // sleep for a moment to avoid excessive CPU spinning
                    Thread.sleep(10L);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new TimeoutException("Expiry time reached before end of output");
    }
}
