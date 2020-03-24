package happy.handler.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import happy.handler.Handler;
import javafx.util.Pair;


@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({
        "happy.handler.Handler"
})
public class HandlerProcessor extends AbstractProcessor {
    private Filer mFiler;
    private Messager mMessager;
    private Elements mElements;
    private Types mTypes;

    private List<Pair<String, ExecutableElement>> mMethodPairs;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
        mElements = processingEnv.getElementUtils();
        mTypes = processingEnv.getTypeUtils();

        mMethodPairs = new ArrayList<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Handler.class);
        List<TypeElement> interfaces = getAnnotatedInterfaces(elements);

        for (TypeElement targetInterface : interfaces) {
            generateProxySourceFile(targetInterface);
        }

        return true;
    }

    private List<TypeElement> getAnnotatedInterfaces(Set<? extends Element> elements) {
        List<TypeElement> interfaces = new ArrayList<>();

        for (Element element : elements) {
            if (notInterface(element)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "require a interface.", element);
            }

            interfaces.add((TypeElement) element);
        }

        return interfaces;
    }

    private boolean notInterface(Element element) {
        return element.getKind() != ElementKind.INTERFACE;
    }

    private void generateProxySourceFile(TypeElement targetInterface) {
        List<ExecutableElement> allMethodElement = getAllMethod(targetInterface);
        checkMethodReturnType(allMethodElement);

        String packageName = mElements.getPackageOf(targetInterface).getQualifiedName().toString();
        String interfaceName = targetInterface.getSimpleName().toString();

        Handler handlerAnno = targetInterface.getAnnotation(Handler.class);
        String proxyClassName = interfaceName + "Handler";
        if (!handlerAnno.value().isEmpty()) {
            proxyClassName = handlerAnno.value();
        }

        // Proxy Class
        TypeSpec.Builder proxyClassBuilder = TypeSpec.classBuilder(proxyClassName)
                .superclass(ClassName.get("android.os", "Handler"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(targetInterface.asType());

        for (TypeParameterElement t : targetInterface.getTypeParameters()) {
            proxyClassBuilder.addTypeVariable(TypeVariableName.get(t));
        }

        // Method Id
        proxyClassBuilder.addFields(generateAllMethodId(allMethodElement));

        // Field: LAST_METHOD_ID
        proxyClassBuilder.addField(FieldSpec.builder(int.class, "LAST_METHOD_ID")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(String.valueOf(mMethodPairs.size()))
                .build());

        // Field: mReceiver
        proxyClassBuilder.addField(FieldSpec.builder(
                TypeName.get(targetInterface.asType()),
                "mReceiver",
                Modifier.PRIVATE
        ).build());

        // implement all method
        proxyClassBuilder.addMethods(implementAllMethod(mMethodPairs));

        // Constructors
        proxyClassBuilder.addMethods(generateConstructors(targetInterface));

        // Override handleMessage
        proxyClassBuilder.addMethod(overrideHandleMessage());

        JavaFile javaFile = JavaFile.builder(packageName, proxyClassBuilder.build())
                .build();

        try {
            javaFile.writeTo(mFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkMethodReturnType(List<ExecutableElement> elements) {
        for (ExecutableElement e : elements) {
            if (returnTypeNotVoid(e)) {
                mMessager.printMessage(Diagnostic.Kind.ERROR, "return type must be void.", e);
            }
        }
    }

    private List<ExecutableElement> getAllMethod(Element element) {
        List<? extends Element> elements = element.getEnclosedElements();

        List<ExecutableElement> result = new ArrayList<>(elements.size());

        for (Element e : elements) {
            if (e instanceof ExecutableElement) {
                result.add((ExecutableElement) e);
            }
        }

        return result;
    }

    private boolean returnTypeNotVoid(ExecutableElement element) {
        return element.getReturnType().getKind() != TypeKind.VOID;
    }

    private List<FieldSpec> generateAllMethodId(List<ExecutableElement> methodElements) {
        List<FieldSpec> allMethodId = new ArrayList<>(methodElements.size());

        for (int i = 1; i <= methodElements.size(); i++) {
            ExecutableElement method = methodElements.get(i - 1);
            FieldSpec methodIdSpec = generateMethodId(method, i);

            allMethodId.add(methodIdSpec);
            mMethodPairs.add(new Pair<>(methodIdSpec.name, method));
        }

        return allMethodId;
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

    private List<MethodSpec> implementAllMethod(List<Pair<String, ExecutableElement>> methodPairs) {
        List<MethodSpec> allMethod = new ArrayList<>(methodPairs.size());

        for (Pair<String, ExecutableElement> methodPair : methodPairs) {
            allMethod.add(implementMethod(methodPair));
        }

        return allMethod;
    }

    private MethodSpec implementMethod(Pair<String, ExecutableElement> methodPair) {
        ExecutableElement methodElement = methodPair.getValue();

        MethodSpec.Builder builder = MethodSpec.overriding(methodElement);

        List<? extends VariableElement> parameters = methodElement.getParameters();

        if (parameters.size() == 0) {
            return builder.addStatement("sendEmptyMessage($N)", methodPair.getKey())
                    .build();
        }

        builder.addStatement("$T args = new $T()", List.class, ArrayList.class);
        for (VariableElement param : parameters) {
            builder.addStatement("args.add($N)", param.getSimpleName().toString());
        }

        builder.addStatement("android.os.Message message = android.os.Message.obtain()");
        builder.addStatement("message.what = $N", methodPair.getKey());
        builder.addStatement("message.obj = args");
        builder.addStatement("sendMessage(message)");

        return builder.build();
    }

    private List<MethodSpec> generateConstructors(TypeElement targetInterface) {
        List<MethodSpec> constructors = new ArrayList<>();

        MethodSpec mainConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("android.os", "Looper"), "looper")
                .addParameter(TypeName.get(targetInterface.asType()), "receiver")
                .addStatement("super(looper)")
                .addStatement("mReceiver = receiver")
                .build();

        MethodSpec constructor2 = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(targetInterface.asType()), "receiver")
                .addStatement("this(android.os.Looper.getMainLooper(), receiver)")
                .build();

        constructors.add(mainConstructor);
        constructors.add(constructor2);

        return constructors;
    }

    private MethodSpec overrideHandleMessage() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("handleMessage")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(void.class)
                .addParameter(ClassName.get("android.os", "Message"), "msg");

        builder.addStatement("List args = (ArrayList)msg.obj")
                .beginControlFlow("switch (msg.what)");


        StringBuilder buff = new StringBuilder();
        for (Pair<String, ExecutableElement> pair : mMethodPairs) {
            buff.delete(0, buff.length());

            builder.addCode("case $N:\n", pair.getKey());

            List<? extends VariableElement> parameters = pair.getValue().getParameters();

            if (parameters.size() < 1) {
                builder.addStatement("mReceiver.$N()", pair.getValue().getSimpleName());
            } else {
                buff.append("mReceiver.")
                        .append(pair.getValue().getSimpleName())
                        .append("(");

                extractParamList(parameters, buff);

                buff.append(")");
                builder.addStatement(buff.toString());
            }

            builder.addStatement("break");
        }

        builder.endControlFlow();
        return builder.build();
    }

    private void extractParamList(List<? extends VariableElement> parameters, StringBuilder buff) {
        for (int i = 0; i < parameters.size(); i++) {
            buff.append("(");

            buff.append(parameters.get(i).asType().toString());

            buff.append(")args.get(")
                    .append(i)
                    .append(")");

            if (i < parameters.size() - 1) {
                buff.append(",");
            }
        }
    }
}
