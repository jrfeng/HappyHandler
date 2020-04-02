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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import javafx.util.Pair;

/**
 * Supported Type:
 * 1. byte, short, int, long, float, double, char, boolean
 * 2. String
 * 3. CharSequence
 * 4. IBinder (API level 18)
 * 5. Parcelable
 * 6. Serializable
 * <p>
 * Supported Array Type:
 * 1. byte[], short[], int[], long[], float[], double[], char[], boolean[]
 * 2. String[]
 * 3. CharSequence[]
 * 4. Parcelable[]
 * <p>
 * Supported List Type:
 * 1. List<Integer>
 * 2. List<String>
 * 3. List<CharSequence>
 * 4. List<? extends Parcelable>
 * <p>
 * Other Supported Type:
 * 1. SparseArray<? extends Parcelable>
 * 2. android.util.Size (API level 21)
 * 3. android.util.SizeF (API level 21)
 * <p>
 * Note: Unsupported Map.
 */
public class MessengerGenerator extends AbstractGenerator {
    private String mMessengerFieldName = "mMessenger";
    private TypeSpec.Builder mMessengerBuilder;

    private Elements mElements;
    private Types mTypes;
    private Messager mMessager;

    private ClassName mBundleType = ClassName.get("android.os", "Bundle");      // android.os.Bundle
    private ClassName mMessageType = ClassName.get("android.os", "Message");    // android.os.Message
    private ClassName mArrayListType = ClassName.get("java.util", "ArrayList"); // java.util.ArrayList

    public MessengerGenerator(ProcessingEnvironment processingEnv) {
        super(processingEnv);

        mElements = processingEnv.getElementUtils();
        mTypes = processingEnv.getTypeUtils();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public void implement_sendMessageStatement(MethodSpec.Builder builder, String argName) {
        builder.beginControlFlow("try")
                .addStatement("$N.send($N)", mMessengerFieldName, argName)
                .nextControlFlow("catch (android.os.RemoteException e)")
                .addStatement("e.printStackTrace()")
                .endControlFlow();
    }

    @Override
    public TypeSpec generate(String className, TypeElement interfaceElement) {
        initMessengerBuilder(className);

        // Inner class: Handler
        TypeSpec.Builder handlerBuilder = super.generate("Handler", interfaceElement).toBuilder();
        handlerBuilder.modifiers.remove(Modifier.PUBLIC);
        handlerBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        mMessengerBuilder.addType(handlerBuilder.build());

        // Constructor
        generateConstructor(interfaceElement);

        // Field: mMessenger
        generateMessengerField();

        // Method: getBinder():IBinder
        generate_getBinder();

        // Method: getMessenger():Messenger
        generate_getMessenger();

        return mMessengerBuilder.build();
    }

    @Override
    protected void implementMethodStatement(MethodSpec.Builder builder, Pair<String, ExecutableElement> methodPair) {
        ExecutableElement methodElement = methodPair.getValue();

        if (isParametersTypeIllegal(methodElement)) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "unsupported param type: " + methodElement.toString());
            return;
        }

        String varMessage = "message";

        builder.addStatement("$T $N = $T.obtain()", mMessageType, varMessage, mMessageType)
                .addStatement("$N.what = $N", varMessage, methodPair.getKey());

        String varData = "data";

        List<? extends VariableElement> parameters = methodElement.getParameters();
        if (parameters.size() > 0) {
            builder.addStatement("$T $N = new $T()", mBundleType, varData, mBundleType);
        }

        for (VariableElement param : parameters) {
            putDataIntoBundle(builder, param, varData);
        }

        if (parameters.size() > 0) {
            builder.addStatement("$N.setData($N)", varMessage, varData);
        }

