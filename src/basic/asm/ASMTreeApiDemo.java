package basic.asm;

/**
 * @Author: Heiku
 * @Date: 2019/12/2
 */
public class ASMTreeApiDemo {

    /**
     * <init>
     * getAge
     * setAge
     * main
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        /*FileInputStream fis = new FileInputStream("target/classes/Basic/asm/ByteCodeDemo.class");

        ClassReader reader = new ClassReader(fis);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);

        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        // skip debug info
        reader.accept(classNode, ClassReader.SKIP_DEBUG);
        classNode.methods.forEach(methodNode -> System.out.println(methodNode.name));

        classNode.accept(writer);*/
    }
}
