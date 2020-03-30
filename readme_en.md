## Download

**Step 1**. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
    repositories {
        ...
        // add the following line
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2**. Add the dependency

```gradle
dependencies {
    implementation 'com.github.jrfeng.HappyHandler:annotation:1.1.1'
    annotationProcessor 'com.github.jrfeng.HappyHandler:compiler:1.1.1'
}
```

## How to use

**HappHandler can help developers use Handler and Messenger happily.**

### 1. Autogenerate Handler

**Step 1**. Create a interface, and annotated with `happy.handler.Hanlder`, like this:

```java
package com.demo;

import happy.handler.Handler;

@Handler
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**Note: Return value of method in interface must be `void`。**

**Step 2**. Build Project

When building project, the `HappyHandler` will automatically generate an `XxxHandler` class according to the interface marked by the `@Handler` annotation (where `Xxx` is the name of the interface, for example, for the `Hello` interface in the above example, a `HelloHandler` class will be generated). The generated class inherits the `android.os.handler` and implements the corresponding interface.

The generated class has two constructor methods, like this:

```java
public HelloHandler(
        Looper looper,      // Looper
        Hello receiver      // your interface
)

public HelloHandler(
        Hello receiver      // your interface
)     // Use Looper.getMainLooper()
```

Then, you can use the `HelloHandler` class in your project, like this:

```java
import com.demo.HelloHandler;

...

// Implemented the Hello interface
public class MainActivity extends AppCompatActivity implements Hello {
    private HelloHandler mHelloHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ...
        mHelloHandler = new HelloHandler(this);
        // Then, you can use mHelloHandler in other threads to update the UI.
        // Avoid the tedious process of manually writing a custom Handler.
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

**Custom the name of Handler class：**

```java
@Handler("MyCustomHandler") // Custom the name of Handler class
public interface Hello {
    void say(String words);

    void sayHello();
}
```

### 2. Autogenerate Messenger

**Step 1**. Create a interface, and annotated with `happy.handler.Messenger`, like this:

```java
@Messenger
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**Note: The return value of the method in the interface must be `void`, and there are restrictions on the parameter type of the method (described later)**

**Step 2**. Build Project

When building project, the HappyHandler will automatically generate an `XxxMessenger` class according to the interface marked by the `@Messenger` annotation (where `Xxx` is the name of the interface, for example, for the `Hello` interface in the above example, a `HellMessenger` class will be generated). The generated class implements the corresponding interface.

The generated class has three constructor methods, like this:

```java
// Use to create client Messenger
public HelloMessenger(IBinder target)

// Used to create server Messenger
public HelloMessenger(
        Looper looper,              // Looper
        Hello receiver              // your interface
)

// Used to create server Messenger
public HelloMessenger(
        Hello receiver              // your interface
) // Use Looper.getMainLooper()
```

At the same time, a `getbinder()` method will be generated:

```java
public IBinder getBinder()
```

Then, we can use the `HelloMessenger` class in our `Service`, like this:

```java
// Service, as server
public class MyService extends Service implements Hello {
    private HelloMessenger mHelloMessenger;

    ...

    @Override
    public IBinder onBind(Intent intent) {
        mHelloMessenger = new HelloMessenger(this);
        return mHelloMessenger.getBinder(); 
    }

    @Override
    public void say(String words) {
        Log.d("MyService", words);
    }

    @Override
    public void sayHello() {
        Log.d("MyService", "Hello!");
    }
}

// Activity, as client
public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private HelloMessenger mHelloMessenger;

    ...

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mHelloMessenger = new HelloMessenger(iBinder);
    }
}
```

**Custom the name of Messenger class：**

```java
@Handler("MyCustomMessenger") // Custom the name of Messenger class
public interface Hello {
    void say(String words);

    void sayHello();
}
```

### Parameter type restrictions for methods in the Messenger interface

**Supported parameter types:**

* `Java Primitive Types`: `byte, short, int, long, float, double, char, boolean`
* `String`
* `CharSequence`
* `IBinder`
* `Parcelable`
* `Serializable`

**Supported Array types:**

* `Java Primitive Array Types`: `byte[], short[], int[], long[], float[], double[], char[], boolean[]`
* `String[]`
* `CharSequence[]`
* `Parcelable[]`

**Supported `List` types (defining parameter as a subtype of `List` is not supported):**

* `List<Integer>`
* `List<String>`
* `List<CharSequence>`
* `List<? extends Parcelable>`

**Other supported types:**

* `SparseArray<? extends Parcelable>`

**Note: The `Map` type is not supported. Please use `Bundle` instead.**

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