        builder.addStatement("_sendMessage($N)", varMessage);
    }

    @Override
    protected void implement_handleMessageStatement(MethodSpec.Builder builder, TypeElement interfaceElement, String paramName) {
        String varReceiver = "receiver";
        builder.addStatement("$T $N = ($T)mReceiverWeakRef.get()", interfaceElement, varReceiver, interfaceElement);

        builder.beginControlFlow("if ($N == null)", varReceiver)
                .addStatement("return")
                .endControlFlow();

        String varData = "data";
        builder.addStatement("$T $N = $N.getData()", mBundleType, varData, paramName)
                .addStatement("$N.setClassLoader(Thread.currentThread().getContextClassLoader())", varData)
                .beginControlFlow("switch ($N.what)", paramName);

        for (Pair<String, ExecutableElement> methodPair : getInterfaceMethodPairs()) {
            builder.addCode("case $N:\n", methodPair.getKey());

            String paramPrefix = methodPair.getKey();
            ExecutableElement methodElement = methodPair.getValue();

            extractParams(builder, methodElement, varData, paramPrefix);

            builder.addStatement("$N.$N($N)", varReceiver, methodElement.getSimpleName(), getParamList(paramPrefix, methodElement))
                    .addStatement("break");
        }

        builder.endControlFlow();
    }

    private void extractParams(MethodSpec.Builder builder, ExecutableElement methodElement, String bundleName, String paramPrefix) {
        List<? extends VariableElement> params = methodElement.getParameters();

        for (VariableElement param : params) {
            extractParam(builder, param, bundleName, paramPrefix);
        }
    }

    private void extractParam(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        if (isPrimitive(param)) {
            extractPrimitive(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isArray(param)) {
            extractArray(builder, param, bundleName, paramPrefix);
            return;
        }

        Element paramType = mTypes.asElement(param.asType());

        if (isSparseArray(paramType)) {
            extractSparseParcelableArray(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isList(paramType)) {
            extractList(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isString(paramType)) {
            extractString(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isCharSequence(paramType)) {
            extractCharSequence(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isIBinder(paramType)) {
            extractIBinder(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isParcelable(paramType)) {
            extractParcelable(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isSerializable(paramType)) {
            extractSerializable(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isSize(paramType)) {
            extractSize(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isSizeF(paramType)) {
            extractSizeF(builder, param, bundleName, paramPrefix);
        }
    }

    private void extractArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        ArrayType arrayType = (ArrayType) param.asType();
        TypeMirror componentType = arrayType.getComponentType();

        if (componentType.getKind().isPrimitive()) {
            extractPrimitiveArray(builder, param, bundleName, paramPrefix);
            return;
        }

        Element componentTypeElement = mTypes.asElement(componentType);

        if (isString(componentTypeElement)) {
            extractStringArray(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isCharSequence(componentTypeElement)) {
            extractCharSequenceArray(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isParcelable(componentTypeElement)) {
            extractParcelableArray(builder, param, bundleName, paramPrefix);
        }
    }

    private void extractPrimitiveArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        ArrayType arrayType = (ArrayType) param.asType();
        TypeMirror componentType = arrayType.getComponentType();

        String paramName = param.getSimpleName().toString();
        String componentTypeName = componentType.toString().toLowerCase();

        componentTypeName = componentTypeName.substring(0, 1).toUpperCase() + componentTypeName.substring(1);

        builder.addStatement("$T $N_$N = $N.get$NArray($S)",
                arrayType,
                paramPrefix,
                paramName,
                bundleName,
                componentTypeName,
                paramName);
    }

    private void extractStringArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        ArrayType arrayType = (ArrayType) param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getStringArray($S)",
                arrayType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    private void extractCharSequenceArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        ArrayType arrayType = (ArrayType) param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getCharSequenceArray($S)",
                arrayType,
                paramPrefix,
                paramName,
                arrayType,
                bundleName,
                paramName);
    }

    private void extractParcelableArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        ArrayType arrayType = (ArrayType) param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getParcelableArray($S)",
                arrayType,
                paramPrefix,
                paramName,
                arrayType,
                bundleName,
                paramName);
    }

    private void extractSparseParcelableArray(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror paramType = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getSparseParcelableArray($S)",
                paramType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    // 1. List<Integer>
    // 2. List<String>
    // 3. List<CharSequence>
    // 4. List<? extends Parcelable>
    private void extractList(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        DeclaredType declaredType = (DeclaredType) param.asType();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() < 1) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "The type argument of List is unknown.");
        }

        Element element = mTypes.asElement(typeArguments.get(0));

        if (isInteger(element)) {
            extractIntegerList(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isString(element)) {
            extractStringList(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isCharSequence(element)) {
            extractCharSequenceList(builder, param, bundleName, paramPrefix);
            return;
        }

        if (isParcelable(element)) {
            extractParcelableList(builder, param, bundleName, paramPrefix);
        }
    }

    private void extractIntegerList(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror paramType = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getIntegerArrayList($S)",
                paramType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    private void extractStringList(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror paramType = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getStringArrayList($S)",
                paramType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    private void extractCharSequenceList(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror paramType = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getCharSequenceArrayList($S)",
                paramType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    private void extractParcelableList(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror paramType = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = $N.getParcelableArrayList($S)",
                paramType,
                paramPrefix,
                paramName,
                bundleName,
                paramName);
    }

    private void extractPrimitive(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();

        String paramTypeName = param.asType().toString().toLowerCase();
        paramTypeName = paramTypeName.substring(0, 1).toUpperCase() + paramTypeName.substring(1);

        builder.addStatement("$T $N_$N = $N.get$N($S)", typeMirror, paramPrefix, paramName, bundleName, paramTypeName, paramName);
    }

    private void extractString(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$T $N_$N = $N.getString($S)", typeMirror, paramPrefix, paramName, bundleName, paramName);
    }

    private void extractCharSequence(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getCharSequence($S)",
                typeMirror,
                paramPrefix,
                paramName,
                typeMirror,
                bundleName,
                paramName);
    }

    private void extractIBinder(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getBinder($S)",
                typeMirror,
                paramPrefix,
                paramName,
                typeMirror,
                bundleName,
                paramName);
    }

    private void extractParcelable(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getParcelable($S)",
                typeMirror,
                paramPrefix,
                paramName,
                typeMirror,
                bundleName,
                paramName);
    }

    private void extractSerializable(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();

        builder.addStatement("$T $N_$N = ($T)$N.getSerializable($S)",
                typeMirror,
                paramPrefix,
                paramName,
                typeMirror,
                bundleName,
                paramName);
    }

    // android.util.Size
    private void extractSize(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$T $N_$N = $N.getSize($S)", typeMirror, paramPrefix, paramName, bundleName, paramName);
    }

    // android.util.SizeF
    private void extractSizeF(MethodSpec.Builder builder, VariableElement param, String bundleName, String paramPrefix) {
        TypeMirror typeMirror = param.asType();
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$T $N_$N = $N.getSizeF($S)", typeMirror, paramPrefix, paramName, bundleName, paramName);
    }

    private String getParamList(String prefix, ExecutableElement method) {
        StringBuilder buf = new StringBuilder();

        List<? extends VariableElement> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            buf.append(prefix)
                    .append("_")
                    .append(params.get(i).getSimpleName());

            if (i < params.size() - 1) {
                buf.append(",");
            }
        }

        return buf.toString();
    }

    @Override
    protected TypeSpec.Builder getInterfaceBuilder() {
        return mMessengerBuilder;
    }

    private void initMessengerBuilder(String className) {
        mMessengerBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    private void generateMessengerField() {
        mMessengerBuilder.addField(FieldSpec.builder(
                ClassName.get("android.os", "Messenger"),
                mMessengerFieldName,
                Modifier.PRIVATE
        ).build());
    }

    private void generate_getBinder() {
        mMessengerBuilder.addMethod(MethodSpec.methodBuilder("getBinder")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("android.os", "IBinder"))
                .addStatement("return $N.getBinder()", mMessengerFieldName)
                .build());
    }

    private void generate_getMessenger() {
        mMessengerBuilder.addMethod(MethodSpec.methodBuilder("getMessenger")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("android.os", "Messenger"))
                .addStatement("return $N", mMessengerFieldName)
                .build());
    }

    // Constructor:
    // 1. (IBinder binder)
    // 2. (Looper looper, Receiver receiver)
    // 3. (Receiver receiver)
    private void generateConstructor(TypeElement interfaceElement) {
        // 1. (IBinder binder)
        MethodSpec factory1 = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.os", "IBinder"), "target")
                .addStatement("$N = new android.os.Messenger(target)", mMessengerFieldName)
                .build();

        // 2. (Looper looper, Receiver receiver)
        MethodSpec factory2 = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.os", "Looper"), "looper")
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver")
                .addStatement("$N = new android.os.Messenger(new Handler(looper, receiver))", mMessengerFieldName)
                .build();

        // 3. (Receiver receiver)
        MethodSpec factory3 = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(interfaceElement.asType()), "receiver")
                .addStatement("this(android.os.Looper.getMainLooper(), receiver)")
                .build();

        mMessengerBuilder.addMethod(factory1)
                .addMethod(factory2)
                .addMethod(factory3);
    }

    private boolean isParametersTypeIllegal(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();

        for (VariableElement param : parameters) {
            if (isParamTypeIllegal(param)) {
                return true;
            }
        }

        return false;
    }

    private boolean isParamTypeIllegal(VariableElement param) {
        if (isPrimitive(param)) {
            return false;
        }

        if (isArray(param) && isArrayTypeLegal(param)) {
            return false;
        }

        Element element = mTypes.asElement(param.asType());

        if (isSparseArray(element) && isSparseArrayTypeLegal(param)) {
            return false;
        }

        if (isList(element) && isListTypeLegal(param)) {
            return false;
        }

        return !(isString(element) ||
                isCharSequence(element) ||
                isIBinder(element) ||
                isParcelable(element) ||
                isSerializable(element) ||
                isSize(element) ||
                isSizeF(element)
        );
    }

    // 1. byte[], short[], int[], long[], float[], double[], char[], boolean[]
    // 2. String[]
    // 3. CharSequence[]
    // 4. Parcelable[]
    private boolean isArrayTypeLegal(VariableElement param) {
        ArrayType arrayType = (ArrayType) param.asType();
        TypeMirror componentType = arrayType.getComponentType();

        if (componentType.getKind().isPrimitive()) {
            return true;
        }

        Element element = mTypes.asElement(componentType);

        return isString(element) ||
                isCharSequence(element) ||
                isParcelable(element);
    }

    private boolean isSparseArrayTypeLegal(VariableElement param) {
        DeclaredType declaredType = (DeclaredType) param.asType();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() < 1) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "The type argument of SparseArray is unknown.");
        }

        return isParcelable(mTypes.asElement(typeArguments.get(0)));
    }

    // 1. List<Integer>
    // 2. List<String>
    // 3. List<CharSequence>
    // 4. List<? extends Parcelable>
    private boolean isListTypeLegal(VariableElement param) {
        DeclaredType declaredType = (DeclaredType) param.asType();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

        for (TypeMirror typeMirror : typeArguments) {
            Element element = mTypes.asElement(typeMirror);
            if (!(isInteger(element) || isString(element) || isCharSequence(element) || isParcelable(element))) {
                return false;
            }
        }

        return true;
    }

    private boolean isArray(Element element) {
        return element.asType().getKind() == TypeKind.ARRAY;
    }

    private boolean isSparseArray(Element element) {
        return isSubType(element, "android.util.SparseArray");
    }

    private boolean isList(Element element) {
        return isSubType(element, "java.util.List");
    }

    private boolean isPrimitive(Element element) {
        return element.asType().getKind().isPrimitive();
    }

    private boolean isString(Element element) {
        return isSubType(element, "java.lang.String");
    }

    private boolean isCharSequence(Element element) {
        return isSubType(element, "java.lang.CharSequence");
    }

    private boolean isIBinder(Element element) {
        return isSubType(element, "android.os.IBinder");
    }

    private boolean isParcelable(Element element) {
        return isSubType(element, "android.os.Parcelable");
    }

    private boolean isSerializable(Element element) {
        return isSubType(element, "java.io.Serializable");
    }

    private boolean isSize(Element element) {
        return isSubType(element, "android.util.Size");
    }

    private boolean isSizeF(Element element) {
        return isSubType(element, "android.util.SizeF");
    }

    private boolean isInteger(Element element) {
        return isSubType(element, "java.lang.Integer");
    }

    private boolean isSubType(Element element, String typeQualifiedName) {
        TypeElement typeElement = mElements.getTypeElement(typeQualifiedName);
        return mTypes.isSubtype(element.asType(), typeElement.asType());
    }

    private void putDataIntoBundle(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        if (isPrimitive(param)) {
            putPrimitive(builder, param, bundleName);
            return;
        }

        if (isArray(param)) {
            putArray(builder, param, bundleName);
            return;
        }

        Element paramType = mTypes.asElement(param.asType());

        if (isSparseArray(paramType)) {
            putSparseParcelableArray(builder, param, bundleName);
            return;
        }

        if (isList(paramType)) {
            putList(builder, param, bundleName);
            return;
        }

        if (isString(paramType)) {
            putString(builder, param, bundleName);
            return;
        }

        if (isCharSequence(paramType)) {
            putCharSequence(builder, param, bundleName);
            return;
        }

        if (isIBinder(paramType)) {
            putBinder(builder, param, bundleName);
            return;
        }

        if (isParcelable(paramType)) {
            putParcelable(builder, param, bundleName);
            return;
        }

        if (isSerializable(paramType)) {
            putSerializable(builder, param, bundleName);
            return;
        }

        if (isSize(paramType)) {
            putSize(builder, param, bundleName);
            return;
        }

        if (isSizeF(paramType)) {
            putSizeF(builder, param, bundleName);
        }
    }

    // byte, short, int, long, float, double, char, boolean
    private void putPrimitive(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        String paramTypeName = param.asType().getKind().toString().toLowerCase();

        builder.addStatement("$N.put$N($S, $N)",
                bundleName,
                paramTypeName.substring(0, 1).toUpperCase() + paramTypeName.substring(1),
                paramName,
                paramName);
    }

    // String
    private void putString(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putString($S, $N)", bundleName, paramName, paramName);
    }

    // CharSequence
    private void putCharSequence(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putCharSequence($S, $N)", bundleName, paramName, paramName);
    }

    // IBinder
    private void putBinder(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        builder.addAnnotation(AnnotationSpec.builder(ClassName.get("androidx.annotation", "RequiresApi"))
                .addMember("value", "android.os.Build.VERSION_CODES.JELLY_BEAN_MR2")
                .build());

        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putBinder($S, $N)", bundleName, paramName, paramName);
    }

    // Parcelable
    private void putParcelable(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putParcelable($S, $N)", bundleName, paramName, paramName);
    }

    // Serializable
    private void putSerializable(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putSerializable($S, $N)", bundleName, paramName, paramName);
    }

    // android.util.Size
    private void putSize(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        builder.addAnnotation(AnnotationSpec.builder(ClassName.get("androidx.annotation", "RequiresApi"))
                .addMember("value", "android.os.Build.VERSION_CODES.LOLLIPOP")
                .build());

        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putSize($S, $N)", bundleName, paramName, paramName);
    }

    // android.util.SizeF
    private void putSizeF(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        builder.addAnnotation(AnnotationSpec.builder(ClassName.get("androidx.annotation", "RequiresApi"))
                .addMember("value", "android.os.Build.VERSION_CODES.LOLLIPOP")
                .build());

        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putSizeF($S, $N)", bundleName, paramName, paramName);
    }

    // 1. byte[], short[], int[], long[], float[], double[], char[], boolean[]
    // 2. String[]
    // 3. CharSequence[]
    // 4. Parcelable[]
    private void putArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        ArrayType arrayType = (ArrayType) param.asType();
        TypeMirror componentType = arrayType.getComponentType();

        if (componentType.getKind().isPrimitive()) {
            putPrimitiveArray(builder, param, bundleName);
            return;
        }

        Element componentTypeElement = mTypes.asElement(componentType);

        if (isString(componentTypeElement)) {
            putStringArray(builder, param, bundleName);
            return;
        }

        if (isCharSequence(componentTypeElement)) {
            putCharSequenceArray(builder, param, bundleName);
            return;
        }

        if (isParcelable(componentTypeElement)) {
            putParcelableArray(builder, param, bundleName);
        }
    }

    // byte[], short[], int[], long[], float[], double[], char[], boolean[]
    private void putPrimitiveArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        ArrayType arrayType = (ArrayType) param.asType();
        String paramName = param.getSimpleName().toString();
        String paramTypeName = arrayType.getComponentType().getKind().toString().toLowerCase();

        builder.addStatement("$N.put$NArray($S, $N)",
                bundleName,
                paramTypeName.substring(0, 1).toUpperCase() + paramTypeName.substring(1),
                paramName,
                paramName);
    }

    // String[]
    private void putStringArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putStringArray($S, $N)", bundleName, paramName, paramName);
    }

    // CharSequence[]
    private void putCharSequenceArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putCharSequenceArray($S, $N)", bundleName, paramName, paramName);
    }

    // Parcelable[]
    private void putParcelableArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putParcelableArray($S, $N)", bundleName, paramName, paramName);
    }

    // SparseArray<? extends Parcelable>
    private void putSparseParcelableArray(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putSparseParcelableArray($S, $N)", bundleName, paramName, paramName);
    }

    // 1. List<Integer>
    // 2. List<String>
    // 3. List<CharSequence>
    // 4. List<? extends Parcelable>
    private void putList(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        DeclaredType declaredType = (DeclaredType) param.asType();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() < 1) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, "The type argument of List is unknown.");
        }

        Element element = mTypes.asElement(typeArguments.get(0));

        if (isInteger(element)) {
            putIntegerArrayList(builder, param, bundleName);
            return;
        }

        if (isString(element)) {
            putStringArrayList(builder, param, bundleName);
            return;
        }

        if (isCharSequence(element)) {
            putCharSequenceArrayList(builder, param, bundleName);
            return;
        }

        if (isParcelable(element)) {
            putParcelableArrayList(builder, param, bundleName);
        }
    }

    private void putIntegerArrayList(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putIntegerArrayList($S, new $T<>($N))", bundleName, paramName, mArrayListType, paramName);
    }

    private void putStringArrayList(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putStringArrayList($S, new $T<>($N))", bundleName, paramName, mArrayListType, paramName);
    }

    private void putCharSequenceArrayList(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putCharSequenceArrayList($S, new $T<>($N))", bundleName, paramName, mArrayListType, paramName);
    }

    private void putParcelableArrayList(MethodSpec.Builder builder, VariableElement param, String bundleName) {
        String paramName = param.getSimpleName().toString();
        builder.addStatement("$N.putParcelableArrayList($S, new $T<>($N))", bundleName, paramName, mArrayListType, paramName);
    }
}
