/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

/**
 * The simplest of class visitors, invokes the method visitor class for each
 * method found.
 */
public class ClassVisitor extends EmptyVisitor {
    public static String PREFIX = "C:";

    private JavaClass clazz;
    private ConstantPoolGen constants;
    private String classReferenceFormat;
    private final DynamicCallManager DCManager = new DynamicCallManager();

    private List<String> classInvokations = new ArrayList<>();
    private List<String> methodCalls = new ArrayList<>();
    private List<Pattern> includedIdentifiers;

    public ClassVisitor(JavaClass jc, List<Pattern> includedIdentifiers) {
        this.clazz = jc;
        this.constants = new ConstantPoolGen(clazz.getConstantPool());
        this.classReferenceFormat = PREFIX + clazz.getClassName() + " %s";
        this.includedIdentifiers = includedIdentifiers;
    }

    public void visitJavaClass(JavaClass jc) {
        jc.getConstantPool().accept(this);
        Method[] methods = jc.getMethods();
        for (Method method : methods) {
            DCManager.retrieveCalls(method, jc);
            DCManager.linkCalls(method);
            method.accept(this);
        }
    }

    public void visitConstantPool(ConstantPool constantPool) {
        for (int i = 0; i < constantPool.getLength(); i++) {
            Constant constant = constantPool.getConstant(i);
            if (constant == null) continue;
            if (constant.getTag() == 7) {
                String referencedClass = constantPool.constantToString(constant);
                if (!JCallGraph.identifierIsIncluded(referencedClass, includedIdentifiers)) continue;
                classInvokations.add(String.format(classReferenceFormat, referencedClass));
            }
        }
    }

    public void visitMethod(Method method) {
        MethodGen mg = new MethodGen(method, clazz.getClassName(), constants);
        MethodVisitor visitor = new MethodVisitor(mg, clazz, includedIdentifiers);
        methodCalls.addAll(visitor.start());
    }

    public ClassVisitor start() {
        visitJavaClass(clazz);
        return this;
    }

    public List<String> getClassInvokations() {
        return classInvokations;
    }

    public List<String> getMethodCalls() {
        return this.methodCalls;
    }
}
