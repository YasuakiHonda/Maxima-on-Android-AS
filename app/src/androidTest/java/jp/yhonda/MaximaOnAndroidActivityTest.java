package jp.yhonda;


import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.web.assertion.WebAssertion;
import android.support.test.espresso.web.assertion.WebViewAssertions;
import android.support.test.espresso.web.sugar.Web;
import android.support.test.espresso.web.sugar.Web.WebInteraction;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.espresso.web.matcher.DomMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(AndroidJUnit4.class)
public class MaximaOnAndroidActivityTest {

    @Rule
    public ActivityTestRule<MaximaOnAndroidActivity> mActivityTestRule = new ActivityTestRule<>(MaximaOnAndroidActivity.class);

    @Test
    public void testMoAInstallandRun() {
        intallSequence();
        waitForStartup();
        ViewInteraction multiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        1),
                                0),
                        isDisplayed()));
        multiAutoCompleteTextView.check(matches(isDisplayed()));

        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));

        appCompatMultiAutoCompleteTextView.perform(replaceText("sum(a[n],n,1,inf)"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-1"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(sin(x)*exp(-x),x,0,inf)"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-2"));
    }

    @Test
    public void testMiscOutputSequence() {
        intallSequence();
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("file_search_maxima"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-1"));


        appCompatMultiAutoCompleteTextView.perform(replaceText("\"$$$$\""), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-2"));

    }

    @Test
    public void testMiscInputSequence() {
        intallSequence();
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("askinteger(n);"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-1"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("y;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-2"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("asksign(x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-3"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("neg;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-4"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(x^n,x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-5"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("yes;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-6"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(exp(n*x),x,0,inf);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-7"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("neg;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-8"));
    }

    @Test
    public void testCSetup() {
        intallSequence();
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("load(ctensor);"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-1"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("csetup();"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-2"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("4"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-3"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("n;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-4"));//Do you want to enter matrix?

        appCompatMultiAutoCompleteTextView.perform(replaceText("1;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Answer 1, 2, 3 or 4 :")));

        appCompatMultiAutoCompleteTextView.perform(replaceText("1;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Row 1 Column 1:")));

        appCompatMultiAutoCompleteTextView.perform(replaceText("a"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Row 2 Column 2:")));

        appCompatMultiAutoCompleteTextView.perform(replaceText("x^2;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Row 3 Column 3:")));

        appCompatMultiAutoCompleteTextView.perform(replaceText("x^2*sin(y)^2;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Row 4 Column 4:")));

        appCompatMultiAutoCompleteTextView.perform(replaceText("-d;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-5"));//Enter functional dependencies

        appCompatMultiAutoCompleteTextView.perform(replaceText("dependes([a,d],x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-6"));//Do you want to see matrix?

        appCompatMultiAutoCompleteTextView.perform(replaceText("y;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-7"));//done


        appCompatMultiAutoCompleteTextView.perform(replaceText("christof(mcs);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-15"));
    }

    @Test
    public void testReuse() {
        /*
        Reuse input
        Compute something
        Compute next thing
        Click the first input
        Ensure the first input is in the input area
        */
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("38*990/20145;"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        // remember the first output element which is clickable
        WebInteraction calcResult=onWebView().withElement(findElement(Locator.ID, "moa1"));

        appCompatMultiAutoCompleteTextView.perform(replaceText("sqrt(25*99*256);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().withElement(findElement(Locator.ID, "MathJax-Element-2"));

        onWebView().withElement(findElement(Locator.ID, "38*990/20145;")).perform(webClick());
        appCompatMultiAutoCompleteTextView.check(matches(withText("38*990/20145;")));

        /*
        Reuse output
        Click the first output
        Ensure the first output is in the input area
         */
        calcResult.perform(webClick());
        appCompatMultiAutoCompleteTextView.check(matches(withText("2508/1343")));

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static void waitFor(int sec) {
        try {
            Thread.sleep(sec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static void intallSequence() {
        try {
            // if started with Installer Activity, we press Install button to continue for installation.
            ViewInteraction button = onView(
                    allOf(withId(R.id.button1),
                            childAtPosition(
                                    childAtPosition(
                                            IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                            3),
                                    1),
                            isDisplayed()));
            button.check(matches(isDisplayed()));

            ViewInteraction appCompatButton = onView(
                    allOf(withId(R.id.button1), withText("Install"), isDisplayed()));
            appCompatButton.perform(click());
        } catch (Exception e) {
            Log.v("MoA","skip install screen");
        }
    }

    private static void waitForStartup() {
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Maxima")));
    }
}
