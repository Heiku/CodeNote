package basic.asm;

/**
 * @Author: Heiku
 * @Date: 2019/12/2
 */
public class ASMCoreApiDemo {

    /**
     * field: name
     * field: age
     * method: <init>
     * method: getAge
     * method: setAge
     * method: main
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)  throws Exception {
        /*FileInputStream fis = new FileInputStream("target/classes/Basic/asm/ByteCodeDemo.class");
        ClassReader classReader = new ClassReader(fis);
        ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, cw){
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                System.out.println("field: " + name);
                return super.visitField(access, name, desc, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                System.out.println("method: " + name);
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };

        // ignore debug info
        classReader.accept(classVisitor, ClassReader.SKIP_DEBUG);*/

    }
}
