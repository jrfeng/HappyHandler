**功能：帮助开发者轻松的使用 Handler 与 Messenger。**

## Handler

**使用 Handler 的传统做法：**

```java
public class MainActivity extends AppCompatActivity {

    ...

    public void say(String words) {
        Toast.makeText(this, words, Toast.LENGTH_SHORT).show();
    }

    public void sayHello() {
        Toast.makeText(this, "Hello!", Toast.LENGTH_SHORT).show();
    }

    // 自定义 Handler
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

可以看到，为了安全的使用 `Handler`，开发者不得不编写一串长长的模板代码，如果需要在多个地方使用 `Handler`，则不得不一遍又一遍的重复这个繁琐过程。`HappyHander` 可以帮助我们简化这一流程，开发者只需定义一个接口，剩下的事交给 `HappyHander` 即可。

**使用 HappyHandler：**

**第 1 步**：创建一个接口，并且使用 `happy.handler.Hanlder` 注解标注它：

```java
package com.demo;

import happy.handler.Handler;

@Handler
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**注意！接口中方法的返回值必须是 `void`。**

**第 2 步**：构建项目

构建项目时，`HappyHandler` 的会根据被 `@Handler` 注解标记的接口自动生成一个 `XxxHandler` 类（其中，`Xxx` 是接口的名称，例如，对于上例中的 `Hello` 接口来说，将生成一个 `HelloHandler` 类），生成的类继承了 `android.os.Handler` 类，并且实现了对应的接口。

生成的类具有 `2` 个构造方法，格式如下所示：

```java
public HelloHandler(
        Looper looper,      // Looper
        Hello receiver      // 接口类型，事件的接收者
)

public HelloHandler(
        Hello receiver      // 接口类型，事件的接收者
)     // 使用默认的 Looper.getMainLooper()
```

接着，我们就可以在项目中自由的使用 `HelloHandler` 类了：

```java
import com.demo.HelloHandler;

...

// 实现了 Hello 接口
public class MainActivity extends AppCompatActivity implements Hello {
    private HelloHandler mHelloHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ...
        mHelloHandler = new HelloHandler(this);
        // 接下来，就可以在其他线程中使用 mHelloHandler 来更新 UI
        // 避免了手动编写自定义 Handler 的繁琐过程
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

**自定义生成的 `Handler` 类的名称:**

```java
@Handler("MyCustomHandler") // 自定义 Handler 名称
public interface Hello {
    void say(String words);

    void sayHello();
}
```

## Messenger

**使用 `Messenger` 时，开发者同样需要编写一系列的模板代码。`HappyHandler` 同样可以简化这一过程。**

**第 1 步**：创建一个接口，并使用 `happy.handler.Messenger` 注解标注它：

```java
@Messenger
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**注意！接口中方法的返回值必须是 `void`。**

**第 2 步**：构建项目

构建项目时，`HappyHandler` 的会根据被 `@Messenger` 注解标记的接口自动生成一个 `XxxMessenger` 类（其中，`Xxx` 是接口的名称，例如，对于上例中的 `Hello` 接口来说，将生成一个 `HelloMessenger` 类），生成的类继承了 `android.os.Handler` 类，并且实现了对应的接口。

生成的类具有 `3` 个构造方法，格式如下所示：

```java
// 用于创建客户端 Messenger
public MyMessenger(IBinder target)

// 用于创建服务端 Messenger
public MyMessenger(
        Looper looper,              // Looper
        HelloMessenger receiver     // 接口类型，事件的接收者
)

// 用于创建服务端 Messenger
public MyMessenger(
        HelloMessenger receiver     // 接口类型，事件的接收者
) // 使用默认的 Looper.getMainLooper()
```

同时还会生成一个 `getBinder()` 方法：

```java
public IBinder getBinder()
```

接着，我们就能在 `Service` 中自由的使用 `HelloMessenger` 类了。

**例：**

```java
// 服务端，实现了 Hello 接口
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

// 客户端
public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private HelloMessenger mHelloMessenger;

    ...

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mHelloMessenger = new HelloMessenger(iBinder);
    }
}
```

**自定义生成的 `Messenger` 类的名称:**

```java
@Handler("MyCustomMessenger") // 自定义 Handler 名称
public interface Hello {
    void say(String words);

    void sayHello();
}
```

## Download

**第 1 步**：在项目的根目录下的 `build.gradle` 文件中添加以下代码：

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**第 2 步**：添加依赖：

```gradle
dependencies {
    implementation 'com.github.jrfeng.HappyHandler:annotation:1.0.1'
    annotationProcessor 'com.github.jrfeng.HappyHandler:compiler:1.0.1'
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