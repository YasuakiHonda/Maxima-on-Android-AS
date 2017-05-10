package jp.yhonda;

/*
    Copyright 2017 Yasuaki Honda (yasuaki.honda@gmail.com)
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

import java.util.*;

public final class AppGlobals {

    private static AppGlobals singleton = new AppGlobals();
    Map<String, String> map = new HashMap<String, String>();

    private AppGlobals() {};

    public static AppGlobals getSingleton() {
        return singleton;
    }

    public void set(String key, String value) {
        map.put(key,value);
    }
    public String get(String key) {
        return ((String)map.get(key));
    }
}
