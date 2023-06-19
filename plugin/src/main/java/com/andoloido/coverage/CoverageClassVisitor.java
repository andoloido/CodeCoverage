package com.andoloido.coverage;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CoverageClassVisitor extends ClassVisitor {
    ClassVisitor clv;
    MappingIdGen mappingIdGen;

    private String className;

    public CoverageClassVisitor(ClassVisitor clv, MappingIdGen mappingIdGen) {
        super(Opcodes.ASM7, clv);
        this.mappingIdGen = mappingIdGen;
        this.clv = clv;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    private int index = 0;

    private boolean hasClinit = false;
    private boolean clinitOnly = false;

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        boolean visit = false;
        // 灰度发现很多其他的anr会报到这个插件，因此减少插桩数量，只在静态初始化方法插桩，一个类只统计上报一次
        // only insert <clinit> to improve performance
        if (clinitOnly && name.equals("<clinit>")) {
            visit = true;
            hasClinit = true;
        } else if (!clinitOnly && (name.equals("<init>") || name.equals("<clinit>"))) {
            visit = true;
        }
        if (visit) {
            final int id = mappingIdGen.genMappingId(className, name, String.valueOf(index));
            index++;
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new CoverageMethodVisitor(access, name, descriptor, signature, exceptions, id, className, mv);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (!hasClinit) {
            final int id = mappingIdGen.genMappingId(className, "<clinit>", String.valueOf(index));
            index++;
            MethodVisitor methodVisitor = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            methodVisitor.visitLdcInsn(id);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "com/ss/android/ugc/bytex/coverage_lib/CoverageLogger", "Log", "(I)V", false);
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitEnd();
        }
        super.visitEnd();
    }
}
