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
package happy.handler.compiler.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

import javafx.util.Pair;

public abstract class AbstractGenerator {
    private Messager mMessager;
    private List<Pair<String, ExecutableElement>> mInterfaceMethodPairs;
    private List<FieldSpec> mAllMethodIdFields;

    private TypeSpec.Builder mHandlerBuilder;

    AbstractGenerator(ProcessingEnvironment processingEnv) {
        mMessager = processingEnv.getMessager();
        mInterfaceMethodPairs = new ArrayList<>();
    }

    public abstract void implement_sendMessageStatement(MethodSpec.Builder builder, String argName);

    protected abstract void implementMethodStatement(MethodSpec.Builder builder, Pair<String, ExecutableElement> methodPair);

    protected abstract void implement_handleMessageStatement(MethodSpec.Builder builder, TypeElement interfaceElement, String paramName);

    protected TypeSpec.Builder getInterfaceBuilder() {
        return mHandlerBuilder;
    }

    protected final List<Pair<String, ExecutableElement>> getInterfaceMethodPairs() {
        return mInterfaceMethodPairs;
    }

    public TypeSpec generate(String className, TypeElement interfaceElement) {
        List<ExecutableElement> allMethodElement = getAllMethod(interfaceElement);
        checkMethodReturnType(allMethodElement);

        mAllMethodIdFields = generateAllMethodId(getAllMethod(interfaceElement));

        TypeSpec.Builder handlerBuilder = buildHandler(className, interfaceElement);

        implementInterface(getInterfaceBuilder(), interfaceElement);

        generateHandlerConstructors(handlerBuilder, interfaceElement);

        return handlerBuilder.build();
    }

    private TypeSpec.Builder buildHandler(String className, TypeElement interfaceElement) {
        // Receiver Class Builder
        mHandlerBuilder = TypeSpec.classBuilder(className)
                .superclass(ClassName.get("android.os", "Handler"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Field: mReceiverWeakRef
        mHandlerBuilder.addField(generateReceiverWeakReference());

        // handleMessage
        mHandlerBuilder.addMethod(implement_handleMessage(interfaceElement));

        return mHandlerBuilder;
    }

    private MethodSpec implement_handleMessage(TypeElement interfaceElement) {
        String paramName = "msg";

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handleMessage")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(void.class)
                .addParameter(ClassName.get("android.os", "Message"), paramName);

        implement_handleMessageStatement(builder, interfaceElement, paramName);

        return builder.build();
    }

    private void implementInterface(TypeSpec.Builder builder, TypeElement interfaceElement) {
        builder.addSuperinterface(interfaceElement.asType());

        // Copy Generic Type
        for (TypeParameterElement t : interfaceElement.getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(t));
        }

        // Method Id
        builder.addFields(mAllMethodIdFields);

        // Field: LAST_METHOD_ID
        builder.addField(generateLastMethodIdField());

        // implement all method
        builder.addMethods(implementAllMethod());

        // _sendMessage
        builder.addMethod(generate_sendMessage());
    }

    private List<ExecutableElement> getAllMethod(TypeElement element) {
        List<? extends Element> elements = element.getEnclosedElements();

        List<ExecutableElement> result = new ArrayList<>(elements.size());

        for (Element e : elements) {
            if (e instanceof ExecutableElement) {
                result.add((ExecutableElement) e);
            }
        }

        return result;
    }

    private void checkMethodReturnType(List<ExecutableElement> elements) {
        for (ExecutableElement e : elements) {
            if (returnTypeNotVoid(e)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "return type must be void.", e);
            }
        }
    }

    private boolean returnTypeNotVoid(ExecutableElement element) {
        return element.getReturnType().getKind() != TypeKind.VOID;
    }

    private List<FieldSpec> generateAllMethodId(List<ExecutableElement> methodElements) {
        mInterfaceMethodPairs.clear();

        List<FieldSpec> allMethodIdSpec = new ArrayList<>(methodElements.size());

        for (int i = 1; i <= methodElements.size(); i++) {
            ExecutableElement method = methodElements.get(i - 1);
            FieldSpec methodIdSpec = generateMethodId(method, i);

            allMethodIdSpec.add(methodIdSpec);
            mInterfaceMethodPairs.add(new Pair<>(methodIdSpec.name, method));
        }

        return allMethodIdSpec;
    }

    private FieldSpec generateLastMethodIdField() {
        return FieldSpec.builder(int.class, "LAST_METHOD_ID")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(String.valueOf(mInterfaceMethodPairs.size()))
                .build();
    }

    private FieldSpec generateReceiverWeakReference() {
        return FieldSpec.builder(WeakReference.class,
                "mReceiverWeakRef",
                Modifier.PRIVATE
        ).build();
    }

    private FieldSpec generateMethodId(ExecutableElement methodElement, int id) {
        return FieldSpec.builder(
                TypeName.INT,
                "METHOD_" + id,
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        ).initializer("$L", id)
                .addJavadoc(methodElement.toString())
                .build();

    }

    private List<MethodSpec> implementAllMethod() {
        List<MethodSpec> allMethod = new ArrayList<>(mInterfaceMethodPairs.size());

        for (Pair<String, ExecutableElement> methodPair : mInterfaceMethodPairs) {
            allMethod.add(implementMethod(methodPair));
        }

        return allMethod;
    }

    private MethodSpec implementMethod(Pair<String, ExecutableElement> methodPair) {
        MethodSpec.Builder builder = MethodSpec.overriding(methodPair.getValue());

        implementMethodStatement(builder, methodPair);

        return builder.build();
    }

    private MethodSpec generate_sendMessage() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("_sendMessage")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get("android.os", "Message"), "message");

        implement_sendMessageStatement(builder, "message");

        return builder.build();
    }

    // Constructor:
    // 1. (Looper looper, Receiver receiver)
    // 2. (Receiver receiver)
    private void generateHandlerConstructors(TypeSpec.Builder receiverBuilder, TypeElement interfaceElement) {
        MethodSpec mainConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.os", "Looper"), "looper")
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver")
                .addStatement("super(looper)")
                .addStatement("mReceiverWeakRef = new $T(receiver)", WeakReference.class)
                .build();

        MethodSpec constructor2 = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver")
                .addStatement("this(android.os.Looper.getMainLooper(), receiver)")
                .build();

        receiverBuilder.addMethod(mainConstructor);
        receiverBuilder.addMethod(constructor2);
    }
}
