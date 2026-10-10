/*
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package jp.yhonda;

import android.app.Activity;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge support. Apps targeting Android 16 always draw behind the
 * system bars, so each screen keeps its content out of the status bar,
 * navigation bar, display cutout and on-screen keyboard itself.
 */
final class SystemBars {
    private SystemBars() {
    }

    /**
     * For AppCompat screens: draw edge-to-edge on every Android version (not
     * only where the system enforces it) and pad the content view. Call
     * before setContentView().
     */
    static void setUp(ComponentActivity activity) {
        EdgeToEdge.enable(activity);
        padForInsets(activity.findViewById(android.R.id.content));
    }

    /**
     * For framework screens without an action bar (the preferences): pad the
     * content view and use dark status bar icons on the light background.
     */
    static void setUpPlain(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        padForInsets(content);
        WindowCompat.getInsetsController(activity.getWindow(), content)
                .setAppearanceLightStatusBars(true);
    }

    /**
     * Pads the view by the system bars, display cutout and keyboard. The
     * action bar of AppCompat screens already consumes the top inset.
     */
    static void padForInsets(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(left + insets.left, top + insets.top,
                    right + insets.right, bottom + insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
