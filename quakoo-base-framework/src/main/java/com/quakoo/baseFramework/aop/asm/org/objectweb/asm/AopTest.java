package com.quakoo.baseFramework.aop.asm.org.objectweb.asm;

public class AopTest {

    public static void teststatic() {

        System.out.println("teststatic");
    }


    public void testException() {
        int i = 0;
        String a = "b";
        System.out.println("test" + a + i);
        String m = null;
        m.split(",");
        System.out.println("hahah ");
    }

    public int testst12(AopTest test) {
        return 1;
    }

    private Object testst(AopTest test) {
        return test;
    }

    public static Object testStaticRetur0(AopTest test) {
        int i = 0;
        try {
            String a = "b";
            System.out.println("testRetur");
            return test.testst(test);
        } catch (Exception e) {
            System.out.println("1111");
        }
        return test;
    }

    public void test(int qa) {
        int i = 0;
        String a = "b";
        System.out.println("testqa" + a + i);
    }

    public static void main(String[] dg) {
        AopTest test = new AopTest();
        test.test(3);
        test.testException();
        testStaticRetur0(test);
        AopTest.teststatic();
    }
}
