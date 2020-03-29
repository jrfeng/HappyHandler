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

import com.squareup.javapoet.MethodSpec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import javafx.util.Pair;

/**
 * Generate Handler Source File
 */
public class HandlerGenerator extends AbstractGenerator {
    public HandlerGenerator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    @Override
    public void implement_sendMessageStatement(MethodSpec.Builder builder, String argName) {
        builder.addStatement("sendMessage($N)", argName);
    }

    @Override
    protected void implementMethodStatement(MethodSpec.Builder builder, Pair<String, ExecutableElement> methodPair) {
        ExecutableElement methodElement = methodPair.getValue();
        List<? extends VariableElement> parameters = methodElement.getParameters();

        builder.addStatement("android.os.Message message = android.os.Message.obtain()");
        builder.addStatement("message.what = $N", methodPair.getKey());

        if (parameters.size() == 0) {
            builder.addStatement("_sendMessage(message)");
            return;
        }

        builder.addStatement("$T args = new $T()", List.class, ArrayList.class);
        for (VariableElement param : parameters) {
            builder.addStatement("args.add($N)", param.getSimpleName().toString());
        }

        builder.addStatement("message.obj = args")
                .addStatement("_sendMessage(message)");
    }

    @Override
    protected void implement_handleMessageStatement(MethodSpec.Builder builder, TypeElement interfaceElement, String paramName) {
        builder.addStatement("$T receiver = ($T)mReceiverWeakRef.get()", interfaceElement, interfaceElement);

        builder.beginControlFlow("if (receiver == null)")
                .addStatement("return")
                .endControlFlow();

        builder.addStatement("List args = (ArrayList)$N.obj", paramName)
                .beginControlFlow("switch ($N.what)", paramName);

        StringBuilder buff = new StringBuilder();
        for (Pair<String, ExecutableElement> pair : getInterfaceMethodPairs()) {
            buff.delete(0, buff.length());

            builder.addCode("case $N:\n", pair.getKey());

            List<? extends VariableElement> parameters = pair.getValue().getParameters();

            if (parameters.size() < 1) {
                builder.addStatement("receiver.$N()", pair.getValue().getSimpleName())
                        .addStatement("$N.recycle()", paramName);
            } else {
                buff.append("receiver.")
                        .append(pair.getValue().getSimpleName())
                        .append("(");

                extractParamList(parameters, buff);

                buff.append(")");

                builder.addStatement(buff.toString())
                        .addStatement("$N.recycle()", paramName);
            }

            builder.addStatement("break");
        }

        builder.endControlFlow();
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
