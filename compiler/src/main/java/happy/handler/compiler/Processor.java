/*
 * MIT License
 *
 * Copyright (c) 2020 jrfeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package happy.handler.compiler;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import happy.handler.Handler;
import happy.handler.Messenger;
import happy.handler.compiler.generator.HandlerGenerator;
import happy.handler.compiler.generator.MessengerGenerator;


@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "happy.handler.Handler",
        "happy.handler.Messenger"
})
public class Processor extends AbstractProcessor {
    private HandlerGenerator mHandlerGenerator;
    private MessengerGenerator mMessengerGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mHandlerGenerator = new HandlerGenerator(processingEnv);
        mMessengerGenerator = new MessengerGenerator(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        generateAllHandlerSourceFile(roundEnv);
        generateAllMessengerSourceFile(roundEnv);
        return true;
    }

    private List<TypeElement> getAllInterfaceElement(Set<? extends Element> elements) {
        List<TypeElement> interfaces = new ArrayList<>();

        for (Element element : elements) {
            if (isInterface(element)) {
                interfaces.add((TypeElement) element);
            }
        }

        return interfaces;
    }

    private boolean isInterface(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    private void generateAllHandlerSourceFile(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Handler.class);
        List<TypeElement> interfaces = getAllInterfaceElement(elements);

        for (TypeElement targetInterface : interfaces) {
            generateHandlerSourceFile(targetInterface);
        }
    }

    private void generateHandlerSourceFile(TypeElement interfaceElement) {
        String className = interfaceElement.getSimpleName() + "Handler";
        if (!"".equals(interfaceElement.getAnnotation(Handler.class).value())) {
            className = interfaceElement.getAnnotation(Handler.class).value();
        }

        TypeSpec handlerSpec = mHandlerGenerator.generate(className, interfaceElement);

        JavaFile handlerFile = JavaFile.builder(
                processingEnv.getElementUtils().getPackageOf(interfaceElement).getQualifiedName().toString(),
                handlerSpec
        ).build();

        try {
            handlerFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.NOTE, "fail, can not create source file: " +
                            handlerFile.packageName + "." +
                            handlerFile.typeSpec.name);
        }
    }

    private void generateAllMessengerSourceFile(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Messenger.class);
        List<TypeElement> interfaces = getAllInterfaceElement(elements);

        for (TypeElement targetInterface : interfaces) {
            generateMessengerSourceFile(targetInterface);
        }
    }

    private void generateMessengerSourceFile(TypeElement interfaceElement) {
        String className = interfaceElement.getSimpleName() + "Messenger";
        if (!"".equals(interfaceElement.getAnnotation(Messenger.class).value())) {
            className = interfaceElement.getAnnotation(Messenger.class).value();
        }

        TypeSpec messengerSpec = mMessengerGenerator.generate(className, interfaceElement);

        JavaFile messengerFile = JavaFile.builder(
                processingEnv.getElementUtils().getPackageOf(interfaceElement).getQualifiedName().toString(),
                messengerSpec
        ).build();

        try {
            messengerFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.NOTE, "fail, can not create source file: " +
                            messengerFile.packageName + "." +
                            messengerFile.typeSpec.name);
        }
    }
}
