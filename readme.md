**Help developer implement `handleMessage()` method.**

**Traditional：**

```java
public class MainActivity extends AppCompatActivity {

    ...

    public void say(String words) {
        Toast.makeText(this, words, Toast.LENGTH_SHORT).show();
    }

    public void sayHello() {
        Toast.makeText(this, "Hello!", Toast.LENGTH_SHORT).show();
    }

    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> mMainActivityRef;

        private static final int SAY = 1;
        private static final int SAY_HELLO = 2;

        MyHandler(MainActivity mainActivity) {
            mMainActivityRef = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity mainActivity = mMainActivityRef.get();
            if (mainActivity == null) {
                return;
            }

            switch (msg.what) {
                case SAY:
                    mainActivity.say((String) msg.obj);
                    break;
                case SAY_HELLO:
                    mainActivity.sayHello();
                    break;
            }
        }
    }
}
```

**Use HappyHandler：**

**Step 1**. Create a interface, and annotated with `happy.handler.Hanlder`:

```java
package com.demo;

import happy.handler.Handler;

@Handler
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**Step 2**. Build project

Build your project, then will automatically generate a **`HelloHandler`** class, that extends `android.os.Handler` and implemented `Hello` interface.

Then, we can use `HelloHandler` like this:

```java
import com.demo.HelloHandler;

...

public class MainActivity extends AppCompatActivity implements Hello {
    private HelloHandler mHelloHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ...
        mHelloHandler = new HelloHandler(this);
    }

    @Override
    public void say(String words) {
        Toast.makeText(this, words, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void sayHello() {
        Toast.makeText(this, "Hello!", Toast.LENGTH_SHORT).show();
    }
}
```

**Custom Handler name:**

```java
@Handler("MyCustomHandler") // custom Handler name
public interface Hello {
    void say(String words);

    void sayHello();
}
```

## Download

**Step 1**. Add it in your root `build.gradle` at the end of repositories:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2**. Add the dependency

```gradle
dependencies {
    implementation 'com.github.jrfeng:HappyHandler:1.0'
}
```

## LICENSE

```
MIT License

Copyright (c) 2020 jrfeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```