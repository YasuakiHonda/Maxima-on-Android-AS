package jp.yhonda;


import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.web.assertion.WebAssertion;
import androidx.test.espresso.web.assertion.WebViewAssertions;
import androidx.test.espresso.web.sugar.Web;
import androidx.test.espresso.web.sugar.Web.WebInteraction;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.espresso.web.matcher.DomMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.containsString;
import static jp.yhonda.TestUtil.reliableClick;
import static jp.yhonda.TestUtil.waitUntil;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MaximaOnAndroidActivityTest2 {

    @Rule
    public ActivityScenarioRule<MaximaOnAndroidActivity> mActivityScenarioRule = new ActivityScenarioRule<>(MaximaOnAndroidActivity.class);

    @Test
    public void maximaOnAndroidActivityTest2() {
        waitForStartup();

        String[] manLang = { "Brasilian Portuguese",
                "English",
                "German",
                "Japanese",
                "Portuguese",
                "Spanish"};
        String[] manSentence = {"Maxima é um sistema de álgebra computacional, implementado em Lisp.",
                "Maxima is a computer algebra system, implemented in Lisp.",
                "Maxima ist ein Computeralgebrasystem, das in Lisp programmiert ist.",
                "Maximaはコンピュータ代数システムです。Lispで実装されています。",
                "Maxima é um Sistema de Computação Algébrica, programado em Lisp.",
                "Maxima es un sistema de cálculo simbólico escrito en Lisp."};
        for (int c=0; c<manLang.length; c++) {
            final int lang = c;
            // Choose the manual language in Preferences
            openMenuAndSelect("Preferences");
            waitUntil(() -> onView(allOf(withText("Language for Maxima Manual"), isDisplayed()))
                    .perform(reliableClick()), 10000);
            waitUntil(() -> onView(allOf(withText(manLang[lang]), isDisplayed()))
                    .perform(reliableClick()), 10000);
            // Go back to Maxima and open the manual
            pressBack();
            openMenuAndSelect("Manual");
            // The manual in the chosen language is displayed: its first paragraph
            // mentioning Lisp. (Read via getText(): the DOM-based matchers garble
            // non-ASCII text.)
            waitUntil(() -> onWebView()
                    .withElement(findElement(Locator.XPATH, "(//p[contains(., 'Lisp')])[1]"))
                    .check(webMatches(getText(), containsString(manSentence[lang]))), 15000);
            // Go back to Maxima
            pressBack();
        }
    }

    /** Opens the overflow menu once the activity is ready and selects an item. */
    private static void openMenuAndSelect(String title) {
        waitUntil(() -> openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext()),
                10000);
        waitUntil(() -> onView(allOf(withId(androidx.appcompat.R.id.title), withText(title),
                isDisplayed())).perform(reliableClick()), 10000);
    }

    @Test
    public void AboutMoA() {
        waitForStartup();

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
