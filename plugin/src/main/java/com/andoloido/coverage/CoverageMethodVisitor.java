package com.andoloido.coverage;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;


/**
 * MethodVisitor
 */
public class CoverageMethodVisitor extends MethodNode {

    private final int mapping;
    private String className;

    private MethodVisitor mv;

    /**
     * constructor
     */
    public CoverageMethodVisitor(int access, String name, String desc, String signature, String[] exceptions, int mapping, String className, MethodVisitor mv) {
        super(Opcodes.ASM7, access, name, desc, signature, exceptions);
        this.mapping = mapping;
        this.className = className;
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (this.mv == null) {
            return;
        }
        if (instructions != null) {
            boolean needInsert = true;
            // static块一定插桩
            if (!name.equals("<clinit>")){
                for (int i = 0; i < instructions.size(); i++) {
                    AbstractInsnNode ins = instructions.get(i);
                    if (Opcodes.INVOKESPECIAL == ins.getOpcode()) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) ins;
                        if (methodInsnNode.owner.equals(className) && methodInsnNode.name.equals("<init>")) {
                            // skip if contains this(...)
                            needInsert = false;
                            break;
                        }
                    }
                }
            }
            if (needInsert) {
                instructions.insertBefore(instructions.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, CoveragePlugin.REPORTER_CLASS, CoveragePlugin.REPORTER_METHOD, "(I)V", false));
                instructions.insertBefore(instructions.get(0), new LdcInsnNode(mapping));
            }
        }

        accept(this.mv);
    }

}
