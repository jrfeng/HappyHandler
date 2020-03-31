[**English**](./readme_en.md)

## Download

**第 1 步**：在项目的根目录下的 `build.gradle` 文件中添加以下代码：

```gradle
allprojects {
    repositories {
        ...
        // 添加下面这行代码
        maven { url 'https://jitpack.io' }
    }
}
```

**第 2 步**：添加依赖：

```gradle
dependencies {
    implementation 'com.github.jrfeng.HappyHandler:annotation:1.1.2'
    annotationProcessor 'com.github.jrfeng.HappyHandler:compiler:1.1.2'
}
```

## 开始使用

**HappyHandler 可帮助开发者轻松的使用 Handler 与 Messenger。**

### 1. 自动生成 Handler

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

接着，就可以在项目中使用 `HelloHandler` 类了：

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

### 2. 自动生成 Messenger

**第 1 步**：创建一个接口，并使用 `happy.handler.Messenger` 注解标注它：

```java
@Messenger
public interface Hello {
    void say(String words);

    void sayHello();
}
```

**注意！接口中方法的返回值必须是 `void`，且对方法的参数类型也是有限制的（后面会介绍）。**

**第 2 步**：构建项目

构建项目时，`HappyHandler` 的会根据被 `@Messenger` 注解标记的接口自动生成一个 `XxxMessenger` 类（其中，`Xxx` 是接口的名称，例如，对于上例中的 `Hello` 接口来说，将生成一个 `HelloMessenger` 类），生成的类实现了对应的接口。

生成的类具有 `3` 个构造方法，格式如下所示：

```java
// 用于创建客户端 Messenger
public HelloMessenger(IBinder target)

// 用于创建服务端 Messenger
public HelloMessenger(
        Looper looper,              // Looper
        Hello receiver              // 接口类型，事件的接收者
)

// 用于创建服务端 Messenger
public HelloMessenger(
        Hello receiver              // 接口类型，事件的接收者
) // 使用默认的 Looper.getMainLooper()
```

同时还会生成一个 `getBinder()` 和一个 `getMessenger()` 方法：

```java
public IBinder getBinder()

public Messenger getMessenger()
```

接着，我们就能在 `Service` 中使用 `HelloMessenger` 类了。

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
@Handler("MyCustomMessenger") // 自定义 Messenger 名称
public interface Hello {
    void say(String words);

    void sayHello();
}
```

### Messenger 接口中方法的参数类型限制

**支持的参数类型：**

* `Java` 基本类型：`byte, short, int, long, float, double, char, boolean`
* `String`
* `CharSequence`
* `IBinder`
* `Parcelable`
* `Serializable`

**支持的数组类型：**

* `Java` 基本类型数组：`byte[], short[], int[], long[], float[], double[], char[], boolean[]`
* `String[]`
* `CharSequence[]`
* `Parcelable[]`

**支持的 List 类型（不支持将参数定义为 List 的子类型）：**

* `List<Integer>`
* `List<String>`
* `List<CharSequence>`
* `List<? extends Parcelable>`

**支持的其他类型：**

* `SparseArray<? extends Parcelable>`

**注意！不支持 `Map` 类型，请使用 `Bundle` 代替。**

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