package jp.yhonda;


import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.web.assertion.WebViewAssertions;
import androidx.test.espresso.web.sugar.Web;
import androidx.test.espresso.web.sugar.Web.WebInteraction;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.espresso.web.matcher.DomMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.w3c.dom.Element;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webContent;
import static androidx.test.espresso.web.matcher.DomMatchers.elementById;
import static androidx.test.espresso.web.matcher.DomMatchers.withTextContent;

@RunWith(AndroidJUnit4.class)
public class MaximaOnAndroidActivityTest {

    @Rule
    public ActivityScenarioRule<MaximaOnAndroidActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MaximaOnAndroidActivity.class);

    @Test
    public void testMoAInstallandRun() {
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
        onWebView().check(webContent(elementById("MathJax-Element-1", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(sin(x)*exp(-x),x,0,inf)"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-2", notNullValue(Element.class))));
    }

    @Test
    public void testMiscOutputSequence() {
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("file_search_maxima"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-1", notNullValue(Element.class))));


        appCompatMultiAutoCompleteTextView.perform(replaceText("\"$$$$\""), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-2", notNullValue(Element.class))));

    }

    @Test
    public void testMiscInputSequence() {
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("askinteger(n);"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-1", notNullValue(Element.class))));
        onWebView().check(webContent(
                elementById("MathJax-Element-1", withTextContent(" \\text{Is }n\\text{ an }{\\it integer}\\text{?}  "))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("y;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-2", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("asksign(x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-3", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("neg;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-4", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(x^n,x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-5", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("yes;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-6", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("integrate(exp(n*x),x,0,inf);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-7", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("neg;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-8", notNullValue(Element.class))));
    }

    @Test
    public void testCSetup() {
        waitForStartup();
        ViewInteraction appCompatMultiAutoCompleteTextView = onView(
                allOf(withId(R.id.editText1), isDisplayed()));
        appCompatMultiAutoCompleteTextView.perform(replaceText("load(ctensor);"), closeSoftKeyboard());
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.enterB), withText("Enter"), isDisplayed()));
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-1", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("csetup();"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-2", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("4"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-3", notNullValue(Element.class))));

        appCompatMultiAutoCompleteTextView.perform(replaceText("n;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-4", notNullValue(Element.class))));//Do you want to enter matrix?

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
        onWebView().check(webContent(elementById("MathJax-Element-5", notNullValue(Element.class))));//Enter functional dependencies

        appCompatMultiAutoCompleteTextView.perform(replaceText("dependes([a,d],x);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-6", notNullValue(Element.class))));//Do you want to see matrix?

        appCompatMultiAutoCompleteTextView.perform(replaceText("y;"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(1000);
        onWebView().check(webContent(elementById("MathJax-Element-7", notNullValue(Element.class))));//done


        appCompatMultiAutoCompleteTextView.perform(replaceText("christof(mcs);"), closeSoftKeyboard());
        appCompatButton2.perform(click());
        waitFor(2000);
        onWebView().check(webContent(elementById("MathJax-Element-15", notNullValue(Element.class))));
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
        onWebView().check(webContent(elementById("MathJax-Element-2", notNullValue(Element.class))));

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

    private static void waitForStartup() {
        // The first start unpacks Maxima's data; wait for the banner.
        jp.yhonda.TestUtil.waitUntil(() -> onWebView().check(WebViewAssertions.webContent(
                DomMatchers.containingTextInBody("Maxima"))), 60000);
    }
}
