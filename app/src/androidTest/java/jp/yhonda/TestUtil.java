package jp.yhonda;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

/** Helpers that keep the UI tests stable on slow (e.g. software-rendered CI) emulators. */
final class TestUtil {
    private TestUtil() {
    }

    /**
     * Runs an Espresso step until it succeeds or timeoutMs has passed. For steps
     * that wait on something Espresso does not track: Maxima starting up, a web
     * page loading, an activity transition finishing.
     */
    static void waitUntil(Runnable step, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            try {
                step.run();
                return;
            } catch (RuntimeException | AssertionError e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * A click that is repeated if the device registered it as a long press,
     * which happens when a slow emulator delays the touch up event.
     */
    static ViewAction reliableClick() {
        return click(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "nothing (retry the click)";
            }

            @Override
            public void perform(UiController uiController, View view) {
            }
        });
    }
}
