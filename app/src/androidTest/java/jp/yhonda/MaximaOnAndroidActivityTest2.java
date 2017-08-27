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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MaximaOnAndroidActivityTest2 {

    @Rule
    public ActivityTestRule<MaximaOnAndroidActivity> mActivityTestRule = new ActivityTestRule<>(MaximaOnAndroidActivity.class);

    @Test
    public void maximaOnAndroidActivityTest2() {
        intallSequence();
        waitForStartup();

        String[] manLang = { "Brasilian Portuguese",
                "English",
                "German",
                "Japanese",
                "Portuguese",
                "Spanish"};
        String[] manXPATH = {"/html/body/p[1]/i",
                "/html/body/p[2]",
                "/html/body/p[1]",
                "/html/body/p[2]",
                "/html/body/p[1]/em",
                "/html/body/p[1]/em"};
        String[] manSentence = {"Maxima é um sistema de álgebra computacional, implementado em Lisp.",
                "Maxima is a computer algebra system, implemented in Lisp.",
                "Maxima ist ein Computeralgebrasystem, das in Lisp programmiert ist.",
                "Maximaはコンピュータ代数システムです。Lispで実装されています。",
                "Maxima é um Sistema de Computação Algébrica, programado em Lisp.",
                "Maxima es un sistema de cálculo simbólico escrito en Lisp."};
        for (int c=0; c<manLang.length; c++) {
            // Press Menu
            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            // Choose Preference menu
            ViewInteraction appCompatTextView = onView(
                    allOf(withId(R.id.title), withText("Preferences"), isDisplayed()));
            appCompatTextView.perform(click());
            // Set the language of manual to japanese
            ViewInteraction linearLayout = onView(
                    allOf(childAtPosition(
                            allOf(withId(android.R.id.list),
                                    withParent(withClassName(is("android.widget.LinearLayout")))),
                            1),
                            isDisplayed()));
            linearLayout.perform(click());

            ViewInteraction checkedTextView = onView(
                    allOf(withId(android.R.id.text1), withText(manLang[c]),
                            childAtPosition(
                                    allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                                            withParent(withClassName(is("android.widget.LinearLayout")))),
                                    c),
                            isDisplayed()));
            checkedTextView.perform(click());
/*
            ViewInteraction checkedTextView = onView(
                    allOf(withId(android.R.id.text1), withText(manLang[c]),
                            childAtPosition(
                                    allOf(withClassName(endsWith("RecycleListView")),
                                            withParent(withClassName(endsWith("FrameLayout")))),
                                    c),
                            isDisplayed()));
            checkedTextView.perform(click());
*/
            // Go back to Maxima
            pressBack();
            // Press menu
            openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
            // Choose Manual menu
            ViewInteraction appCompatTextView2 = onView(
                    allOf(withId(R.id.title), withText("Manual"), isDisplayed()));
            appCompatTextView2.perform(click());

            // Added a sleep statement to match the app's execution delay.
            // The recommended way to handle such scenarios is to use Espresso idling resources:
            // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Make sure Japanese Manual is displayed.
            onWebView()
                    .withElement(findElement(Locator.XPATH, manXPATH[c])).check(webMatches(getText(), containsString(manSentence[c])));
            // Go back to Maxima
            pressBack();
        }
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

    private static void waitFor(int sec) {
        try {
            Thread.sleep(sec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private static void waitForStartup() {
        waitFor(1000);
        Web.onWebView().check(WebViewAssertions.webContent(DomMatchers.containingTextInBody("Maxima")));
    }
}
