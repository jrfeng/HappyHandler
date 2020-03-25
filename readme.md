帮助开发者实现 **`handleMessage()`** 方法。

**传统做法：**

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

**第 2 步**：构造项目

构建你的项目，注解处理器会根据接口自动生成一个 **`HelloHandler`** 类，该类继承了 `android.os.Handler` 类，并且实现了前面创建的 `Hello` 接口。

接着，我们就可以在项目中自由的使用 `HelloHandler` 类了：

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

这样可以避免编写不必要的 `switch` 代码。

**自定义 `Handler` 名称:**

```java
@Handler("MyCustomHandler") // 自定义 Handler 名称
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