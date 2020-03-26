package happy.handler.compiler.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class MessengerGenerator extends AbstractGenerator {
    private String mMessengerFieldName = "mMessenger";
    private TypeSpec.Builder mMessengerBuilder;
    private String mClassName;

    public MessengerGenerator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    @Override
    public void implementSendMessageStatement(MethodSpec.Builder builder, String argName) {
        builder.beginControlFlow("try")
                .addStatement("$N.send($N)", mMessengerFieldName, argName)
                .nextControlFlow("catch (android.os.RemoteException e)")
                .addStatement("e.printStackTrace()")
                .endControlFlow();
    }

    @Override
    public TypeSpec generate(String className, TypeElement interfaceElement) {
        mClassName = className;

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

        return mMessengerBuilder.build();
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
}
